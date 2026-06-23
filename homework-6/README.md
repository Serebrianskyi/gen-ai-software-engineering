# AI-Powered Multi-Agent Banking Pipeline — Kotlin

**Author: Roman Serebrianskyi**

---

## Overview

This project is a Kotlin re-implementation of the file-based multi-agent banking pipeline from Homework 6. Raw transactions from `sample-transactions.json` are validated, scored for fraud risk, and settled — with every inter-agent handoff recorded as a structured JSON message in `shared/` subdirectories. The entire pipeline runs end-to-end with a single Gradle command and produces auditable results for every transaction.

Each AI agent (Validator, Fraud Detector) calls the local `claude` CLI via `ProcessBuilder`, keeping inference inside the existing Claude Code session with no API key or external credits required. The Settlement Processor is pure Kotlin logic.

---

## Pipeline Architecture

```
sample-transactions.json
         │
   Main.kt  (orchestrator)
         │
         ▼
┌─────────────────────┐
│ Transaction         │  shared/input/
│ Validator           │──────────────► shared/results/  (status: rejected)
└─────────────────────┘
         │ valid → shared/output/
         ▼
┌─────────────────────┐
│ Fraud Detector      │──────────────► shared/results/  (status: flagged)
└─────────────────────┘
         │ cleared → shared/output/
         ▼
┌─────────────────────┐
│ Settlement          │──────────────► shared/results/  (status: settled)
│ Processor           │──────────────► pipeline_summary.json
└─────────────────────┘
```

**Shared directory layout:**
```
shared/
├── input/       ← integrator drops initial messages here
├── processing/  ← agent moves message here while working
├── output/      ← intermediate results between agents
└── results/     ← final outcomes (rejected / flagged / settled)
```

---

## Agent Responsibilities

| Agent | File | Role |
|---|---|---|
| Transaction Validator | `agents/TransactionValidator.kt` | Calls Claude to check required fields, positive `BigDecimal` amount, ISO 4217 currency; masks PII |
| Fraud Detector | `agents/FraudDetector.kt` | Calls Claude to score risk 0.0–1.0 (high-value, off-hours, cross-border, structuring); flags ≥ 0.4 |
| Settlement Processor | `agents/SettlementProcessor.kt` | Pure Kotlin — settles cleared transactions; writes `pipeline_summary.json` |
| MCP Server | `mcp/PipelineServer.kt` | Exposes pipeline results as queryable MCP tools and a summary resource |

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.0 + JVM 21 |
| Build | Gradle 8.10 (Kotlin DSL) |
| JSON | kotlinx.serialization |
| Monetary precision | `java.math.BigDecimal` (never `Double` for amounts) |
| Claude calls | `claude --print` via `ProcessBuilder` (no API key) |
| MCP server | `io.modelcontextprotocol:kotlin-sdk` |
| Testing | JUnit 5 |
| Coverage gate | JaCoCo `minimumRatio = 0.80` (pre-push hook blocks if < 80%) |

---

## Skills

- `/run-pipeline` — runs the pipeline end-to-end and shows a summary
- `/validate-transactions` — dry-run validation without processing

---

## Sample Results (8 transactions)

| Transaction | Status | Notes |
|---|---|---|
| TXN001 | settled | Clean, low-value domestic |
| TXN002 | flagged | High-value $25,000 (risk 0.4) |
| TXN003 | settled | Structuring amount $9,999.99 (risk 0.3, below threshold) |
| TXN004 | flagged | Off-hours + cross-border DE (risk 0.5) |
| TXN005 | flagged | High-value $75,000 (risk 0.4) |
| TXN006 | rejected | Invalid currency code XYZ |
| TXN007 | rejected | Negative amount -$100.00 |
| TXN008 | settled | Clean, domestic salary advance |