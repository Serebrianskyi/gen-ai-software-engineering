package mcp

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * MCP server exposing the pipeline results as queryable tools and a summary resource.
 *
 * Tools:
 *   get_transaction_status(transaction_id) — status of a single transaction
 *   list_pipeline_results()               — summary of all processed transactions
 *
 * Resource:
 *   pipeline://summary — latest pipeline run as human-readable text
 */
private val JSON = Json { ignoreUnknownKeys = true }

private val RESULTS_DIR: Path = Path.of(
    object {}.javaClass.protectionDomain.codeSource.location.toURI()
).parent.parent.parent.parent.resolve("shared/results")

private fun readResult(path: Path): JsonObject =
    JSON.parseToJsonElement(path.readText()).jsonObject["data"]?.jsonObject ?: JsonObject(emptyMap())

fun main() = runBlocking {
    val server = Server(
        serverInfo = Implementation(name = "pipeline-status", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
                resources = ServerCapabilities.Resources(subscribe = false, listChanged = false),
            )
        ),
    )

    server.addTool(
        name = "get_transaction_status",
        description = "Return the current pipeline status for a transaction by ID",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("transaction_id", buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction ID (e.g. TXN001)")
                })
            },
            required = listOf("transaction_id"),
        ),
    ) { request ->
        val txnId = request.arguments["transaction_id"]?.jsonPrimitive?.content
            ?: return@addTool CallToolResult(content = listOf(TextContent("Missing transaction_id")), isError = true)
        val file = RESULTS_DIR.resolve("$txnId.json")
        if (!file.exists()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Transaction $txnId not found in results")),
                isError = true,
            )
        }
        val data = readResult(file)
        val result = buildJsonObject {
            put("transaction_id", txnId)
            put("status", data["status"] ?: JsonNull)
            put("risk_score", data["risk_score"] ?: JsonNull)
            put("risk_rules", data["risk_rules"] ?: buildJsonArray {})
            put("rejection_reason", data["rejection_reason"] ?: JsonNull)
            put("settled_at", data["settled_at"] ?: JsonNull)
        }
        CallToolResult(content = listOf(TextContent(result.toString())))
    }

    server.addTool(
        name = "list_pipeline_results",
        description = "Return a summary of all processed transactions and their statuses",
        inputSchema = Tool.Input(),
    ) { _ ->
        if (!RESULTS_DIR.exists()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("No results found — run the pipeline first")),
                isError = true,
            )
        }
        val transactions = RESULTS_DIR.listDirectoryEntries("*.json")
            .filter { it.name != "pipeline_summary.json" }
            .sortedBy { it.name }
            .map { f ->
                val data = readResult(f)
                buildJsonObject {
                    put("transaction_id", data["transaction_id"] ?: JsonPrimitive(f.nameWithoutExtension))
                    put("status", data["status"] ?: JsonNull)
                    put("risk_score", data["risk_score"] ?: JsonNull)
                    put("rejection_reason", data["rejection_reason"] ?: JsonNull)
                }
            }

        val summaryFile = RESULTS_DIR.resolve("pipeline_summary.json")
        val summary = if (summaryFile.exists()) JSON.parseToJsonElement(summaryFile.readText()) else JsonNull

        val result = buildJsonObject {
            put("transactions", buildJsonArray { transactions.forEach { add(it) } })
            put("summary", summary)
        }
        CallToolResult(content = listOf(TextContent(result.toString())))
    }

    server.addResource(
        uri = "pipeline://summary",
        name = "Pipeline Summary",
        description = "Latest pipeline run summary as human-readable text",
        mimeType = "text/plain",
    ) { _ ->
        val summaryFile = RESULTS_DIR.resolve("pipeline_summary.json")
        if (!summaryFile.exists()) {
            return@addResource ReadResourceResult(
                contents = listOf(TextResourceContents("No pipeline run found. Execute the pipeline first.", "pipeline://summary", "text/plain"))
            )
        }
        val s = JSON.parseToJsonElement(summaryFile.readText()).jsonObject
        val lines = mutableListOf(
            "=== Pipeline Run Summary ===",
            "Total:     ${s["total"]?.jsonPrimitive?.int ?: 0}",
            "Settled:   ${s["settled"]?.jsonPrimitive?.int ?: 0}",
            "Flagged:   ${s["flagged"]?.jsonPrimitive?.int ?: 0}",
            "Rejected:  ${s["rejected"]?.jsonPrimitive?.int ?: 0}",
            "Completed: ${s["processed_at"]?.jsonPrimitive?.content ?: "unknown"}",
        )
        RESULTS_DIR.listDirectoryEntries("*.json")
            .filter { it.name != "pipeline_summary.json" }
            .forEach { f ->
                val data = readResult(f)
                if (data["status"]?.jsonPrimitive?.content == "rejected") {
                    if (lines.none { it.startsWith("\nRejected") }) lines += "\nRejected transactions:"
                    lines += "  ${data["transaction_id"]?.jsonPrimitive?.content}: ${data["rejection_reason"]?.jsonPrimitive?.content}"
                }
            }
        ReadResourceResult(
            contents = listOf(TextResourceContents(lines.joinToString("\n"), "pipeline://summary", "text/plain"))
        )
    }

    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered(),
    )
    server.connect(transport)

    // keep the server alive until the transport closes
    val done = java.util.concurrent.CountDownLatch(1)
    server.onClose { done.countDown() }
    done.await()
}