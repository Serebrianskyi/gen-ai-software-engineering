package agents

import AgentMessage
import PipelineSummary
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.readText

private val log = Logger.getLogger("settlement_processor")

// POLARIS — the fixed point: cleared transactions always find their way here
private const val E = ""
private const val POLARIS = "${E}[1;32m[POLARIS]${E}[0m"
private const val GREEN   = "${E}[32m"
private const val RESET   = "${E}[0m"

private val JSON = Json { ignoreUnknownKeys = true }

private fun audit(txnId: String, outcome: String) {
    log.info("""{"timestamp":"${Instant.now()}","agent":"settlement_processor","transaction_id":"$txnId","outcome":"$outcome"}""")
}

private fun makeMessage(targetAgent: String, data: JsonObject): AgentMessage = AgentMessage(
    messageId = UUID.randomUUID().toString(),
    timestamp = Instant.now().toString(),
    sourceAgent = "settlement_processor",
    targetAgent = targetAgent,
    data = data,
)

fun settleTransaction(message: JsonObject): AgentMessage {
    val data = message["data"]?.jsonObject ?: message
    val txnId = data["transaction_id"]?.jsonPrimitive?.content ?: "UNKNOWN"
    val amount = data["amount"]?.jsonPrimitive?.content ?: "?"
    val currency = data["currency"]?.jsonPrimitive?.content ?: "?"

    println("  $POLARIS $txnId reached me — cleared by both ANDROMEDA and SIRIUS.")
    println("  $POLARIS \"Both validators signed off on this one. I'm booking $amount $currency now. The ledger doesn't lie.\"")

    val out = buildJsonObject {
        data.forEach { (k, v) -> put(k, v) }
        put("status", "settled")
        put("settled_at", Instant.now().toString())
    }

    println("  $POLARIS ${GREEN}✓ $txnId SETTLED$RESET — done.")
    audit(txnId, "settled")
    return makeMessage("results", out)
}

fun generateSummary(resultsDir: Path): PipelineSummary {
    var settled = 0; var flagged = 0; var rejected = 0
    resultsDir.toFile().listFiles { f -> f.name.endsWith(".json") && f.name != "pipeline_summary.json" }
        ?.forEach { file ->
            try {
                val msg = JSON.parseToJsonElement(file.readText()).jsonObject
                when (msg["data"]?.jsonObject?.get("status")?.jsonPrimitive?.content) {
                    "settled" -> settled++
                    "flagged" -> flagged++
                    "rejected" -> rejected++
                }
            } catch (_: Exception) {}
        }
    return PipelineSummary(
        total = settled + flagged + rejected,
        settled = settled,
        flagged = flagged,
        rejected = rejected,
        processedAt = Instant.now().toString(),
    )
}