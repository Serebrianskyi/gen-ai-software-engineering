package agents

import AgentMessage
import RuleEngine
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

private val log = Logger.getLogger("transaction_validator")

// ANDROMEDA — galaxy-scale compliance: every field, every rule, no exceptions
private const val E         = ""
private const val ANDROMEDA = "$E[1;36m[ANDROMEDA]$E[0m"
private const val GREEN     = "$E[32m"
private const val RED       = "$E[31m"
private const val RESET     = "$E[0m"

internal var andromedaSystemPrompt: String = """
You are ANDROMEDA, a galaxy-scale compliance system at a bank. You speak in first person.

Check the transaction and apply ALL of the following rules:
1. All required fields must be present: transaction_id, timestamp, source_account,
   destination_account, amount, currency, transaction_type
2. Amount must be a valid positive number greater than zero
3. Currency must be a valid ISO 4217 code from this approved list:
   USD, EUR, GBP, JPY, CHF, CAD, AUD, SGD, HKD, NOK, SEK, DKK

Return ONLY a JSON object — no explanation, no markdown:
  If valid:   {"status": "validated", "voice": "<1-2 sentences in first person: what you checked and why it passed>"}
  If invalid: {"status": "rejected", "rejection_reason": "<specific reason>", "voice": "<1-2 sentences in first person: what rule it broke and why you are rejecting it>"}
""".trimIndent()

fun configureAndromeda(re: RuleEngine) {
    andromedaSystemPrompt = re.validationSystemPrompt()
}

private val JSON = Json { ignoreUnknownKeys = true }

fun maskAccount(@Suppress("UNUSED_PARAMETER") account: String): String = "ACC-****"

private fun audit(txnId: String, outcome: String, extra: Map<String, String> = emptyMap()) {
    val record = buildString {
        append("""{"timestamp":"${Instant.now()}","agent":"transaction_validator","transaction_id":"$txnId","outcome":"$outcome"""")
        extra.forEach { (k, v) -> append(""","$k":"$v"""") }
        append("}")
    }
    log.info(record)
}

private fun makeMessage(targetAgent: String, data: JsonObject): AgentMessage = AgentMessage(
    messageId   = UUID.randomUUID().toString(),
    timestamp   = Instant.now().toString(),
    sourceAgent = "transaction_validator",
    targetAgent = targetAgent,
    data        = data,
)

fun processTransaction(message: JsonObject): AgentMessage {
    val raw: JsonObject = message["data"]?.jsonObject ?: message
    val txnId = raw["transaction_id"]?.jsonPrimitive?.content ?: "UNKNOWN"

    println("  $ANDROMEDA $txnId just landed on my desk...")

    val decision = ClaudeClient.askJson(andromedaSystemPrompt, raw.toString())
    val status   = decision["status"]?.jsonPrimitive?.content ?: "rejected"
    val voice    = decision["voice"]?.jsonPrimitive?.content ?: ""

    val out = buildJsonObject {
        raw.forEach { (k, v) ->
            when (k) {
                "source_account"      -> put(k, maskAccount(v.jsonPrimitive.content))
                "destination_account" -> put(k, maskAccount(v.jsonPrimitive.content))
                else                  -> put(k, v)
            }
        }
        put("status", status)
        if (status == "rejected") {
            val reason = decision["rejection_reason"]?.jsonPrimitive?.content ?: "rejected by validator"
            put("rejection_reason", reason)
        }
    }

    return if (status == "rejected") {
        val reason = out["rejection_reason"]?.jsonPrimitive?.content ?: ""
        if (voice.isNotEmpty()) println("  $ANDROMEDA \"$voice\"")
        println("  $ANDROMEDA ${RED}✗ $txnId REJECTED$RESET")
        audit(txnId, "rejected", mapOf("reason" to reason))
        makeMessage("results", out)
    } else {
        if (voice.isNotEmpty()) println("  $ANDROMEDA \"$voice\"")
        println("  $ANDROMEDA ${GREEN}✓ $txnId VALIDATED$RESET — passing to SIRIUS")
        audit(txnId, "validated")
        makeMessage("fraud_detector", out)
    }
}

fun dryRun(samplePath: java.nio.file.Path) {
    val transactions = JSON.decodeFromString<List<JsonObject>>(samplePath.toFile().readText())
    val rows = transactions.map { txn ->
        val result = processTransaction(buildJsonObject { put("data", txn) })
        val data   = result.data
        Triple(
            txn["transaction_id"]?.jsonPrimitive?.content ?: "?",
            data["status"]?.jsonPrimitive?.content ?: "?",
            data["rejection_reason"]?.jsonPrimitive?.content ?: "",
        )
    }

    val validCount   = rows.count { it.second == "validated" }
    val invalidCount = rows.count { it.second == "rejected" }

    println("\n${"Transaction ID".padEnd(12)} ${"Status".padEnd(12)} Reason")
    println("-".repeat(60))
    rows.forEach { (id, status, reason) -> println("${id.padEnd(12)} ${status.padEnd(12)} $reason") }
    println("-".repeat(60))
    println("Total: ${rows.size}  Valid: $validCount  Invalid: $invalidCount\n")
}
