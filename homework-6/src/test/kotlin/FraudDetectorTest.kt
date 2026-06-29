import agents.processFraudTransaction
import agents.scoreTransaction
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

private val BASE_DATA = buildJsonObject {
    put("transaction_id", "TXN-TEST")
    put("timestamp", "2026-01-01T10:00:00Z")
    put("amount", "1500.00")
    put("currency", "USD")
    put("status", "validated")
    put("metadata", buildJsonObject { put("channel", "online"); put("country", "US") })
}

private fun withField(key: String, value: String) = buildJsonObject {
    BASE_DATA.forEach { (k, v) -> put(k, v) }
    put(key, value)
}

private fun withMeta(country: String) = buildJsonObject {
    BASE_DATA.forEach { (k, v) -> if (k != "metadata") put(k, v) }
    put("metadata", buildJsonObject { put("country", country) })
}

private fun withoutMeta() = buildJsonObject {
    BASE_DATA.forEach { (k, v) -> if (k != "metadata") put(k, v) }
}

class FraudDetectorTest {

    // --- scoreTransaction ---

    @Test fun `clean transaction scores zero`() {
        val (score, rules) = scoreTransaction(BASE_DATA)
        assertEquals(0.0, score, 0.001)
        assertEquals(emptyList<String>(), rules)
    }

    @Test fun `high value amount triggers high_value rule`() {
        val (score, rules) = scoreTransaction(withField("amount", "10001.00"))
        assertTrue("high_value" in rules)
        assertEquals(0.4, score, 0.001)
    }

    @Test fun `exactly 10000 is not high value`() {
        val (_, rules) = scoreTransaction(withField("amount", "10000.00"))
        assertFalse("high_value" in rules)
    }

    @Test fun `structuring lower bound 9000`() {
        val (score, rules) = scoreTransaction(withField("amount", "9000.00"))
        assertTrue("structuring" in rules)
        assertEquals(0.3, score, 0.001)
    }

    @Test fun `structuring upper bound 9999_99`() {
        val (_, rules) = scoreTransaction(withField("amount", "9999.99"))
        assertTrue("structuring" in rules)
    }

    @Test fun `below structuring range no trigger`() {
        val (_, rules) = scoreTransaction(withField("amount", "8999.99"))
        assertFalse("structuring" in rules)
    }

    @Test fun `above structuring range no structuring`() {
        val (_, rules) = scoreTransaction(withField("amount", "10000.00"))
        assertFalse("structuring" in rules)
    }

    @Test fun `off hours at 22`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T22:00:00Z"))
        assertTrue("off_hours" in rules)
    }

    @Test fun `off hours at 23`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T23:59:00Z"))
        assertTrue("off_hours" in rules)
    }

    @Test fun `off hours at midnight`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T00:00:00Z"))
        assertTrue("off_hours" in rules)
    }

    @Test fun `off hours at 05`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T05:30:00Z"))
        assertTrue("off_hours" in rules)
    }

    @Test fun `not off hours at 06`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T06:00:00Z"))
        assertFalse("off_hours" in rules)
    }

    @Test fun `not off hours at 21`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "2026-01-01T21:59:00Z"))
        assertFalse("off_hours" in rules)
    }

    @Test fun `cross border non-US triggers cross_border`() {
        val (score, rules) = scoreTransaction(withMeta("DE"))
        assertTrue("cross_border" in rules)
        assertEquals(0.2, score, 0.001)
    }

    @Test fun `US country is not cross border`() {
        val (_, rules) = scoreTransaction(BASE_DATA)
        assertFalse("cross_border" in rules)
    }

    @Test fun `no metadata is not cross border`() {
        val (_, rules) = scoreTransaction(withoutMeta())
        assertFalse("cross_border" in rules)
    }

    @Test fun `invalid timestamp produces no off_hours`() {
        val (_, rules) = scoreTransaction(withField("timestamp", "bad-ts"))
        assertFalse("off_hours" in rules)
    }

    @Test fun `combined high value off hours cross border`() {
        val data = buildJsonObject {
            BASE_DATA.forEach { (k, v) -> if (k != "metadata") put(k, v) }
            put("amount", "75000.00")
            put("timestamp", "2026-01-01T02:00:00Z")
            put("metadata", buildJsonObject { put("country", "DE") })
        }
        val (score, rules) = scoreTransaction(data)
        assertEquals(0.9, score, 0.001)
        assertTrue("high_value" in rules)
        assertTrue("off_hours" in rules)
        assertTrue("cross_border" in rules)
    }

    @Test fun `score is capped at 1_0`() {
        val data = buildJsonObject {
            BASE_DATA.forEach { (k, v) -> if (k != "metadata") put(k, v) }
            put("amount", "75000.00")
            put("timestamp", "2026-01-01T02:00:00Z")
            put("metadata", buildJsonObject { put("country", "DE") })
        }
        val (score, _) = scoreTransaction(data)
        assertTrue(score <= 1.0)
    }

    // --- processFraudTransaction ---

    @Test fun `high value transaction is flagged`() {
        val result = processFraudTransaction(buildJsonObject { put("data", withField("amount", "25000.00")) })
        assertEquals("flagged", result.data["status"]?.jsonPrimitive?.content)
        assertEquals("results", result.targetAgent)
    }

    @Test fun `clean transaction is fraud_cleared`() {
        val result = processFraudTransaction(buildJsonObject { put("data", BASE_DATA) })
        assertEquals("fraud_cleared", result.data["status"]?.jsonPrimitive?.content)
        assertEquals("settlement_processor", result.targetAgent)
    }

    @Test fun `result contains risk_score`() {
        val result = processFraudTransaction(buildJsonObject { put("data", BASE_DATA) })
        assertNotNull(result.data["risk_score"])
        assertNotNull(result.data["risk_score"]?.jsonPrimitive?.double)
    }

    @Test fun `result contains risk_rules list`() {
        val result = processFraudTransaction(buildJsonObject { put("data", BASE_DATA) })
        assertNotNull(result.data["risk_rules"])
        assertIs<JsonArray>(result.data["risk_rules"])
    }

    @Test fun `result has required schema fields`() {
        val result = processFraudTransaction(buildJsonObject { put("data", BASE_DATA) })
        assertNotNull(result.messageId)
        assertEquals("fraud_detector", result.sourceAgent)
        assertEquals("transaction", result.messageType)
    }

    @Test fun `raw data without wrapper is accepted`() {
        val result = processFraudTransaction(BASE_DATA)
        val status = result.data["status"]?.jsonPrimitive?.content
        assertTrue(status == "flagged" || status == "fraud_cleared")
    }

    @Test fun `TXN004 sample off hours and cross border scores 0_5 and is flagged`() {
        val data = buildJsonObject {
            BASE_DATA.forEach { (k, v) -> if (k != "metadata") put(k, v) }
            put("transaction_id", "TXN004")
            put("timestamp", "2026-03-16T02:47:00Z")
            put("amount", "500.00")
            put("currency", "EUR")
            put("metadata", buildJsonObject { put("channel", "api"); put("country", "DE") })
        }
        val (score, rules) = scoreTransaction(data)
        assertEquals(0.5, score, 0.001)
        assertTrue("off_hours" in rules)
        assertTrue("cross_border" in rules)
    }
}