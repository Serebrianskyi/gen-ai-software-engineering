import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Full pipeline integration test using temp directories, mirroring test_integration.py.
 * Each test injects a fresh temp shared/ tree into the global path vars in Main.kt.
 */
class IntegrationTest {

    private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        val shared = Files.createTempDirectory("hw6-integration")
        listOf("input", "processing", "output", "results").forEach { (shared / it).createDirectories() }
        INPUT_DIR = shared / "input"
        PROCESSING_DIR = shared / "processing"
        OUTPUT_DIR = shared / "output"
        RESULTS_DIR = shared / "results"
        SAMPLE_FILE = Path.of("sample-transactions.json")
    }

    private fun runPipeline() {
        setupDirectories()
        writeInputMessages(loadTransactions())
        runValidator()
        runFraudDetector()
        runSettlement()
    }

    private fun resultStatus(txnId: String): String {
        val data = JSON.parseToJsonElement(RESULTS_DIR.resolve("$txnId.json").readText())
            .jsonObject["data"]?.jsonObject ?: return "?"
        return data["status"]?.jsonPrimitive?.content ?: "?"
    }

    @Test fun `all 8 transactions reach results directory`() {
        runPipeline()
        assertEquals(8, RESULTS_DIR.listDirectoryEntries("TXN*.json").size)
    }

    @Test fun `pipeline summary counts are 3 settled 3 flagged 2 rejected`() {
        runPipeline()
        val summary = JSON.decodeFromString<PipelineSummary>(
            RESULTS_DIR.resolve("pipeline_summary.json").readText()
        )
        assertEquals(8, summary.total)
        assertEquals(3, summary.settled)
        assertEquals(3, summary.flagged)
        assertEquals(2, summary.rejected)
    }

    @Test fun `specific transaction statuses match expected outcomes`() {
        runPipeline()
        assertEquals("settled", resultStatus("TXN001"))   // clean, low-value domestic
        assertEquals("flagged", resultStatus("TXN002"))   // high-value $25k
        assertEquals("settled", resultStatus("TXN003"))   // structuring score 0.3 < 0.4 threshold
        assertEquals("flagged", resultStatus("TXN004"))   // off-hours + cross-border DE
        assertEquals("flagged", resultStatus("TXN005"))   // high-value $75k
        assertEquals("rejected", resultStatus("TXN006"))  // invalid currency XYZ
        assertEquals("rejected", resultStatus("TXN007"))  // negative amount
        assertEquals("settled", resultStatus("TXN008"))   // clean domestic
    }

    @Test fun `no files remain in processing after pipeline`() {
        runPipeline()
        assertEquals(emptyList<Path>(), PROCESSING_DIR.listDirectoryEntries("*.json"))
    }

    @Test fun `settled transactions have settled_at field`() {
        runPipeline()
        RESULTS_DIR.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "settled")
                assertNotNull(data["settled_at"], "settled_at missing in ${f.name}")
        }
    }

    @Test fun `rejected transactions have non-empty rejection_reason`() {
        runPipeline()
        RESULTS_DIR.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "rejected") {
                val reason = data["rejection_reason"]?.jsonPrimitive?.content
                assertNotNull(reason, "rejection_reason missing in ${f.name}")
                assertTrue(reason!!.isNotEmpty())
            }
        }
    }

    @Test fun `flagged transactions have risk_score at least 0_4`() {
        runPipeline()
        RESULTS_DIR.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            if (data["status"]?.jsonPrimitive?.content == "flagged") {
                val score = data["risk_score"]?.jsonPrimitive?.double ?: -1.0
                assertTrue(score >= 0.4, "risk_score $score < 0.4 in ${f.name}")
            }
        }
    }

    @Test fun `accounts are masked in all result files`() {
        runPipeline()
        RESULTS_DIR.listDirectoryEntries("TXN*.json").forEach { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@forEach
            assertEquals("ACC-****", data["source_account"]?.jsonPrimitive?.content, "unmasked source in ${f.name}")
            assertEquals("ACC-****", data["destination_account"]?.jsonPrimitive?.content, "unmasked destination in ${f.name}")
        }
    }
}