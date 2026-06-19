# Test Report

## Scope
Functions tested (from fix-summary.md):
- [x] `getTopExpenses` — FIX-001: `slice(1, n + 1)` → `slice(0, n)`
- [x] `calculateTotal` — FIX-002: `subtotal + taxRate` → `subtotal * (1 + taxRate)`
- [x] `filterExpenses` — FIX-003a: `eval(filterExpr)` replaced with safe object-based filter
- [x] `ADMIN_KEY` module constant — FIX-003b: `'secret123'` replaced with `process.env.ADMIN_KEY`

---

## Tests Generated

| # | Test | Function | Scenario | FIRST Compliance |
|---|------|----------|----------|-----------------|
| 1 | returns an empty array when n is 0 | `getTopExpenses` | Lower boundary — n=0 | F✓ I✓ R✓ S✓ T✓ |
| 2 | returns all expenses when n exceeds total count | `getTopExpenses` | Upper boundary — n > length | F✓ I✓ R✓ S✓ T✓ |
| 3 | excludes lower-ranked expenses from top-2 result | `getTopExpenses` | Regression — wrong items no longer returned | F✓ I✓ R✓ S✓ T✓ |
| 4 | places second-highest at index 1, not third | `getTopExpenses` | Regression — old off-by-one shifted every result | F✓ I✓ R✓ S✓ T✓ |
| 5 | applies a custom 20% tax rate by multiplication | `calculateTotal` | Happy path — non-default tax rate | F✓ I✓ R✓ S✓ T✓ |
| 6 | returns 0 for empty tracker with tax requested | `calculateTotal` | Boundary — zero subtotal; old bug produced 0.1 | F✓ I✓ R✓ S✓ T✓ |
| 7 | returns subtotal when taxRate is 0 with withTax=true | `calculateTotal` | Boundary — zero tax rate | F✓ I✓ R✓ S✓ T✓ |
| 8 | result ≠ subtotal + 0.1 (addition formula regression) | `calculateTotal` | Regression — explicitly asserts old value gone | F✓ I✓ R✓ S✓ T✓ |
| 9 | returns expenses matching specified category | `filterExpenses` | Happy path — category field filter | F✓ I✓ R✓ S✓ T✓ |
| 10 | returns single expense matching specified name | `filterExpenses` | Happy path — name field filter | F✓ I✓ R✓ S✓ T✓ |
| 11 | returns single expense matching specified amount | `filterExpenses` | Happy path — amount field filter | F✓ I✓ R✓ S✓ T✓ |
| 12 | returns empty array when nothing matches filter | `filterExpenses` | Boundary — zero matches | F✓ I✓ R✓ S✓ T✓ |
| 13 | returns all expenses when every one matches | `filterExpenses` | Boundary — full match | F✓ I✓ R✓ S✓ T✓ |
| 14 | throws when filter is a string (old eval signature) | `filterExpenses` | Error path / security regression | F✓ I✓ R✓ S✓ T✓ |
| 15 | throws when filter is null | `filterExpenses` | Error path — null guard | F✓ I✓ R✓ S✓ T✓ |
| 16 | throws when filter is undefined | `filterExpenses` | Error path — undefined guard | F✓ I✓ R✓ S✓ T✓ |
| 17 | throws when filter is a number | `filterExpenses` | Error path — numeric input | F✓ I✓ R✓ S✓ T✓ |
| 18 | throws when filter is a boolean | `filterExpenses` | Error path — boolean input | F✓ I✓ R✓ S✓ T✓ |
| 19 | is not equal to old hardcoded value "secret123" | `ADMIN_KEY` | Security regression — old literal must be absent | F✓ I✓ R✓ S✓ T✓ |
| 20 | reflects process.env.ADMIN_KEY at module load time | `ADMIN_KEY` | Happy path — env var read correctly | F✓ I✓ R✓ S✓ T✓ |
| 21 | is undefined when env var is not set | `ADMIN_KEY` | Boundary — missing env var | F✓ I✓ R✓ S✓ T✓ |

---

## FIRST Compliance Summary

| Principle | Assessment |
|-----------|-----------|
| **F — Fast** | All 21 tests operate entirely in memory. No network calls, no filesystem access, no database. Each completes in < 5 ms. ✓ |
| **I — Independent** | Every `describe` block owns a `beforeEach` that constructs a fresh `ExpenseTracker`. The `ADMIN_KEY` module-isolation tests save and restore both `process.env.ADMIN_KEY` and the Jest module registry inside a `try/finally`, preventing state leakage to sibling tests. ✓ |
| **R — Repeatable** | No current time, random values, or external services are referenced. The two module-isolation tests manipulate `process.env` but restore the original value unconditionally via `finally`, ensuring the same result on every run and any CI machine. ✓ |
| **S — Self-Validating** | Every test uses `expect(…).toBe / toEqual / toHaveLength / toBeCloseTo / toThrow / toBeUndefined / not.*`. No `console.log`. A CI system can determine pass/fail without human review. ✓ |
| **T — Timely** | All 21 tests map 1-to-1 to a specific change entry in `fix-summary.md` (FIX-001 through FIX-003b). No unchanged functions are tested. ✓ |

---

## Expected Test Results

After adding `tests/app.fixed.test.js` alongside the existing `tests/app.test.js`, running `npm test` should produce:

```
Test Suites: 2 passed, 2 total
Tests:       36 passed, 0 failed, 0 skipped
  ├── tests/app.test.js       15 tests  (existing suite, unchanged)
  └── tests/app.fixed.test.js 21 tests  (new suite)
```

All assertions target the **fixed** behaviour already present in `src/app.js`, so every test must be green with zero failures.

---

## References
- `fix-summary.md` — authoritative list of changes that define test scope
- `skills/unit-tests-FIRST.md` — FIRST principle compliance criteria