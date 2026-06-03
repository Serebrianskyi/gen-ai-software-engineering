# Implementation Plan — Expense Tracker Bug Fixes

**Planner**: Bug Planner Agent
**Date**: 2026-06-01
**Source**: `research/verified-research.md`
**Test command**: `npm test` (run after each fix; all tests must pass at the end)

---

## Fix 001 — Off-by-One in getTopExpenses

**File**: `src/app.js`
**Line**: 26

**Before**:
```javascript
return sorted.slice(1, n + 1);
```

**After**:
```javascript
return sorted.slice(0, n);
```

**Verification**: Run `npm test`. The test `returns the highest expense first` must now pass.

---

## Fix 002 — Wrong Tax Calculation

**File**: `src/app.js`
**Line**: 32

**Before**:
```javascript
return subtotal + taxRate;
```

**After**:
```javascript
return subtotal * (1 + taxRate);
```

**Verification**: Run `npm test`. The test `returns correct total with 10% tax` must now pass.

---

## Fix 003a — Remove eval() — Safe Filter Replacement

**File**: `src/app.js`
**Lines**: 37–40

**Before**:
```javascript
filterExpenses(filterExpr) {
  // eslint-disable-next-line no-eval
  return this.expenses.filter(e => eval(filterExpr));
}
```

**After**:
```javascript
filterExpenses(filter) {
  if (typeof filter !== 'object' || filter === null) {
    throw new Error('filter must be an object with field and value properties');
  }
  const { field, value } = filter;
  return this.expenses.filter(e => e[field] === value);
}
```

**Verification**: Manually confirm `eval` no longer appears in `src/app.js`.

---

## Fix 003b — Move Hardcoded Credential to Environment Variable

**File**: `src/app.js`
**Line**: 51

**Before**:
```javascript
const ADMIN_KEY = 'secret123';
```

**After**:
```javascript
const ADMIN_KEY = process.env.ADMIN_KEY;
if (!ADMIN_KEY) {
  console.warn('Warning: ADMIN_KEY environment variable is not set');
}
```

**Verification**: Manually confirm the string `'secret123'` no longer appears in `src/app.js`.

---

## Post-Fix Checklist

- [ ] `npm test` — all tests pass (0 failures)
- [ ] `eval` does not appear in `src/app.js`
- [ ] `secret123` does not appear in `src/app.js`
- [ ] `fix-summary.md` written with before/after for each change