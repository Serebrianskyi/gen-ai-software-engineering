import agents.*
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.time.Instant
import kotlin.io.path.*

/**
 * Full REST pipeline integration test.
 *
 * Starts all four agent HTTP servers on test ports, dispatches the 8 sample
 * transactions, waits for all results, then asserts the expected outcomes.
 */
class IntegrationTest {

    private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var servers: List<HttpServer> = emptyList()
    private lateinit var resultsDir: Path

    private val testConfig = PipelineConfig(
        agents = mapOf(
            "andromeda" to AgentEndpoint("localhost", 19081),
            "sirius"    to AgentEndpoint("localhost", 19082),
            "vega"      to AgentEndpoint("localhost", 19083),
            "polaris"   to AgentEndpoint("localhost", 19084),
        ),
        pipeline = listOf("andromeda", "sirius", "vega", "polaris"),
        prerequisites = mapOf(
            "andromeda" to emptyList(),
            "sirius"    to listOf("andromeda"),
            "vega"      to listOf("andromeda", "sirius"),
            "polaris"   to listOf("andromeda", "sirius"),
        ),
        maxHops = 20,
    )

    @BeforeEach
    fun setup() {
        val shared = Files.createTempDirectory("hw6-integration-rest")
        resultsDir = (shared / "results").also { it.createDirectories() }
        RESULTS_DIR    = resultsDir
        SAMPLE_FILE    = Path.of("sample-transactions.json")
        PIPELINE_CONFIG = testConfig

        servers = testConfig.agents.keys.map { name ->
            val port = testConfig.endpointFor(name).port
            startAgentServer(port, name) { incoming ->
                when (name) {
                    "andromeda" -> handle(name, incoming, testConfig, resultsDir) { msg ->
                        val result   = processTransaction(msg.data)
                        val terminal = result.data["status"]?.jsonPrimitive?.content == "rejected"
                        result.data to terminal
                    }
                    "sirius" -> handle(name, incoming, testConfig, resultsDir) { msg ->
                        val result = processFraudTransaction(msg.data)
                        result.data to false
                    }
                    "vega" -> handle(name, incoming, testConfig, resultsDir) { msg ->
                        processVega(msg.data)
                    }
                    "polaris" -> handle(name, incoming, testConfig, resultsDir) { msg ->
                        val status = msg.data["status"]?.jsonPrimitive?.content
                        if (status == "fraud_cleared") {
                            val result = settleTransaction(msg.data)
                            result.data to true
                        } else {
                            msg.data to true
                        }
                    }
                    else -> error("Unknown agent: $name")
                }
            }
        }
        Thread.sleep(200)
    }

    @AfterEach
    fun teardown() {
        servers.forEach { it.stop(0) }
    }

    private fun dispatch(transactions: List<JsonObject>) {
        val pipeline   = testConfig.pipeline
        val firstAgent = pipeline.first()
        val firstUrl   = testConfig.endpointFor(firstAgent).url
        transactions.forEach { txn ->
            postToAgent(firstUrl, AgentMessage(
                messageId     = UUID.randomUUID().toString(),
                timestamp     = Instant.now().toString(),
                sourceAgent   = "test-orchestrator",
                targetAgent   = firstAgent,
                data          = txn,
                pipelineQueue = pipeline.drop(1),
            ))
        }
    }

