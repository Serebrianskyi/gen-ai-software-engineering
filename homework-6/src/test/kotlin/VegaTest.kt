import agents.processVega
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private val BASE_CLEARED = buildJsonObject {
    put("transaction_id", "TXN-V01")
    put("timestamp", "2026-01-01T10:00:00Z")
    put("amount", "1500.00")
    put("currency", "USD")
    put("status", "fraud_cleared")
    put("risk_score", 0.0)
    put("risk_rules", buildJsonArray {})
    put("source_account", "ACC-****")
    put("destination_account", "ACC-****")
}

private val BASE_FLAGGED = buildJsonObject {
    BASE_CLEARED.forEach { (k, v) -> put(k, v) }
    put("status", "flagged")
    put("risk_score", 0.7)
    put("risk_rules", buildJsonArray { add("high_value"); add("off_hours") })
}

class VegaTest {

    // ── fraud_cleared pass-through ────────────────────────────────────────────

    @Test fun `fraud cleared transaction is not terminal`() {
        val (_, terminal) = processVega(BASE_CLEARED)
        assertFalse(terminal)
    }

    @Test fun `fraud cleared data is preserved unchanged`() {
        val (outData, _) = processVega(BASE_CLEARED)
        assertEquals("fraud_cleared", outData["status"]?.jsonPrimitive?.content)
        assertEquals("TXN-V01", outData["transaction_id"]?.jsonPrimitive?.content)
    }

    @Test fun `fraud cleared does not add compliance fields`() {
        val (outData, _) = processVega(BASE_CLEARED)
        assertNull(outData["compliance_report"])
        assertNull(outData["recommended_action"])
    }

    // ── flagged SAR generation ────────────────────────────────────────────────

    @Test fun `flagged transaction is terminal`() {
        val (_, terminal) = processVega(BASE_FLAGGED)
        assertTrue(terminal)
    }

    @Test fun `flagged transaction gets compliance report`() {
        val (outData, _) = processVega(BASE_FLAGGED)
        val report = outData["compliance_report"]?.jsonPrimitive?.content
        assertNotNull(report)
        assertTrue(report!!.isNotEmpty())
    }

    @Test fun `flagged transaction gets recommended action`() {
        val (outData, _) = processVega(BASE_FLAGGED)
        val action = outData["recommended_action"]?.jsonPrimitive?.content
        assertNotNull(action)
        assertTrue(action in listOf("file_sar", "escalate_to_compliance", "hold_for_review"))
    }

    @Test fun `flagged transaction gets risk summary`() {
        val (outData, _) = processVega(BASE_FLAGGED)
        assertNotNull(outData["risk_summary"]?.jsonPrimitive?.content)
    }

    @Test fun `flagged transaction preserves original fields`() {
        val (outData, _) = processVega(BASE_FLAGGED)
        assertEquals("TXN-V01", outData["transaction_id"]?.jsonPrimitive?.content)
        assertEquals("flagged", outData["status"]?.jsonPrimitive?.content)
        assertEquals(0.7, outData["risk_score"]?.jsonPrimitive?.double)
    }

    @Test fun `flagged transaction preserves risk rules`() {
        val (outData, _) = processVega(BASE_FLAGGED)
        val rules = outData["risk_rules"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        assertTrue("high_value" in rules)
        assertTrue("off_hours" in rules)
    }
}
