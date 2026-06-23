import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

@Serializable
data class FraudRule(
    val id: String,
    val description: String,
    val score: Double,
)

@Serializable
data class FraudConfig(
    @SerialName("flag_threshold") val flagThreshold: Double,
    val rules: List<FraudRule>,
)

@Serializable
data class ValidationConfig(
    @SerialName("required_fields")         val requiredFields:       List<String>,
    @SerialName("approved_currencies")     val approvedCurrencies:   List<String>,
    @SerialName("amount_must_be_positive") val amountMustBePositive: Boolean = true,
)

@Serializable
data class RulesConfig(
    val validation: ValidationConfig,
    val fraud: FraudConfig,
)

class RuleEngine private constructor(val rules: RulesConfig) {

    fun validationSystemPrompt(): String {
        val fields = rules.validation.requiredFields.joinToString(", ")
        val currencies = rules.validation.approvedCurrencies.joinToString(", ")
        return """
You are ANDROMEDA, a galaxy-scale compliance system at a bank. You speak in first person.

Check the transaction and apply ALL of the following rules:
1. All required fields must be present: $fields
2. Amount must be a valid positive number greater than zero
3. Currency must be a valid ISO 4217 code from this approved list: $currencies

Return ONLY a JSON object — no explanation, no markdown:
  If valid:   {"status": "validated", "voice": "<1-2 sentences in first person: what you checked and why it passed>"}
  If invalid: {"status": "rejected", "rejection_reason": "<specific reason>", "voice": "<1-2 sentences in first person: what rule it broke and why you are rejecting it>"}
""".trimIndent()
    }

    fun fraudSystemPrompt(): String {
        val threshold = rules.fraud.flagThreshold
        val ruleLines = rules.fraud.rules.joinToString("\n") { r ->
            "- ${r.description}: +${r.score} points — label \"${r.id}\""
        }
        return """
You are SIRIUS, the sharpest fraud detection eye in the sky. You speak in first person.

Assess this transaction's risk score applying ALL of these rules exactly:
$ruleLines
- Cap total risk score at 1.0

DECISION RULE: score >= $threshold → "flagged"; score < $threshold → "fraud_cleared"

Return ONLY a JSON object — no explanation, no markdown:
{
  "risk_score": <float between 0.0 and 1.0>,
  "risk_rules": [<list of triggered rule labels>],
  "status": "flagged" or "fraud_cleared",
  "voice": "<1-2 sentences in first person: what rules you ran, what triggered, and your verdict>"
}
""".trimIndent()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(path: Path): RuleEngine = RuleEngine(json.decodeFromString(path.readText()))

        fun of(config: RulesConfig): RuleEngine = RuleEngine(config)
    }
}
