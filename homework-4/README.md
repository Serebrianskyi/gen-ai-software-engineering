# Homework 4 — 4-Agent Pipeline

**Student**: Roman Serebrianskyi

A 4-agent Claude Code pipeline that automatically verifies bug research, applies fixes, audits security, and generates unit tests — all from a single command.

---

## Pipeline Overview

```
npm run pipeline
      │
      ▼
┌─────────────────────┐
│ Bug Research        │  reads  research/codebase-research.md
│ Verifier            │  writes research/verified-research.md
│ model: opus-4-8     │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Bug Fixer           │  reads  implementation-plan.md
│ model: sonnet-4-6   │  writes src/app.js (fixed), fix-summary.md
└────────┬────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌──────────┐  ┌────────────────────┐
│ Security │  │ Unit Test          │
│ Verifier │  │ Generator          │
│ opus-4-8 │  │ sonnet-4-6         │
│          │  │ skill: FIRST       │
│ writes:  │  │ writes:            │
│ security │  │ tests/app.fixed    │
│ -report  │  │ .test.js           │
│ .md      │  │ test-report.md     │
└──────────┘  └────────────────────┘
```

---

## Sample Application

A minimal Node.js **Expense Tracker** (`src/app.js`) with four intentionally seeded issues:

| ID | Type | Function | Description |
|----|------|----------|-------------|
| BUG-001 | Logic | `getTopExpenses(n)` | `slice(1, n+1)` skips the highest expense |
| BUG-002 | Logic | `calculateTotal(withTax, taxRate)` | `subtotal + taxRate` instead of `subtotal * (1 + taxRate)` |
| SEC-003a | Security | `filterExpenses(filterExpr)` | `eval()` on user input — code injection |
| SEC-003b | Security | module level | `ADMIN_KEY = 'secret123'` — hardcoded credential |

Run the app before the pipeline to see two tests fail. Run it after to see all tests pass.

---

## Agent Model Choices

| Agent | Model | Justification |
|-------|-------|--------------|
| Bug Research Verifier | `claude-opus-4-8` | Fact-checking requires verifying exact file:line references and code snippets — the strongest reasoning model reduces false-positives in the verification |
| Bug Fixer | `claude-sonnet-4-6` | Applying a pre-written, exact implementation plan is a routine text transformation — Sonnet is fast and fully capable for this task |
| Security Verifier | `claude-opus-4-8` | Security reasoning (injection vectors, credential handling, OWASP patterns) benefits from deeper analysis to avoid missed findings |
| Unit Test Generator | `claude-sonnet-4-6` | Generating Jest tests from a known fix list is structured, mechanical work — Sonnet produces clean, idiomatic test code efficiently |

---

## Skills

| Skill | Used by | Purpose |
|-------|---------|---------|
| `skills/research-quality-measurement.md` | Research Verifier | Defines VERIFIED / PARTIAL / UNVERIFIED / INVALID quality rubric |
| `skills/unit-tests-FIRST.md` | Unit Test Generator | Defines FIRST (Fast, Independent, Repeatable, Self-validating, Timely) checklist |

Skills are auto-loaded by `pipeline.js` — any `skills/*.md` reference found in agent instructions is injected into that agent's system prompt automatically.

---

## Project Structure

```
homework-4/
├── README.md
├── HOWTORUN.md
├── IMPLEMENTATION_PLAN.md
├── package.json                          # npm run pipeline | npm test | npm run reset
├── pipeline.js                           # orchestrates all 4 agents via Claude Code CLI
├── reset.js                              # restores buggy baseline + clears outputs (re-run)
├── personas.js                           # CLI-only persona banter (PIPELINE_DRAMA=0 to mute)
├── fixtures/app.buggy.js                 # pristine "before" source (reset baseline)
├── run-pipeline.sh                       # shell wrapper for npm run pipeline
├── agents/
│   ├── research-verifier.agent.md        # model: claude-opus-4-8
│   ├── bug-fixer.agent.md                # model: claude-sonnet-4-6
│   ├── security-verifier.agent.md        # model: claude-opus-4-8
│   └── unit-test-generator.agent.md      # model: claude-sonnet-4-6
├── skills/
│   ├── research-quality-measurement.md
│   └── unit-tests-FIRST.md
├── context/bugs/
│   ├── 001/bug-context.md               # off-by-one in getTopExpenses
│   ├── 002/bug-context.md               # wrong tax calculation
│   └── 003/bug-context.md               # eval injection + hardcoded secret
├── research/
│   └── codebase-research.md             # pre-seeded Bug Researcher output
├── implementation-plan.md               # pre-seeded Bug Planner output
├── src/
│   ├── app.js                           # Expense Tracker (buggy before pipeline)
│   └── utils.js
├── tests/
│   └── app.test.js                      # initial tests (2 fail before pipeline)
└── docs/screenshots/
```

---

## Pipeline Outputs (generated at runtime)

| File | Created by |
|------|-----------|
| `research/verified-research.md` | Bug Research Verifier |
| `fix-summary.md` | Bug Fixer |
| `src/app.js` (patched) | Bug Fixer |
| `security-report.md` | Security Verifier |
| `tests/app.fixed.test.js` | Unit Test Generator |
| `test-report.md` | Unit Test Generator |