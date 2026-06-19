# How to Run — Homework 4

## Prerequisites

| Requirement | Version | Check |
|-------------|---------|-------|
| Node.js | ≥ 18 | `node --version` |
| npm | ≥ 9 | `npm --version` |
| Claude Code CLI | latest | `claude --version` |

The pipeline calls `claude --print` via the local Claude Code CLI. It uses your existing Claude Code session authentication — **no API key required**.

## Setup

```bash
cd homework-4
npm install
```

## Run the app (before pipeline — bugs present)

```bash
npm start
# or: node src/app.js
```

## Run tests (before pipeline — 2 tests fail)

```bash
npm test
```

Expected output:
```
FAIL tests/app.test.js
  ✓ addExpense › adds an expense to the list
  ✓ addExpense › throws when name is missing
  ✗ getTopExpenses › returns the highest expense first   ← BUG-001
  ✗ calculateTotal › returns correct total with 10% tax  ← BUG-002
  ...
Tests: 2 failed, X passed
```

## Run the full pipeline (single command)

```bash
npm run pipeline
# or: ./run-pipeline.sh
```

### 🎭 Drama mode (persona banter)

By default the pipeline narrates itself as a conversation between four
coworkers who react in-character to the **real results** of each phase:

| Persona | Voice |
|---------|-------|
| 🧐 Research Verifier | hyperactive PM / Donkey — *"Is it done? Is it REALLY done?"* |
| 🛠️ Bug Fixer | exhausted, furious backend dev (censored swearing) |
| 🪨 Security Verifier | nightclub bouncer who talks like a caveman |
| 🧃 Unit Test Generator | a teenager fluent in slang |

The banter is generated with a fast model (`claude-haiku-4-5`) and printed to
the terminal **only** — it never touches the graded deliverable files. Turn it
off for a quiet/grading run:

```bash
PIPELINE_DRAMA=0 npm run pipeline
```

The pipeline runs four phases and prints progress:

```
🤖  4-AGENT PIPELINE — Expense Tracker

════════════════════════════════════════════════════════════
  PHASE: Bug Research Verification
════════════════════════════════════════════════════════════
[pipeline] Model: claude-opus-4-8
[pipeline] Loaded skill: skills/research-quality-measurement.md
[pipeline] Written → research/verified-research.md

════════════════════════════════════════════════════════════
  PHASE: Bug Fixing
════════════════════════════════════════════════════════════
[pipeline] Model: claude-sonnet-4-6
[pipeline] Code fixes applied to src/app.js
[pipeline] Written → fix-summary.md

════════════════════════════════════════════════════════════
  PHASE: Security Verification
════════════════════════════════════════════════════════════
[pipeline] Model: claude-opus-4-8
[pipeline] Written → security-report.md

════════════════════════════════════════════════════════════
  PHASE: Unit Test Generation
════════════════════════════════════════════════════════════
[pipeline] Model: claude-sonnet-4-6
[pipeline] Loaded skill: skills/unit-tests-FIRST.md
[pipeline] Written → tests/app.fixed.test.js
[pipeline] Written → test-report.md

✅  Pipeline complete.
```

## Re-running the pipeline (reset to the "before" state)

The pipeline edits `src/app.js` in place and writes several output files. To run
it again from a clean slate, reset first:

```bash
npm run reset
```

This restores `src/app.js` from the pristine buggy baseline
(`fixtures/app.buggy.js`) and removes the generated outputs:

| Restored / removed | |
|--------------------|--|
| `src/app.js` | restored to buggy baseline |
| `research/verified-research.md` | removed |
| `fix-summary.md` | removed |
| `security-report.md` | removed |
| `test-report.md` | removed |
| `tests/app.fixed.test.js` | removed |

Seeded inputs (`research/codebase-research.md`, `implementation-plan.md`,
`src/utils.js`, `tests/app.test.js`, `agents/`, `skills/`, `context/`) are left
untouched. Typical loop:

```bash
npm run reset      # back to before
npm test           # 2 failures (BUG-001, BUG-002)
npm run pipeline   # regenerate fixes + outputs
npm test           # all pass
```

> The generated files are required homework deliverables, so they live in their
> documented locations and are committed — `npm run reset` regenerates them
> rather than them being treated as throwaway build artifacts.

## Verify results (after pipeline — all tests pass)

```bash
npm test
```

Expected output:
```
PASS tests/app.test.js
PASS tests/app.fixed.test.js
Tests: 0 failed, all passed
```

## View pipeline outputs

| File | Description |
|------|-------------|
| `research/verified-research.md` | Research quality rating + verification details |
| `fix-summary.md` | Before/after for each applied fix |
| `security-report.md` | Security findings with severity ratings |
| `test-report.md` | Test coverage + FIRST compliance summary |
| `tests/app.fixed.test.js` | Generated tests for fixed functions |

## Troubleshooting

**`Error: claude: command not found`**
→ Claude Code CLI is not installed or not in PATH. Install it: `npm install -g @anthropic-ai/claude-code`

**`Required file not found: research/codebase-research.md`**
→ Ensure you are running from the `homework-4/` directory.

**Pipeline halts at Bug Fixer with structured output warning**
→ The fallback patch is applied automatically; check `src/app.js` and `fix-summary.md` for results.

**`npm test` still shows failures after pipeline**
→ Check `fix-summary.md` for any "FAILED" or "NOT RUN" entries; re-run the pipeline or apply the fix manually per `implementation-plan.md`.