import agents.*
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.*

private val BASE_DIR: Path = Path.of(object {}.javaClass.protectionDomain.codeSource.location.toURI())
    .parent.parent.parent.parent  // build/classes/kotlin/main → project root

var RESULTS_DIR: Path = BASE_DIR.resolve("shared/results")
var SAMPLE_FILE: Path = BASE_DIR.resolve("sample-transactions.json")
lateinit var PIPELINE_CONFIG: PipelineConfig

private val log = Logger.getLogger("orchestrator")
private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }

fun audit(event: String, extra: Map<String, Any> = emptyMap()) {
    val parts = buildString {
        append("""{"timestamp":"${Instant.now()}","agent":"orchestrator","event":"$event"""")
        extra.forEach { (k, v) -> append(""","$k":"$v"""") }
        append("}")
    }
    log.info(parts)
}

fun setupDirectories() {
    RESULTS_DIR.createDirectories()
    RESULTS_DIR.listDirectoryEntries("*.json").forEach { f ->
        if (f.name != "pipeline_summary.json") f.deleteIfExists()
    }
}

fun loadTransactions(): List<JsonObject> =
    JSON.decodeFromString<List<JsonObject>>(SAMPLE_FILE.readText())

// ── Agent HTTP handlers ───────────────────────────────────────────────────────

fun startAllServers(): List<HttpServer> {
    val cfg = PIPELINE_CONFIG
    return cfg.agents.keys.map { name ->
        val port = cfg.endpointFor(name).port
        startAgentServer(port, name) { incoming ->
            when (name) {
                "andromeda" -> handle(name, incoming, cfg, RESULTS_DIR) { msg ->
                    val result   = processTransaction(msg.data)
                    val terminal = result.data["status"]?.jsonPrimitive?.content == "rejected"
                    result.data to terminal
                }
                "sirius" -> handle(name, incoming, cfg, RESULTS_DIR) { msg ->
                    val result = processFraudTransaction(msg.data)
                    result.data to false   // never terminal — VEGA or POLARIS handles the endpoint
                }
                "vega" -> handle(name, incoming, cfg, RESULTS_DIR) { msg ->
                    processVega(msg.data)
                }
                "polaris" -> handle(name, incoming, cfg, RESULTS_DIR) { msg ->
                    val status = msg.data["status"]?.jsonPrimitive?.content
                    if (status == "fraud_cleared") {
                        val result = settleTransaction(msg.data)
                        result.data to true
                    } else {
                        msg.data to true   // pass through any non-clearable transaction as-is
                    }
                }
                else -> error("No handler registered for agent '$name'")
            }
        }
    }
}

// ── Main ─────────────────────────────────────────────────────────────────────

fun main(args: Array<String>) {
    if ("--dry-run" in args) {
        dryRun(SAMPLE_FILE)
        return
    }

    val configPath = args.find { it.startsWith("--config=") }
        ?.removePrefix("--config=")
        ?.let { BASE_DIR.resolve(it) }
        ?: BASE_DIR.resolve("pipeline-config.json")

    val rulesPath = args.find { it.startsWith("--rules=") }
        ?.removePrefix("--rules=")
        ?.let { BASE_DIR.resolve(it) }
        ?: BASE_DIR.resolve("rules.json")

    PIPELINE_CONFIG = PipelineConfig.load(configPath)
    val ruleEngine  = RuleEngine.load(rulesPath)

    configureAndromeda(ruleEngine)
    configureSirius(ruleEngine)

    setupDirectories()
    audit("pipeline_start")

    val servers = startAllServers()
    Thread.sleep(300)  // brief startup grace for HttpServer threads

    val transactions = loadTransactions()
    audit("transactions_loaded", mapOf("count" to transactions.size))

    val pipeline   = PIPELINE_CONFIG.pipeline
    val firstAgent = pipeline.first()
    val firstUrl   = PIPELINE_CONFIG.endpointFor(firstAgent).url

    println("\n════════════════════════════════════════")
    println("  Pipeline: ${pipeline.joinToString(" → ") { it.uppercase() }}")
    println("════════════════════════════════════════")

    transactions.forEach { txn ->
        val msg = AgentMessage(
            messageId     = UUID.randomUUID().toString(),
            timestamp     = Instant.now().toString(),
            sourceAgent   = "orchestrator",
            targetAgent   = firstAgent,
            data          = txn,
            pipelineQueue = pipeline.drop(1),
        )
        postToAgent(firstUrl, msg)
    }

    // Poll for completion — all TXN result files written
    val expected = transactions.size
    val deadline = System.currentTimeMillis() + 120_000
    while (System.currentTimeMillis() < deadline) {
        val count = RESULTS_DIR.listDirectoryEntries("TXN*.json").size
        if (count >= expected) break
        Thread.sleep(300)
    }

    val summary = generateSummary(RESULTS_DIR)
    RESULTS_DIR.resolve("pipeline_summary.json").writeText(JSON.encodeToString(summary))

    println("\n════════════════════════════════════════")
    println("  PIPELINE SUMMARY")
    println("════════════════════════════════════════")
    println("  Total:    ${summary.total}")
    println("  Settled:  ${summary.settled}")
    println("  Flagged:  ${summary.flagged}")
    println("  Rejected: ${summary.rejected}")
    println("  Completed: ${summary.processedAt}")

    val rejected = RESULTS_DIR.listDirectoryEntries("TXN*.json").mapNotNull { f ->
        val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@mapNotNull null
        if (data["status"]?.jsonPrimitive?.content == "rejected")
            (data["transaction_id"]?.jsonPrimitive?.content ?: "?") to
                (data["rejection_reason"]?.jsonPrimitive?.content ?: "")
        else null
    }
    if (rejected.isNotEmpty()) {
        println("\n  Rejected transactions:")
        rejected.forEach { (id, reason) -> println("    $id: $reason") }
    }

    audit("pipeline_complete", mapOf("total" to summary.total, "settled" to summary.settled,
        "flagged" to summary.flagged, "rejected" to summary.rejected))

    servers.forEach { it.stop(0) }
    println()
}
