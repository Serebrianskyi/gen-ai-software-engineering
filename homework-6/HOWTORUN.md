# How to Run (Kotlin)

## Prerequisites

- JDK 21+
- Gradle (wrapper included — no install needed)
- `claude` CLI on PATH (already running if you're in Claude Code)
- Node.js (for context7 MCP server via `npx`)

---

## 1. Install / build dependencies

```bash
cd homework-6-kotlin
./gradlew build -x test
```

## 2. Clean generated data (recommended before each run)

```bash
./clean.sh
```

Removes all JSON files from `shared/input/`, `shared/processing/`, `shared/output/`, and `shared/results/`.
The pipeline clears most of these automatically at startup, but running `clean.sh` first guarantees a
completely fresh state — especially useful after interrupted or partial runs.

## 3. Run the full pipeline

```bash
./gradlew run
```

Loads `sample-transactions.json`, runs all three agents in sequence, and writes results to `shared/results/`. Expected output:

```
  PIPELINE SUMMARY
════════════════════════════════════════
  Total:    8
  Settled:  3
  Flagged:  3
  Rejected: 2
```

## 4. Validate transactions without processing (dry-run)

```bash
./gradlew run --args="--dry-run"
```

Reports valid/invalid counts and rejection reasons without writing any files.

## 5. Run the test suite

```bash
./gradlew test
```

## 6. Check test coverage

```bash
./gradlew test jacocoTestCoverageVerification
```

The coverage gate requires ≥ 80% line coverage. The HTML report is at `build/reports/jacoco/test/html/index.html`.

The coverage gate also runs automatically before `git push` via the pre-push hook in `.claude/settings.json`.

## 7. Start the MCP server

```bash
./gradlew shadowJar    # build fat jar first
java -cp build/libs/homework-6-kotlin-1.0.0-all.jar mcp.PipelineServerKt
```

Available tools (via MCP client):
- `get_transaction_status("TXN001")` — status of a single transaction
- `list_pipeline_results()` — summary of all results
- Resource `pipeline://summary` — human-readable run summary

## 8. Use Claude Code skills

Inside Claude Code (from the `homework-6/` directory):

```
/run-pipeline           # runs the pipeline end-to-end
/validate-transactions  # dry-run validation table
```