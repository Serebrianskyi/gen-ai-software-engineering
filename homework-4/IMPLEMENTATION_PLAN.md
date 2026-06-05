# Homework 4 — Implementation Plan: 4-Agent Pipeline

## Overview

Build a 4-agent Claude Code pipeline that operates on a seeded Node.js expense tracker app:
finds and verifies bugs, applies fixes, audits security, and generates unit tests — all triggered by one command.

---

## Stack

| Concern | Choice | Reason |
|---------|--------|--------|
| App language | Node.js (plain CommonJS) | `npm run pipeline` is the natural entry point; no extra runtime needed |
| Test framework | Jest | Zero config, fast, widely understood |
| Pipeline trigger | `run-pipeline.sh` invoked via `npm run pipeline` | Shell script chains `claude` CLI calls sequentially |
| Agent format | Claude Code `.agent.md` frontmatter | Native to the toolchain used in this course |

---

## Seeded App: Expense Tracker (`src/`)

### Files

| File | Purpose |
|------|---------|
| `src/app.js` | `ExpenseTracker` class — contains all seeded bugs/security issues |
| `src/utils.js` | Pure formatter helpers — clean, no bugs |
| `tests/app.test.js` | Initial test suite — some tests intentionally fail before pipeline |
| `package.json` | Scripts: `start`, `test`, `pipeline` |

### Seeded Issues

| ID | Type | Function | Bug |
|----|------|----------|-----|
| 001 | Bug | `getTopExpenses(n)` | `slice(1, n+1)` — skips the highest expense; should be `slice(0, n)` |
| 002 | Bug | `calculateTotal(withTax, taxRate)` | `subtotal + taxRate` — adds rate instead of multiplying; should be `subtotal * (1 + taxRate)` |
| 003 | Security | `filterExpenses(filterExpr)` | `eval(filterExpr)` on user-controlled input — code injection |
| 003b | Security | module level | `const ADMIN_KEY = "secret123"` — hardcoded credential |

---

## Deliverables & Build Order

### Phase 1 — Mini Application

**1.1** `src/app.js` — `ExpenseTracker` class with all 4 seeded issues  
**1.2** `src/utils.js` — `formatCurrency(amount)`, `formatDate(date)` helpers  
**1.3** `tests/app.test.js` — Jest tests covering `addExpense`, `getTopExpenses`, `calculateTotal`, `filterExpenses`; two tests intentionally fail pre-fix  
**1.4** `package.json` — scripts: `test` → Jest, `start` → `node src/app.js`, `pipeline` → `./run-pipeline.sh`

### Phase 2 — Bug Context Files (pipeline seeds)

**2.1** `context/bugs/001/bug-context.md` — describes off-by-one in `getTopExpenses`  
**2.2** `context/bugs/002/bug-context.md` — describes wrong tax calculation  
**2.3** `context/bugs/003/bug-context.md` — describes eval injection + hardcoded secret

### Phase 3 — Pre-seeded Research & Plan (pipeline inputs)

**3.1** `research/codebase-research.md` — simulates Bug Researcher output; contains file:line refs, code snippets, and descriptions of all 4 issues  
**3.2** `implementation-plan.md` — simulates Bug Planner output; specifies exact before/after code changes per file, test command to run after each fix

### Phase 4 — Skills

**4.1** `skills/research-quality-measurement.md` — defines 4 quality levels (VERIFIED / PARTIAL / UNVERIFIED / INVALID) with criteria; used by research-verifier agent  
**4.2** `skills/unit-tests-FIRST.md` — defines FIRST (Fast, Independent, Repeatable, Self-validating, Timely) with per-principle checklist; used by unit-test-generator agent

### Phase 5 — Agent Files

**5.1** `agents/research-verifier.agent.md`
- Model: `claude-opus-4-8` — fact-checking and reasoning about research quality needs the strongest model
- Reads: `research/codebase-research.md`, source files
- Skill: `skills/research-quality-measurement.md`
- Writes: `research/verified-research.md`

**5.2** `agents/bug-fixer.agent.md`
- Model: `claude-sonnet-4-6` — applying a pre-written plan is routine work; speed + capability balanced
- Reads: `implementation-plan.md`, source files
- Writes: applies code changes to `src/app.js`, then writes `fix-summary.md`

**5.3** `agents/security-verifier.agent.md`
- Model: `claude-opus-4-8` — security reasoning (injection vectors, OWASP, secrets) benefits from deep analysis
- Reads: `fix-summary.md`, changed source files
- Writes: `security-report.md` (report only — no code edits)

**5.4** `agents/unit-test-generator.agent.md`
- Model: `claude-sonnet-4-6` — test scaffolding for known changes is mechanical
- Reads: `fix-summary.md`, changed source files
- Skill: `skills/unit-tests-FIRST.md`
- Writes: test files under `tests/`, then `test-report.md`

### Phase 6 — Pipeline Runner

**6.1** `run-pipeline.sh` — sequential execution:
```
Step 1: research-verifier  →  verified-research.md
Step 2: bug-fixer          →  src/app.js (patched) + fix-summary.md
Step 3: security-verifier  →  security-report.md
Step 4: unit-test-generator →  tests/ (new files) + test-report.md
```
Each step checks exit code; pipeline halts on failure with a clear error message.

### Phase 7 — Documentation

**7.1** `README.md` — overview, pipeline diagram, model choices + justification, author info  
**7.2** `HOWTORUN.md` — prerequisites, install steps, run commands, expected output per phase

---

## Agent Output Files (created at pipeline runtime, not pre-committed)

| File | Created by |
|------|-----------|
| `research/verified-research.md` | research-verifier |
| `fix-summary.md` | bug-fixer |
| `security-report.md` | security-verifier |
| `test-report.md` | unit-test-generator |

---

## Requirements Traceability

| TASKS.md requirement | Covered by |
|---------------------|-----------|
| 4 agents in `agents/` | Phases 5.1–5.4 |
| Explicit model per agent + justification | Phase 5 + README §Model Choices |
| Single-command execution | Phase 6 (`npm run pipeline` → `run-pipeline.sh`) |
| Skills auto-loaded by relevant agents | `skills/` refs in agent frontmatter |
| `research-quality-measurement.md` skill | Phase 4.1 |
| `unit-tests-FIRST.md` skill | Phase 4.2 |
| Sample mini app with ≥2 bugs + ≥1 security issue | Phase 1 (4 issues seeded) |
| App runnable locally | `npm start` / `npm test` in `package.json` |
| Agent outputs committed | All 4 output files generated by pipeline run |
| Screenshots in `docs/screenshots/` | Captured after pipeline run |
| README with author info | Phase 7.1 |