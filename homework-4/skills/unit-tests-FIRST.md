# Skill: Unit Tests — FIRST Principles

Use this skill when generating unit tests. Every test you write must satisfy all five FIRST criteria before it is considered acceptable.

---

## The FIRST Principles

### F — Fast
- Each test must complete in milliseconds, not seconds.
- No network calls, no file I/O, no database connections inside the test.
- Use in-memory data and synchronous operations wherever possible.
- **Check**: Would this test still pass if the network were disconnected?

### I — Independent
- Tests must not depend on each other's execution order.
- Each test sets up its own state in `beforeEach` / local variables.
- No shared mutable state between tests.
- **Check**: Can you run a single test in isolation and get the same result?

### R — Repeatable
- The same test must produce the same result every time, on any machine.
- No reliance on current time, random values, environment variables, or external services.
- If time-dependent logic must be tested, inject a clock or freeze time.
- **Check**: Run the test 100 times — does it always pass or always fail?

### S — Self-Validating
- Tests must produce a clear boolean outcome: pass or fail.
- No manual inspection of output required to determine correctness.
- Use `expect(...).toBe(...)` / `expect(...).toThrow(...)` style assertions — never `console.log`.
- **Check**: Can a CI system determine pass/fail without human review?

### T — Timely
- Tests are written for the code that just changed — no more, no less.
- Do not write tests for unchanged code in the same batch.
- Cover the happy path, boundary conditions, and error paths for each changed function.
- **Check**: Does every test in this batch map to a specific change in the fix summary?

---

## Applying FIRST in test-report.md

For each generated test, include a one-line FIRST compliance note:
```
Test: <test name>
FIRST: F✓ I✓ R✓ S✓ T✓  — <any deviations noted here>
```

If a principle is violated, mark it with ✗ and explain why and how to fix it.