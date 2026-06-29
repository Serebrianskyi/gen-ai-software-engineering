import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.writeText

private val routingJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
private val routingLog  = Logger.getLogger("routing")

/**
 * Entry point for every agent's HTTP handler.
 *
 * 1. Guards against max_hops.
 * 2. Checks this agent's own prerequisites vs processedBy.
 *    If any are missing: rewrites the queue so they run first, re-inserts self, forwards — no processing.
 * 3. If all prerequisites are satisfied: calls [process], then routes the result.
 *
 * @param self       this agent's name (e.g. "andromeda")
 * @param incoming   message as received via HTTP
 * @param config     loaded pipeline-config.json
 * @param resultsDir directory where terminal results are written
 * @param process    the agent's business logic — returns (outData, terminal).
 *                   terminal=true means skip remaining queue and write to results immediately.
 */
fun handle(
    self: String,
    incoming: AgentMessage,
    config: PipelineConfig,
    resultsDir: Path,
    process: (AgentMessage) -> Pair<JsonObject, Boolean>,
) {
    val newHops = incoming.hops + 1
    if (newHops > config.maxHops) {
        routingLog.severe("[$self] max_hops (${config.maxHops}) exceeded for txn ${incoming.data["transaction_id"]?.jsonPrimitive?.content}")
        writeResult(resultsDir, self, incoming, buildJsonObject {
            incoming.data.forEach { (k, v) -> put(k, v) }
            put("status", "error")
            put("pipeline_error", "max_hops (${config.maxHops}) exceeded — check for circular prerequisites in pipeline-config.json")
        })
        return
    }

    val prereqs = config.prerequisitesFor(self)
    val missing = prereqs.filter { it !in incoming.processedBy }

    if (missing.isNotEmpty()) {
        // Rewrite routing: insert missing prereqs before self, deduplicate the rest
        val cleanedQueue = incoming.pipelineQueue.filter { it !in missing && it != self }
        val newQueue = missing + listOf(self) + cleanedQueue
        val next = newQueue.first()
        routingLog.info("[$self] prereqs missing $missing — rewriting queue to $newQueue, forwarding to $next")
        postToAgent(
            config.endpointFor(next).url,
            incoming.copy(
                messageId    = UUID.randomUUID().toString(),
                timestamp    = Instant.now().toString(),
                sourceAgent  = self,
                targetAgent  = next,
                pipelineQueue = newQueue.drop(1),
                hops         = newHops,
            ),
        )
        return
    }

    val (outData, terminal) = process(incoming)
    forward(self, incoming, outData, terminal, config, resultsDir)
}

private fun forward(
    self: String,
    incoming: AgentMessage,
    outData: JsonObject,
    terminal: Boolean,
    config: PipelineConfig,
    resultsDir: Path,
) {
    val processedBy = incoming.processedBy + self
    val newHops     = incoming.hops + 1

    if (terminal || incoming.pipelineQueue.isEmpty()) {
        writeResult(resultsDir, self, incoming, outData)
        return
    }

    val next      = incoming.pipelineQueue.first()
    val remainder = incoming.pipelineQueue.drop(1)

    postToAgent(
        config.endpointFor(next).url,
        AgentMessage(
            messageId     = UUID.randomUUID().toString(),
            timestamp     = Instant.now().toString(),
            sourceAgent   = self,
            targetAgent   = next,
            messageType   = incoming.messageType,
            data          = outData,
            pipelineQueue = remainder,
            processedBy   = processedBy,
            hops          = newHops,
        ),
    )
}

fun writeResult(resultsDir: Path, self: String, incoming: AgentMessage, data: JsonObject) {
    val txnId = data["transaction_id"]?.jsonPrimitive?.content
        ?: incoming.data["transaction_id"]?.jsonPrimitive?.content
        ?: "UNKNOWN"
    val msg = AgentMessage(
        messageId   = UUID.randomUUID().toString(),
        timestamp   = Instant.now().toString(),
        sourceAgent = self,
        targetAgent = "results",
        data        = data,
    )
    resultsDir.resolve("$txnId.json").writeText(routingJson.encodeToString(msg))
}
