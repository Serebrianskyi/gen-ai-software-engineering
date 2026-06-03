# Codebase Research — Expense Tracker Bug Report

**Researcher**: Bug Researcher Agent
**Date**: 2026-06-01
**Target**: `src/app.js`, `src/utils.js`

---

## Executive Summary

Three issues found in `src/app.js`: two logic bugs and two security vulnerabilities (grouped as one issue). `src/utils.js` is clean. All issues are in the `ExpenseTracker` class or module-level constants.

---

## Issue 1 — Off-by-One in getTopExpenses (BUG-001)

**File**: `src/app.js`
**Line**: 26
**Function**: `getTopExpenses(n)`

**Snippet**:
```javascript
return sorted.slice(1, n + 1);
```

**Analysis**: After sorting in descending order, the highest-value expense sits at index 0. Using `slice(1, n + 1)` skips that index and returns elements 1 through n, meaning the single most expensive item is never included in any "top N" result.

**Expected fix**: `slice(0, n)`

**Test impact**: `getTopExpenses` test for the highest expense fails.

---

## Issue 2 — Incorrect Tax Calculation (BUG-002)

**File**: `src/app.js`
**Line**: 32
**Function**: `calculateTotal(withTax, taxRate)`

**Snippet**:
```javascript
return subtotal + taxRate;
```

**Analysis**: `taxRate` is a decimal fraction (e.g. `0.1` for 10%). Adding it directly to `subtotal` treats it as a currency amount rather than a multiplier. For a subtotal of 1245.00 with 10% tax, the result is 1245.1 instead of the correct 1369.5.

**Expected fix**: `return subtotal * (1 + taxRate);`

**Test impact**: `calculateTotal with 10% tax` test fails.

---

## Issue 3 — Security: eval() and Hardcoded Credential (SEC-003)

### 3a — eval() Code Injection

**File**: `src/app.js`
**Lines**: 38–40
**Function**: `filterExpenses(filterExpr)`

**Snippet**:
```javascript
// eslint-disable-next-line no-eval
return this.expenses.filter(e => eval(filterExpr));
```

**Analysis**: `filterExpr` is passed directly to `eval()`. If this value originates from user input or an HTTP request body, an attacker can execute arbitrary JavaScript — reading environment variables, spawning child processes, or deleting files. The `eslint-disable` comment suggests the developer was aware of the issue but suppressed the warning rather than fixing it.

**Severity**: CRITICAL

### 3b — Hardcoded Credential

**File**: `src/app.js`
**Line**: 51

**Snippet**:
```javascript
const ADMIN_KEY = 'secret123';
```

**Analysis**: The credential is committed in plaintext to source control. Anyone with repository access has the key permanently, even after rotation, because it exists in git history.

**Severity**: HIGH

---

## Files Examined

| File | Lines | Status |
|------|-------|--------|
| `src/app.js` | 53 | Issues found (see above) |
| `src/utils.js` | 15 | Clean — no issues |
| `tests/app.test.js` | 76 | Two tests fail pre-fix (BUG-001, BUG-002) |