import agents.processTransaction
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files

private val BASE_TXN = buildJsonObject {
    put("transaction_id", "TXN-TEST")
    put("timestamp", "2026-01-01T10:00:00Z")
    put("source_account", "ACC-1001")
    put("destination_account", "ACC-2001")
    put("amount", "1500.00")
    put("currency", "USD")
    put("transaction_type", "transfer")
    put("metadata", buildJsonObject { put("channel", "online"); put("country", "US") })
}

private fun msg(txn: JsonObject) = buildJsonObject { put("data", txn) }

private fun withField(key: String, value: String) = buildJsonObject {
    BASE_TXN.forEach { (k, v) -> put(k, v) }
    put(key, value)
}

private fun withoutField(vararg keys: String) = buildJsonObject {
    BASE_TXN.forEach { (k, v) -> if (k !in keys) put(k, v) }
}

class TransactionValidatorTest {

    // --- happy path ---

    @Test fun `valid transaction has status validated`() {
        val result = processTransaction(msg(BASE_TXN))
        assertEquals("validated", result.data["status"]?.jsonPrimitive?.content)
    }

    @Test fun `valid transaction routes to fraud detector`() {
        val result = processTransaction(msg(BASE_TXN))
        assertEquals("transaction_validator", result.sourceAgent)
        assertEquals("fraud_detector", result.targetAgent)
    }

    @Test fun `message contains required schema fields`() {
        val result = processTransaction(msg(BASE_TXN))
        assertNotNull(result.messageId)
        assertNotNull(result.timestamp)
        assertEquals("transaction", result.messageType)
        assertNotNull(result.data)
    }

    @Test fun `accounts are masked in output`() {
        val result = processTransaction(msg(BASE_TXN))
        assertEquals("ACC-****", result.data["source_account"]?.jsonPrimitive?.content)
        assertEquals("ACC-****", result.data["destination_account"]?.jsonPrimitive?.content)
    }

    @Test fun `raw dict without data wrapper is accepted`() {
        val result = processTransaction(BASE_TXN)
        assertEquals("validated", result.data["status"]?.jsonPrimitive?.content)
    }

    // --- missing fields ---

    @Test fun `missing transaction_id is rejected`() {
        val result = processTransaction(msg(withoutField("transaction_id")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("transaction_id") == true)
    }

    @Test fun `missing source and destination accounts rejected with missing required fields`() {
        val result = processTransaction(msg(withoutField("source_account", "destination_account")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("missing required fields") == true)
    }

    @Test fun `rejected transaction routes to results`() {
        val result = processTransaction(msg(withField("currency", "XYZ")))
        assertEquals("results", result.targetAgent)
    }

    // --- amount validation ---

    @Test fun `non-numeric amount is rejected`() {
        val result = processTransaction(msg(withField("amount", "not-a-number")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("invalid amount format") == true)
    }

    @Test fun `zero amount is rejected`() {
        val result = processTransaction(msg(withField("amount", "0")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("non-positive amount") == true)
    }

    @Test fun `negative amount is rejected`() {
        val result = processTransaction(msg(withField("amount", "-100.00")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("non-positive amount") == true)
    }

    @Test fun `very small positive amount is valid`() {
        val result = processTransaction(msg(withField("amount", "0.01")))
        assertEquals("validated", result.data["status"]?.jsonPrimitive?.content)
    }

    // --- currency validation ---

    @Test fun `invalid currency XYZ is rejected`() {
        val result = processTransaction(msg(withField("currency", "XYZ")))
        assertEquals("rejected", result.data["status"]?.jsonPrimitive?.content)
        assertTrue(result.data["rejection_reason"]?.jsonPrimitive?.content?.contains("XYZ") == true)
    }

    @ParameterizedTest
    @ValueSource(strings = ["USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "SGD", "HKD", "NOK", "SEK", "DKK"])
    fun `all valid ISO 4217 currencies are accepted`(currency: String) {
        val result = processTransaction(msg(withField("currency", currency)))
        assertEquals("validated", result.data["status"]?.jsonPrimitive?.content)
    }

    // --- dry-run mode ---

    @Test fun `dry run reports correct counts`() {
        val tmp = Files.createTempDirectory("hw6test")
        val sample = tmp.resolve("sample.json")
        sample.toFile().writeText(Json.encodeToString(JsonArray.serializer(), buildJsonArray {
            add(BASE_TXN)
            add(withField("currency", "XYZ"))
        }))
        // Just test it doesn't throw and produces output
        val out = java.io.ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(java.io.PrintStream(out))
        try {
            agents.dryRun(sample)
        } finally {
            System.setOut(oldOut)
        }
        val output = out.toString()
        assertTrue(output.contains("Total: 2"))
        assertTrue(output.contains("Valid: 1"))
        assertTrue(output.contains("Invalid: 1"))
    }

    @Test fun `dry run shows transaction id`() {
        val tmp = Files.createTempDirectory("hw6test")
        val sample = tmp.resolve("sample.json")
        sample.toFile().writeText(Json.encodeToString(JsonArray.serializer(), buildJsonArray { add(BASE_TXN) }))
        val out = java.io.ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(java.io.PrintStream(out))
        try {
            agents.dryRun(sample)
        } finally {
            System.setOut(oldOut)
        }
        assertTrue(out.toString().contains("TXN-TEST"))
    }
}