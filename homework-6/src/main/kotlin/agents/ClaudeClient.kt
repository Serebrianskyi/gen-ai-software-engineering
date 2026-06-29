package agents

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Claude CLI wrapper shared by all pipeline agents.
 *
 * Runs `claude` from /tmp so project .claude/settings.json hooks are not loaded.
 * System prompt via --system-prompt flag; user message via stdin.
 * No API key needed — uses Claude Code's existing daemon auth.
 */
object ClaudeClient {

    private val json = Json { ignoreUnknownKeys = true }

    fun ask(
        system: String,
        userContent: String,
        model: String = "claude-haiku-4-5-20251001",
    ): String {
        val process = ProcessBuilder(
            "claude",
            "--print",
            "--model", model,
            "--system-prompt", system,
            "--no-session-persistence",
            "--output-format", "text",
        )
            .directory(java.io.File("/tmp"))
            .redirectErrorStream(false)
            .start()

        process.outputStream.bufferedWriter().use { it.write(userContent) }
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        return output
    }

    fun askJson(
        system: String,
        userContent: String,
        model: String = "claude-haiku-4-5-20251001",
    ): JsonObject {
        var text = ask(system, userContent, model).trim()
        if (text.startsWith("```")) {
            val lines = text.lines()
            text = lines.drop(1).joinToString("\n").substringBeforeLast("```").trim()
        }
        return json.parseToJsonElement(text) as JsonObject
    }
}