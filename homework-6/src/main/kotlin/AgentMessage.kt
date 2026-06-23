import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AgentMessage(
    @SerialName("message_id") val messageId: String,
    val timestamp: String,
    @SerialName("source_agent") val sourceAgent: String,
    @SerialName("target_agent") val targetAgent: String,
    @SerialName("message_type") val messageType: String = "transaction",
    val data: JsonObject,
)

@Serializable
data class Transaction(
    @SerialName("transaction_id") val transactionId: String,
    val timestamp: String,
    @SerialName("source_account") val sourceAccount: String,
    @SerialName("destination_account") val destinationAccount: String,
    val amount: String,
    val currency: String,
    @SerialName("transaction_type") val transactionType: String,
    val description: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class PipelineSummary(
    val total: Int,
    val settled: Int,
    val flagged: Int,
    val rejected: Int,
    @SerialName("processed_at") val processedAt: String,
)