package agents

import AgentMessage
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

private val log = Logger.getLogger("fraud_detector")

// SIRIUS — the sky's brightest eye: nothing suspicious escapes its light
private const val E = ""
private const val SIRIUS = "${E}[1;33m[SIRIUS]${E}[0m"
private const val GREEN  = "${E}[32m"
private const val YELLOW = "${E}[33m"
private const val RED    = "${E}[31m"
private const val RESET  = "${E}[0m"

private val SYSTEM_PROMPT = """
You are a fraud detection analyst at a bank. Assess this transaction's risk score.

Apply ALL of these scoring rules exactly:
- Amount > ${'$'}10,000: +0.4 points — label "high_value"
- Amount ${'$'}9,000–${'$'}9,999.99 (structuring attempt just below reporting threshold): +0.3 — label "structuring"
- Transaction UTC hour is 22, 23, or 0–5 (off-hours activity): +0.3 — label "off_hours"
- metadata.country is not "US" (cross-border transaction): +0.2 — label "cross_border"
- Cap total risk score at 1.0

DECISION RULE: score >= 0.4 → "flagged"; score < 0.4 → "fraud_cleared"

Return ONLY a JSON object — no explanation, no markdown:
{
  "risk_score": <float between 0.0 and 1.0>,
  "risk_rules": [<list of triggered rule labels>],
  "status": "flagged" or "fraud_cleared",
  "analysis": "<one sentence explaining the key risk factors or why it is clean>"
}
""".trimIndent()

private fun audit(txnId: String, outcome: String, extra: Map<String, String> = emptyMap()) {
    val record = buildString {
        append("""{"timestamp":"${Instant.now()}","agent":"fraud_detector","transaction_id":"$txnId","outcome":"$outcome"""")
        extra.forEach { (k, v) -> append(""","$k":"$v"""") }
        append("}")
    }
    log.info(record)
}

private fun makeMessage(targetAgent: String, data: JsonObject): AgentMessage = AgentMessage(
    messageId = UUID.randomUUID().toString(),
    timestamp = Instant.now().toString(),
    sourceAgent = "fraud_detector",
    targetAgent = targetAgent,
    data = data,
)

fun scoreTransaction(data: JsonObject): Pair<Double, List<String>> {
    val decision = ClaudeClient.askJson(SYSTEM_PROMPT, data.toString())
    val score = decision["risk_score"]?.jsonPrimitive?.double ?: 0.0
    val rules = decision["risk_rules"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    return score to rules
}

fun processFraudTransaction(message: JsonObject): AgentMessage {
    val data = (message["data"]?.jsonObject ?: message).toMutableMap()
    val txnId = data["transaction_id"]?.jsonPrimitive?.content ?: "UNKNOWN"

    println("  $SIRIUS Scanning $txnId for anomalies...")

    val decision = ClaudeClient.askJson(SYSTEM_PROMPT, JsonObject(data).toString())
    val score = decision["risk_score"]?.jsonPrimitive?.double ?: 0.0
    val rules = decision["risk_rules"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    val analysis = decision["analysis"]?.jsonPrimitive?.content ?: ""

    val out = buildJsonObject {
        data.forEach { (k, v) -> put(k, v) }
        put("risk_score", score.coerceIn(0.0, 1.0).let { Math.round(it * 100).toDouble() / 100 })
        put("risk_rules", buildJsonArray { rules.forEach { add(it) } })
        if (analysis.isNotEmpty()) put("fraud_analysis", analysis)
        if (score >= 0.4) put("status", "flagged") else put("status", "fraud_cleared")
    }

    return if (score >= 0.4) {
        val ruleStr = if (rules.isEmpty()) "no labels" else rules.joinToString(", ")
        println("  $SIRIUS ${RED}⚠ $txnId FLAGGED$RESET — risk ${"%.1f".format(score)} [$ruleStr]")
        if (analysis.isNotEmpty()) println("  $SIRIUS     $YELLOW↳ $analysis$RESET")
        audit(txnId, "flagged", mapOf("risk_score" to score.toString(), "rules" to rules.toString()))
        makeMessage("results", out)
    } else {
        println("  $SIRIUS ${GREEN}✓ $txnId clear$RESET — risk ${"%.1f".format(score)}, no triggers")
        if (analysis.isNotEmpty()) println("  $SIRIUS     ↳ $analysis")
        audit(txnId, "fraud_cleared", mapOf("risk_score" to score.toString()))
        makeMessage("settlement_processor", out)
    }
}