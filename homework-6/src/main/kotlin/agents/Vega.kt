package agents

import AgentMessage
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.logging.Logger

private val log = Logger.getLogger("vega")

// VEGA — compliance star: files SARs on flagged transactions, passes cleared ones through
private const val E      = ""
private const val VEGA   = "${E}[1;35m[VEGA]${E}[0m"
private const val GREEN  = "${E}[32m"
private const val YELLOW = "${E}[33m"
private const val RESET  = "${E}[0m"

private val SYSTEM_PROMPT = """
You are VEGA, the compliance star of the pipeline. You speak in first person.

You receive a transaction that has already been validated and fraud-scored.
Your job depends on the transaction's status field:

- If status is "flagged": generate a structured compliance report for regulatory review.
- If status is "fraud_cleared": respond with {"action": "pass_through"} — nothing more.

For flagged transactions, return ONLY this JSON object — no explanation, no markdown:
{
  "compliance_report": "<2–3 sentences describing the suspicious activity and why it warrants review>",
  "risk_summary": "<comma-separated key risk factors that triggered the flag>",
  "recommended_action": "<exactly one of: file_sar, escalate_to_compliance, hold_for_review>",
  "voice": "<1–2 sentences in first person about your compliance finding>"
}

For pass-through, return ONLY: {"action": "pass_through"}
""".trimIndent()

fun processVega(data: JsonObject): Pair<JsonObject, Boolean> {
    val txnId  = data["transaction_id"]?.jsonPrimitive?.content ?: "UNKNOWN"
    val status = data["status"]?.jsonPrimitive?.content ?: "unknown"

    println("  $VEGA $txnId arrived. Status: $status")

    if (status != "flagged") {
        println("  $VEGA ${GREEN}→ $txnId is clean — passing to settlement$RESET")
        log.info("""{"timestamp":"${Instant.now()}","agent":"vega","transaction_id":"$txnId","outcome":"pass_through"}""")
        return data to false
    }

    val decision = ClaudeClient.askJson(SYSTEM_PROMPT, data.toString())
    val voice    = decision["voice"]?.jsonPrimitive?.content ?: ""

    if (voice.isNotEmpty()) println("  $VEGA \"$voice\"")

    val out = buildJsonObject {
        data.forEach { (k, v) -> put(k, v) }
        decision["compliance_report"]?.let { put("compliance_report", it) }
        decision["risk_summary"]?.let     { put("risk_summary", it) }
        decision["recommended_action"]?.let { put("recommended_action", it) }
    }

    val action = decision["recommended_action"]?.jsonPrimitive?.content ?: "hold_for_review"
    println("  $VEGA ${YELLOW}⚑ $txnId — SAR filed [$action]$RESET")
    log.info("""{"timestamp":"${Instant.now()}","agent":"vega","transaction_id":"$txnId","outcome":"sar_filed","action":"$action"}""")

    return out to true  // terminal — flagged transactions don't proceed to settlement
}
