import agents.generateSummary
import agents.settleTransaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

private val BASE_MESSAGE = buildJsonObject {
    put("message_id", "test-id")
    put("timestamp", "2026-01-01T10:00:00Z")
    put("source_agent", "fraud_detector")
    put("target_agent", "settlement_processor")
    put("message_type", "transaction")
    put("data", buildJsonObject {
        put("transaction_id", "TXN-TEST")
        put("amount", "1500.00")
        put("currency", "USD")
        put("status", "fraud_cleared")
        put("source_account", "ACC-****")
        put("destination_account", "ACC-****")
        put("risk_score", 0.0)
    })
}

private val JSON = Json { prettyPrint = true }

class SettlementProcessorTest {

    @Test fun `settle transaction sets status to settled`() {
        val result = settleTransaction(BASE_MESSAGE)
        assertEquals("settled", result.data["status"]?.jsonPrimitive?.content)
    }

    @Test fun `settle transaction routes to results`() {
        val result = settleTransaction(BASE_MESSAGE)
        assertEquals("results", result.targetAgent)
    }

    @Test fun `settle transaction sets source agent`() {
        val result = settleTransaction(BASE_MESSAGE)
        assertEquals("settlement_processor", result.sourceAgent)
    }

    @Test fun `settled result includes settled_at timestamp`() {
        val result = settleTransaction(BASE_MESSAGE)
        val settledAt = result.data["settled_at"]?.jsonPrimitive?.content
        assertNotNull(settledAt)
        assertTrue(settledAt!!.isNotEmpty())
    }

    @Test fun `settled result has message schema fields`() {
        val result = settleTransaction(BASE_MESSAGE)
        assertNotNull(result.messageId)
        assertNotNull(result.timestamp)
        assertEquals("transaction", result.messageType)
    }

    @Test fun `settled result preserves transaction id`() {
        val result = settleTransaction(BASE_MESSAGE)
        assertEquals("TXN-TEST", result.data["transaction_id"]?.jsonPrimitive?.content)
    }

    @Test fun `raw data without wrapper is accepted`() {
        val raw = BASE_MESSAGE["data"]!!.jsonObject
        val result = settleTransaction(raw)
        assertEquals("settled", result.data["status"]?.jsonPrimitive?.content)
    }

    // --- generateSummary ---

    private fun writeResult(dir: java.nio.file.Path, name: String, status: String) {
        val msg = buildJsonObject {
            put("message_id", "x")
            put("source_agent", "test")
            put("target_agent", "results")
            put("timestamp", "2026-01-01T00:00:00Z")
            put("message_type", "transaction")
            put("data", buildJsonObject {
                put("transaction_id", name)
                put("status", status)
            })
        }
        dir.resolve("$name.json").toFile().writeText(JSON.encodeToString(msg))
    }

    @Test fun `generate summary counts settled flagged rejected`() {
        val tmp = Files.createTempDirectory("hw6summary")
        writeResult(tmp, "TXN001", "settled")
        writeResult(tmp, "TXN002", "flagged")
        writeResult(tmp, "TXN003", "rejected")
        writeResult(tmp, "TXN004", "settled")
        val summary = generateSummary(tmp)
        assertEquals(4, summary.total)
        assertEquals(2, summary.settled)
        assertEquals(1, summary.flagged)
        assertEquals(1, summary.rejected)
    }

    @Test fun `generate summary skips pipeline_summary json`() {
        val tmp = Files.createTempDirectory("hw6summary2")
        writeResult(tmp, "TXN001", "settled")
        tmp.resolve("pipeline_summary.json").toFile().writeText("""{"total":99}""")
        val summary = generateSummary(tmp)
        assertEquals(1, summary.total)
    }

    @Test fun `generate summary empty dir returns zeros`() {
        val tmp = Files.createTempDirectory("hw6empty")
        val summary = generateSummary(tmp)
        assertEquals(0, summary.total)
        assertEquals(0, summary.settled)
    }

    @Test fun `generate summary includes processed_at`() {
        val tmp = Files.createTempDirectory("hw6ts")
        val summary = generateSummary(tmp)
        assertNotNull(summary.processedAt)
        assertTrue(summary.processedAt.isNotEmpty())
    }

    @Test fun `generate summary ignores malformed json`() {
        val tmp = Files.createTempDirectory("hw6malformed")
        tmp.resolve("bad.json").toFile().writeText("NOT JSON")
        writeResult(tmp, "TXN001", "settled")
        val summary = generateSummary(tmp)
        assertEquals(1, summary.total)
    }
}