# Bug 002 — Wrong Tax Calculation in calculateTotal

## Summary
`calculateTotal(withTax, taxRate)` adds the tax rate as a flat value instead of applying it as a percentage multiplier, producing wildly incorrect totals.

## Location
**File**: `src/app.js`
**Line**: 32
**Function**: `calculateTotal(withTax, taxRate)`

## Reproduction
```javascript
const tracker = new ExpenseTracker();
tracker.addExpense('Laptop', 1200, 'tech');

tracker.calculateTotal(true, 0.1);
// Returns: 1200.1   ← WRONG (subtotal + 0.1)
// Expected: 1320.0  ← CORRECT (subtotal * 1.1)
```

## Root Cause
```javascript
// BUGGY
return subtotal + taxRate;       // adds 0.1 to 1200 → 1200.1

// CORRECT
return subtotal * (1 + taxRate); // multiplies 1200 by 1.1 → 1320
```

The `taxRate` parameter is a decimal fraction (e.g. `0.1` = 10%). Adding it directly to the subtotal treats it as a currency amount rather than a percentage.

## Impact
- All tax-inclusive totals are understated by orders of magnitude.
- Invoices, reports, and compliance calculations depending on this method are incorrect.
- The bug is subtle: the return value is slightly above the subtotal, not obviously wrong for small amounts.

## Severity
**High** — financial calculation correctness issue with compliance implications.