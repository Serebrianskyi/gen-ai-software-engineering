# AI-Powered Multi-Agent Banking Pipeline — Kotlin Specification

**Author: Roman Serebrianskyi**

---

## 1. High-Level Objective

Re-implement the file-based multi-agent banking pipeline in Kotlin, preserving identical behaviour, Claude system prompts, JSON message schema, and test assertions from the Python original — with Kotlin idioms, Gradle build tooling, JUnit 5, and JaCoCo coverage gates.

---

## 2. Mid-Level Objectives

- Transactions with missing required fields, non-positive amounts, or invalid ISO 4217 currency codes are rejected with a machine-readable `rejection_reason` field before entering the fraud stage
- Transactions above $10,000, submitted between 22:00–06:00 UTC, or originating cross-border are assigned a numeric risk score; those scoring ≥ 0.4 are flagged and isolated from settlement
- All inter-agent communication uses the standard JSON message schema written to `shared/` subdirectories; no agent reads directly from another agent's internal state
- Every agent operation emits an audit log line containing ISO 8601 timestamp, agent name, transaction ID, and outcome — account numbers are masked to `ACC-****` and never logged in plain text
- The full pipeline runs end-to-end via `./gradlew run`; all 8 sample transactions reach `shared/results/` with a final status of `settled`, `flagged`, or `rejected`
- JaCoCo coverage gate blocks `git push` if line coverage drops below 80%

---

## 3. Implementation Notes

- **Monetary values**: `java.math.BigDecimal` for all amount comparisons — never `Double`
- **Currency codes**: validate against a fixed ISO 4217 set; reject unknown codes (e.g. `"XYZ"`)
- **Logging**: every log line must include `timestamp` (ISO 8601), `agent`, `transaction_id`, `outcome`; mask accounts as `ACC-****`
- **Claude calls**: use `ProcessBuilder(listOf("claude", "--print", "--model", model, "--system-prompt", system, "--no-session-persistence", "--output-format", "text"))` from `/tmp` — identical approach to Python's `subprocess.run`
- **Message schema**: every JSON file passed between agents must include: `message_id`, `timestamp`, `source_agent`, `target_agent`, `message_type`, `data`
- **Build**: Gradle 8 with Kotlin DSL; `kotlinx.serialization` for JSON; JUnit 5 + JaCoCo; `io.modelcontextprotocol:kotlin-sdk` for MCP
- **No external databases**: all state lives in `shared/` JSON files

---

## 4. Context

### Beginning state
- `sample-transactions.json` — 8 raw transaction records (same as Python version)
- `shared/input/`, `shared/processing/`, `shared/output/`, `shared/results/` — empty directories
- No agent code exists

### Ending state
- `Main.kt` — orchestrator that sets up dirs, loads transactions, runs agents in sequence
- `agents/TransactionValidator.kt`, `agents/FraudDetector.kt`, `agents/SettlementProcessor.kt`
- `shared/results/` — 8 result files (2 rejected, 3 flagged, 3 settled)
- `pipeline_summary.json` — counts of settled/flagged/rejected
- `src/test/kotlin/` — unit + integration tests with ≥ 80% JaCoCo coverage
- `mcp/PipelineServer.kt` — Kotlin MCP SDK server exposing pipeline query tools
- `README.md`, `HOWTORUN.md`

---

## 5. Low-Level Tasks

```
Task: Transaction Validator (Agent 1)
Prompt: "Implement a Kotlin transaction validator agent that uses the Claude CLI subprocess to validate each transaction for required fields, positive BigDecimal amount, and ISO 4217 currency code. Return processTransaction(message: JsonObject): AgentMessage. Route valid transactions to fraud_detector, rejected ones to results with rejection_reason."
File to CREATE: agents/TransactionValidator.kt
Function to CREATE: processTransaction(message: JsonObject): AgentMessage, dryRun(samplePath: Path)
Details: Required fields: transaction_id, timestamp, source_account, destination_account, amount, currency, transaction_type. Reject if amount <= 0. Reject if currency not in ISO 4217 set. Mask accounts to ACC-****. Same Claude system prompt as Python.
```

```
Task: Fraud Detector (Agent 2)
Prompt: "Implement a Kotlin fraud detector agent using the Claude CLI subprocess. Scoring rules: amount > $10k +0.4 (high_value); $9k–$9999.99 +0.3 (structuring); UTC hour 22–23 or 0–5 +0.3 (off_hours); metadata.country != US +0.2 (cross_border); cap at 1.0. Flag if score >= 0.4."
File to CREATE: agents/FraudDetector.kt
Function to CREATE: scoreTransaction(data: JsonObject): Pair<Double, List<String>>, processFraudTransaction(message: JsonObject): AgentMessage
Details: Same decision rules and Claude system prompt as Python. Use BigDecimal for amount comparisons.
```

```
Task: Settlement Processor (Agent 3)
Prompt: "Implement a pure Kotlin settlement processor that reads fraud-cleared transactions, marks them settled with a settled_at timestamp, and generates pipeline_summary.json."
File to CREATE: agents/SettlementProcessor.kt
Function to CREATE: settleTransaction(message: JsonObject): AgentMessage, generateSummary(resultsDir: Path): PipelineSummary
Details: No Claude calls — pure logic. Write to shared/results/ with status: "settled". Generate pipeline_summary.json with keys: total, settled, flagged, rejected, processed_at.
```

```
Task: MCP Server
Prompt: "Implement a Kotlin MCP server using io.modelcontextprotocol:kotlin-sdk exposing get_transaction_status tool, list_pipeline_results tool, and pipeline://summary resource."
File to CREATE: mcp/PipelineServer.kt
Details: Use StdioServerTransport with kotlinx.io Source/Sink adapters. Mirror FastMCP Python implementation.
```