    private fun waitForResults(expected: Int, timeoutMs: Long = 90_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (resultsDir.listDirectoryEntries("TXN*.json").size >= expected) return
            Thread.sleep(300)
        }
        val found = resultsDir.listDirectoryEntries("TXN*.json").size
        error("Timed out waiting for results: expected $expected, found $found")
    }

    private fun resultData(txnId: String): JsonObject =
        JSON.parseToJsonElement(resultsDir.resolve("$txnId.json").readText())
            .jsonObject["data"]!!.jsonObject

    private fun runFullPipeline(): List<JsonObject> {
        val transactions = JSON.decodeFromString<List<JsonObject>>(SAMPLE_FILE.readText())
        dispatch(transactions)
        waitForResults(transactions.size)
        return transactions
    }

    @Test fun `all 8 transactions reach results directory`() {
        runFullPipeline()
        assertEquals(8, resultsDir.listDirectoryEntries("TXN*.json").size)
    }

    @Test fun `specific transaction statuses match expected outcomes`() {
        runFullPipeline()
        assertEquals("settled", resultData("TXN001")["status"]?.jsonPrimitive?.content)
        assertEquals("flagged", resultData("TXN002")["status"]?.jsonPrimitive?.content)
        assertEquals("settled", resultData("TXN003")["status"]?.jsonPrimitive?.content)
        assertEquals("flagged", resultData("TXN004")["status"]?.jsonPrimitive?.content)
        assertEquals("flagged", resultData("TXN005")["status"]?.jsonPrimitive?.content)
        assertEquals("rejected", resultData("TXN006")["status"]?.jsonPrimitive?.content)
        assertEquals("rejected", resultData("TXN007")["status"]?.jsonPrimitive?.content)
        assertEquals("settled", resultData("TXN008")["status"]?.jsonPrimitive?.content)
    }

    @Test fun `settled transactions have settled_at field`() {
        runFullPipeline()
        resultsDir.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "settled")
                assertNotNull(data["settled_at"], "settled_at missing in ${f.name}")
        }
    }

    @Test fun `rejected transactions have non-empty rejection_reason`() {
        runFullPipeline()
        resultsDir.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "rejected") {
                val reason = data["rejection_reason"]?.jsonPrimitive?.content
                assertNotNull(reason, "rejection_reason missing in ${f.name}")
                assertTrue(reason!!.isNotEmpty())
            }
        }
    }

    @Test fun `flagged transactions have risk_score at least 0_4`() {
        runFullPipeline()
        resultsDir.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "flagged") {
                val score = data["risk_score"]?.jsonPrimitive?.double ?: -1.0
                assertTrue(score >= 0.4, "risk_score $score < 0.4 in ${f.name}")
            }
        }
    }

    @Test fun `flagged transactions have compliance_report from VEGA`() {
        runFullPipeline()
        resultsDir.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "flagged") {
                assertNotNull(data["compliance_report"], "compliance_report missing in ${f.name}")
                assertNotNull(data["recommended_action"], "recommended_action missing in ${f.name}")
            }
        }
    }

    @Test fun `accounts are masked in all result files`() {
        runFullPipeline()
        resultsDir.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            assertEquals("ACC-****", data["source_account"]?.jsonPrimitive?.content, "unmasked source in ${f.name}")
            assertEquals("ACC-****", data["destination_account"]?.jsonPrimitive?.content, "unmasked destination in ${f.name}")
        }
    }

    @Test fun `pipeline_queue is configurable — scrambled order self-heals`() {
        // Send one transaction directly to polaris first (wrong order)
        val txn = JSON.decodeFromString<List<JsonObject>>(SAMPLE_FILE.readText()).first()
        // Manually route: start at polaris — it will detect missing prereqs and reorder
        postToAgent(testConfig.endpointFor("polaris").url, AgentMessage(
            messageId     = UUID.randomUUID().toString(),
            timestamp     = Instant.now().toString(),
            sourceAgent   = "test-scrambled",
            targetAgent   = "polaris",
            data          = txn,
            pipelineQueue = emptyList(),
        ))
        waitForResults(1)
        val status = resultsDir.listDirectoryEntries("TXN*.json")
            .firstOrNull()?.let {
                JSON.parseToJsonElement(it.readText()).jsonObject["data"]?.jsonObject
                    ?.get("status")?.jsonPrimitive?.content
            }
        // TXN001 is a clean low-value domestic txn — should still settle
        assertEquals("settled", status)
    }
}
