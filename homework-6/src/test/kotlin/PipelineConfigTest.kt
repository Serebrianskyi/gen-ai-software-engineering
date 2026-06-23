import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files

class PipelineConfigTest {

    private fun config(
        pipeline: List<String> = listOf("andromeda", "sirius", "polaris"),
        prerequisites: Map<String, List<String>> = mapOf(
            "andromeda" to emptyList(),
            "sirius"    to listOf("andromeda"),
            "polaris"   to listOf("andromeda", "sirius"),
        ),
        maxHops: Int = 10,
    ) = PipelineConfig(
        agents = mapOf(
            "andromeda" to AgentEndpoint("localhost", 8081),
            "sirius"    to AgentEndpoint("localhost", 8082),
            "polaris"   to AgentEndpoint("localhost", 8083),
            "vega"      to AgentEndpoint("localhost", 8084),
        ),
        pipeline      = pipeline,
        prerequisites = prerequisites,
        maxHops       = maxHops,
    )

    // ── endpointFor ───────────────────────────────────────────────────────────

    @Test fun `endpointFor returns correct port`() {
        assertEquals(8081, config().endpointFor("andromeda").port)
        assertEquals(8082, config().endpointFor("sirius").port)
    }

    @Test fun `endpointFor builds correct url`() {
        val url = config().endpointFor("andromeda").url
        assertEquals("http://localhost:8081/process", url)
    }

    @Test fun `endpointFor throws for unknown agent`() {
        assertThrows<IllegalStateException> { config().endpointFor("unknown") }
    }

    // ── prerequisitesFor ──────────────────────────────────────────────────────

    @Test fun `prerequisitesFor returns declared prereqs`() {
        assertEquals(listOf("andromeda"), config().prerequisitesFor("sirius"))
        assertEquals(listOf("andromeda", "sirius"), config().prerequisitesFor("polaris"))
    }

    @Test fun `prerequisitesFor returns empty list for andromeda`() {
        assertEquals(emptyList<String>(), config().prerequisitesFor("andromeda"))
    }

    @Test fun `prerequisitesFor returns empty list for undeclared agent`() {
        assertEquals(emptyList<String>(), config().prerequisitesFor("unknown"))
    }

    // ── maxHops ───────────────────────────────────────────────────────────────

    @Test fun `default maxHops is 20`() {
        val cfg = PipelineConfig(
            agents = mapOf("a" to AgentEndpoint("localhost", 9000)),
            pipeline = listOf("a"),
        )
        assertEquals(20, cfg.maxHops)
    }

    @Test fun `custom maxHops is respected`() {
        assertEquals(5, config(maxHops = 5).maxHops)
    }

    // ── load from file ────────────────────────────────────────────────────────

    @Test fun `load from valid pipeline-config json`() {
        val tmp  = Files.createTempDirectory("pipelinecfg-test")
        val file = tmp.resolve("pipeline-config.json")
        file.toFile().writeText("""
            {
              "agents": {
                "andromeda": {"host": "localhost", "port": 8081}
              },
              "pipeline": ["andromeda"],
              "prerequisites": {"andromeda": []},
              "max_hops": 15
            }
        """.trimIndent())
        val cfg = PipelineConfig.load(file)
        assertEquals(listOf("andromeda"), cfg.pipeline)
        assertEquals(15, cfg.maxHops)
        assertEquals(8081, cfg.endpointFor("andromeda").port)
    }

    @Test fun `load ignores unknown json fields`() {
        val tmp  = Files.createTempDirectory("pipelinecfg-test2")
        val file = tmp.resolve("pipeline-config.json")
        file.toFile().writeText("""
            {
              "agents": {"a": {"host": "h", "port": 1}},
              "pipeline": ["a"],
              "unknown_field": "ignored"
            }
        """.trimIndent())
        assertDoesNotThrow { PipelineConfig.load(file) }
    }
}
