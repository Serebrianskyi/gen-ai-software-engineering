import agents.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.*

private val BASE_DIR: Path = Path.of(object {}.javaClass.protectionDomain.codeSource.location.toURI())
    .parent.parent.parent.parent  // build/classes/kotlin/main → project root
private val SHARED: Path = BASE_DIR.resolve("shared")

var INPUT_DIR: Path = SHARED.resolve("input")
var PROCESSING_DIR: Path = SHARED.resolve("processing")
var OUTPUT_DIR: Path = SHARED.resolve("output")
var RESULTS_DIR: Path = SHARED.resolve("results")
var SAMPLE_FILE: Path = BASE_DIR.resolve("sample-transactions.json")

private val log = Logger.getLogger("integrator")
private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }

fun audit(event: String, extra: Map<String, Any> = emptyMap()) {
    val parts = buildString {
        append("""{"timestamp":"${Instant.now()}","agent":"integrator","event":"$event"""")
        extra.forEach { (k, v) -> append(""","$k":"$v"""") }
        append("}")
    }
    log.info(parts)
}

fun setupDirectories() {
    listOf(INPUT_DIR, PROCESSING_DIR, OUTPUT_DIR, RESULTS_DIR).forEach { it.createDirectories() }
    listOf(INPUT_DIR, PROCESSING_DIR, OUTPUT_DIR).forEach { dir ->
        dir.listDirectoryEntries("*.json").forEach { it.deleteIfExists() }
    }
    RESULTS_DIR.listDirectoryEntries("*.json").forEach { f ->
        if (f.name != "pipeline_summary.json") f.deleteIfExists()
    }
}

fun loadTransactions(): List<JsonObject> =
    JSON.decodeFromString<List<JsonObject>>(SAMPLE_FILE.readText())

fun writeInputMessages(transactions: List<JsonObject>) {
    transactions.forEach { txn ->
        val txnId = txn["transaction_id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
        val msg = AgentMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = Instant.now().toString(),
            sourceAgent = "integrator",
            targetAgent = "transaction_validator",
            data = txn,
        )
        INPUT_DIR.resolve("$txnId.json").writeText(JSON.encodeToString(msg))
    }
}

fun runValidator(): Map<String, List<Map<String, String>>> {
    val stage = mutableMapOf("validated" to mutableListOf<Map<String, String>>(), "rejected" to mutableListOf())
    INPUT_DIR.listDirectoryEntries("*.json").sortedBy { it.name }.forEach { msgFile ->
        val message = JSON.parseToJsonElement(msgFile.readText()).jsonObject
        val procPath = PROCESSING_DIR.resolve(msgFile.name)
        msgFile.moveTo(procPath, overwrite = true)
        val result = processTransaction(message)
        val data = result.data
        val status = data["status"]?.jsonPrimitive?.content ?: "rejected"
        val outPath = if (status == "rejected") RESULTS_DIR.resolve(msgFile.name) else OUTPUT_DIR.resolve(msgFile.name)
        outPath.writeText(JSON.encodeToString(result))
        procPath.deleteIfExists()
        if (status == "rejected") {
            stage["rejected"]!! += mapOf("id" to (data["transaction_id"]?.jsonPrimitive?.content ?: msgFile.nameWithoutExtension), "reason" to (data["rejection_reason"]?.jsonPrimitive?.content ?: ""))
        } else {
            stage["validated"]!! += mapOf("id" to (data["transaction_id"]?.jsonPrimitive?.content ?: msgFile.nameWithoutExtension))
        }
    }
    return stage
}

fun runFraudDetector(): Map<String, List<Map<String, String>>> {
    val stage = mutableMapOf("flagged" to mutableListOf<Map<String, String>>(), "cleared" to mutableListOf())
    OUTPUT_DIR.listDirectoryEntries("*.json").sortedBy { it.name }.forEach { msgFile ->
        val message = JSON.parseToJsonElement(msgFile.readText()).jsonObject
        val procPath = PROCESSING_DIR.resolve(msgFile.name)
        msgFile.moveTo(procPath, overwrite = true)
        val result = processFraudTransaction(message)
        val data = result.data
        val status = data["status"]?.jsonPrimitive?.content ?: "fraud_cleared"
        val outPath = if (status == "flagged") RESULTS_DIR.resolve(msgFile.name) else OUTPUT_DIR.resolve(msgFile.name)
        outPath.writeText(JSON.encodeToString(result))
        procPath.deleteIfExists()
        if (status == "flagged") {
            stage["flagged"]!! += mapOf("id" to (data["transaction_id"]?.jsonPrimitive?.content ?: msgFile.nameWithoutExtension))
        } else {
            stage["cleared"]!! += mapOf("id" to (data["transaction_id"]?.jsonPrimitive?.content ?: msgFile.nameWithoutExtension))
        }
    }
    return stage
}

fun runSettlement(): PipelineSummary {
    val stage = mutableListOf<String>()
    OUTPUT_DIR.listDirectoryEntries("*.json").sortedBy { it.name }.forEach { msgFile ->
        val message = JSON.parseToJsonElement(msgFile.readText()).jsonObject
        if (message["data"]?.jsonObject?.get("status")?.jsonPrimitive?.content != "fraud_cleared") return@forEach
        val procPath = PROCESSING_DIR.resolve(msgFile.name)
        msgFile.moveTo(procPath, overwrite = true)
        val result = settleTransaction(message)
        RESULTS_DIR.resolve(msgFile.name).writeText(JSON.encodeToString(result))
        procPath.deleteIfExists()
        stage += result.data["transaction_id"]?.jsonPrimitive?.content ?: msgFile.nameWithoutExtension
    }
    val summary = generateSummary(RESULTS_DIR)
    RESULTS_DIR.resolve("pipeline_summary.json").writeText(JSON.encodeToString(summary))
    return summary
}

fun main(args: Array<String>) {
    if ("--dry-run" in args) {
        dryRun(SAMPLE_FILE)
        return
    }

    audit("pipeline_start")
    setupDirectories()

    val transactions = loadTransactions()
    audit("transactions_loaded", mapOf("count" to transactions.size))
    writeInputMessages(transactions)

    println("\n════════════════════════════════════════")
    println("  STAGE 1 — ANDROMEDA  [Transaction Validator]")
    println("  \"Every field. Every rule. No exceptions.\"")
    println("════════════════════════════════════════")
    runValidator()

    println("\n════════════════════════════════════════")
    println("  STAGE 2 — SIRIUS  [Fraud Detector]")
    println("  \"The brightest eye in the sky sees everything.\"")
    println("════════════════════════════════════════")
    runFraudDetector()

    println("\n════════════════════════════════════════")
    println("  STAGE 3 — POLARIS  [Settlement Processor]")
    println("  \"Cleared transactions always find their way here.\"")
    println("════════════════════════════════════════")
    val summary = runSettlement()

    println("\n════════════════════════════════════════")
    println("  PIPELINE SUMMARY")
    println("════════════════════════════════════════")
    println("  Total:    ${summary.total}")
    println("  Settled:  ${summary.settled}")
    println("  Flagged:  ${summary.flagged}")
    println("  Rejected: ${summary.rejected}")
    println("  Completed: ${summary.processedAt}")

    val rejected = RESULTS_DIR.listDirectoryEntries("*.json")
        .filter { it.name != "pipeline_summary.json" }
        .mapNotNull { f ->
            val data = JSON.parseToJsonElement(f.readText()).jsonObject["data"]?.jsonObject ?: return@mapNotNull null
            if (data["status"]?.jsonPrimitive?.content == "rejected")
                (data["transaction_id"]?.jsonPrimitive?.content ?: "?") to (data["rejection_reason"]?.jsonPrimitive?.content ?: "")
            else null
        }
    if (rejected.isNotEmpty()) {
        println("\n  Rejected transactions:")
        rejected.forEach { (id, reason) -> println("    $id: $reason") }
    }

    audit("pipeline_complete", mapOf("total" to summary.total, "settled" to summary.settled, "flagged" to summary.flagged, "rejected" to summary.rejected))
    println()
}