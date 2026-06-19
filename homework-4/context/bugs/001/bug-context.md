# Bug 001 — Off-by-One in getTopExpenses

## Summary
`getTopExpenses(n)` always skips the highest-value expense due to an off-by-one error in the array slice.

## Location
**File**: `src/app.js`
**Line**: 26
**Function**: `getTopExpenses(n)`

## Reproduction
```javascript
const tracker = new ExpenseTracker();
tracker.addExpense('Coffee', 5, 'food');
tracker.addExpense('Laptop', 1200, 'tech');
tracker.addExpense('Book', 25, 'education');

tracker.getTopExpenses(1);
// Returns: [{ name: 'Book', amount: 25 }]  ← WRONG
// Expected: [{ name: 'Laptop', amount: 1200 }]
```

## Root Cause
```javascript
// BUGGY
return sorted.slice(1, n + 1);  // starts at index 1, skipping the #1 item

// CORRECT
return sorted.slice(0, n);      // starts at index 0
```

After sorting descending, the highest item is at index 0. `slice(1, n+1)` discards it and returns items 1…n instead of 0…n-1.

## Impact
- Callers relying on "top N" results always receive wrong data.
- Dashboard totals, budget alerts, and reports built on this method are silently incorrect.

## Severity
**Medium** — data integrity issue; no crash, but produces wrong output.