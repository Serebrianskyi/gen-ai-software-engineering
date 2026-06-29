import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

@Serializable
data class AgentEndpoint(val host: String, val port: Int) {
    val url: String get() = "http://$host:$port/process"
}

@Serializable
data class PipelineConfig(
    val agents: Map<String, AgentEndpoint>,
    val pipeline: List<String>,
    val prerequisites: Map<String, List<String>> = emptyMap(),
    @SerialName("max_hops") val maxHops: Int = 20,
) {
    fun endpointFor(name: String): AgentEndpoint =
        agents[name] ?: error("Unknown agent '$name'. Known agents: ${agents.keys.sorted()}")

    fun prerequisitesFor(name: String): List<String> =
        prerequisites[name] ?: emptyList()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun load(path: Path): PipelineConfig = json.decodeFromString(path.readText())
    }
}
