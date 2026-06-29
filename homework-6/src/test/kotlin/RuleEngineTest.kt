import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class RuleEngineTest {

    private fun engine(
        fields: List<String> = listOf("transaction_id", "amount", "currency"),
        currencies: List<String> = listOf("USD", "EUR"),
        threshold: Double = 0.4,
        rules: List<FraudRule> = listOf(FraudRule("high_value", "Amount > \$10,000", 0.4)),
    ) = RuleEngine.of(
        RulesConfig(
            validation = ValidationConfig(fields, currencies, true),
            fraud      = FraudConfig(threshold, rules),
        )
    )

    // ── validationSystemPrompt ────────────────────────────────────────────────

    @Test fun `validation prompt contains all required field names`() {
        val prompt = engine(fields = listOf("transaction_id", "timestamp", "amount")).validationSystemPrompt()
        assertTrue(prompt.contains("transaction_id"))
        assertTrue(prompt.contains("timestamp"))
        assertTrue(prompt.contains("amount"))
    }

    @Test fun `validation prompt contains approved currencies`() {
        val prompt = engine(currencies = listOf("USD", "GBP", "JPY")).validationSystemPrompt()
        assertTrue(prompt.contains("USD"))
        assertTrue(prompt.contains("GBP"))
        assertTrue(prompt.contains("JPY"))
    }

    @Test fun `validation prompt mentions ANDROMEDA persona`() {
        val prompt = engine().validationSystemPrompt()
        assertTrue(prompt.contains("ANDROMEDA"))
    }

    @Test fun `validation prompt instructs returning JSON only`() {
        val prompt = engine().validationSystemPrompt()
        assertTrue(prompt.contains("Return ONLY a JSON object"))
    }

    // ── fraudSystemPrompt ─────────────────────────────────────────────────────

    @Test fun `fraud prompt contains SIRIUS persona`() {
        val prompt = engine().fraudSystemPrompt()
        assertTrue(prompt.contains("SIRIUS"))
    }

    @Test fun `fraud prompt contains flag threshold`() {
        val prompt = engine(threshold = 0.5).fraudSystemPrompt()
        assertTrue(prompt.contains("0.5"))
    }

    @Test fun `fraud prompt lists rule descriptions`() {
        val rules = listOf(
            FraudRule("high_value", "Amount > \$10,000", 0.4),
            FraudRule("off_hours",  "UTC hour is 22-5",  0.3),
        )
        val prompt = engine(rules = rules).fraudSystemPrompt()
        assertTrue(prompt.contains("Amount > \$10,000"))
        assertTrue(prompt.contains("UTC hour is 22-5"))
    }

    @Test fun `fraud prompt lists rule ids as labels`() {
        val rules = listOf(FraudRule("my_rule", "Description", 0.2))
        val prompt = engine(rules = rules).fraudSystemPrompt()
        assertTrue(prompt.contains("my_rule"))
    }

    @Test fun `fraud prompt contains rule scores`() {
        val rules = listOf(FraudRule("r", "desc", 0.35))
        val prompt = engine(rules = rules).fraudSystemPrompt()
        assertTrue(prompt.contains("0.35"))
    }

    // ── load from file ────────────────────────────────────────────────────────

    @Test fun `load from valid rules json`() {
        val tmp = Files.createTempDirectory("ruleengine-test")
        val file = tmp.resolve("rules.json")
        file.toFile().writeText("""
            {
              "validation": {
                "required_fields": ["id", "amount"],
                "approved_currencies": ["USD"],
                "amount_must_be_positive": true
              },
              "fraud": {
                "flag_threshold": 0.3,
                "rules": [{"id": "r", "description": "d", "score": 0.3}]
              }
            }
        """.trimIndent())
        val re = RuleEngine.load(file)
        assertEquals(0.3, re.rules.fraud.flagThreshold)
        assertEquals(listOf("id", "amount"), re.rules.validation.requiredFields)
    }

    @Test fun `loaded engine generates prompts without throwing`() {
        val tmp = Files.createTempDirectory("ruleengine-test2")
        val file = tmp.resolve("rules.json")
        file.toFile().writeText("""
            {
              "validation": {"required_fields":["id"],"approved_currencies":["USD"],"amount_must_be_positive":true},
              "fraud": {"flag_threshold":0.4,"rules":[{"id":"r","description":"d","score":0.4}]}
            }
        """.trimIndent())
        val re = RuleEngine.load(file)
        assertDoesNotThrow { re.validationSystemPrompt() }
        assertDoesNotThrow { re.fraudSystemPrompt() }
    }
}
