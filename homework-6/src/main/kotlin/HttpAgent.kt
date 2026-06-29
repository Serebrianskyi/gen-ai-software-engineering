import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors

private val httpCodec = Json { ignoreUnknownKeys = true; prettyPrint = false }
private val httpClient: HttpClient = HttpClient.newHttpClient()

fun startAgentServer(port: Int, agentName: String, handler: (AgentMessage) -> Unit): HttpServer {
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/process") { exchange ->
        if (exchange.requestMethod != "POST") {
            exchange.sendResponseHeaders(405, -1)
            exchange.close()
            return@createContext
        }
        try {
            val body = exchange.requestBody.readBytes().decodeToString()
            exchange.sendResponseHeaders(202, -1)
            exchange.close()
            val message = httpCodec.decodeFromString<AgentMessage>(body)
            handler(message)
        } catch (e: Exception) {
            System.err.println("[$agentName:$port] handler error: ${e.message}")
            try { exchange.sendResponseHeaders(400, -1); exchange.close() } catch (_: Exception) {}
        }
    }
    server.executor = Executors.newVirtualThreadPerTaskExecutor()
    server.start()
    return server
}

fun postToAgent(url: String, message: AgentMessage) {
    val body = httpCodec.encodeToString(message)
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    httpClient.send(request, HttpResponse.BodyHandlers.discarding())
}
