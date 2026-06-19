'use strict';

// Tests for the four changes recorded in fix-summary.md.
// Existing coverage in tests/app.test.js is NOT duplicated here.

const { ExpenseTracker, ADMIN_KEY } = require('../src/app');

// ─────────────────────────────────────────────────────────────────────────────
// FIX-001 · getTopExpenses — slice now starts at index 0, not index 1
// ─────────────────────────────────────────────────────────────────────────────
describe('getTopExpenses — FIX-001: slice corrected to start at index 0', () => {
  let tracker;

  beforeEach(() => {
    tracker = new ExpenseTracker();
    // Inserted in non-sorted order to prove sort-then-slice works correctly
    tracker.addExpense('Coffee', 5.00, 'food');      // rank 4 (lowest)
    tracker.addExpense('Laptop', 1200.00, 'tech');   // rank 1 (highest)
    tracker.addExpense('Book', 25.00, 'education');  // rank 2
    tracker.addExpense('Lunch', 15.00, 'food');      // rank 3
  });

  it('returns an empty array when n is 0 (lower boundary)', () => {
    expect(tracker.getTopExpenses(0)).toEqual([]);
  });

  it('returns all expenses when n exceeds the total expense count (upper boundary)', () => {
    const result = tracker.getTopExpenses(100);
    expect(result).toHaveLength(4);
  });

  it('excludes lower-ranked expenses from a top-2 result', () => {
    // Before fix: slice(1, 3) skipped rank-1 so ranks 2 & 3 appeared — Lunch could surface
    // After fix:  slice(0, 2) correctly returns ranks 1 & 2 only
    const top2 = tracker.getTopExpenses(2);
    const names = top2.map(e => e.name);
    expect(names).not.toContain('Lunch');    // rank 3 — must not be in top 2
    expect(names).not.toContain('Coffee');   // rank 4 — must not be in top 2
  });

  it('places the second-highest expense at index 1, not the third (old off-by-one regression)', () => {
    // Before fix: slice(1, 3) on the sorted array yielded [rank2, rank3] = [Book, Lunch]
    // After fix:  slice(0, 2) yields [rank1, rank2] = [Laptop, Book]
    const top2 = tracker.getTopExpenses(2);
    expect(top2[1].name).toBe('Book');
    expect(top2[1].amount).toBe(25.00);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// FIX-002 · calculateTotal — now multiplies (1 + taxRate) instead of adding
// ─────────────────────────────────────────────────────────────────────────────
describe('calculateTotal — FIX-002: tax applied by multiplication, not addition', () => {
  let tracker;

  beforeEach(() => {
    tracker = new ExpenseTracker();
    tracker.addExpense('Coffee', 5.00, 'food');
    tracker.addExpense('Laptop', 1200.00, 'tech');
    tracker.addExpense('Book', 25.00, 'education');
    tracker.addExpense('Lunch', 15.00, 'food');
    // subtotal = 1245.00
  });

  it('applies a custom 20% tax rate correctly using multiplication (subtotal × 1.2)', () => {
    // Old bug: 1245 + 0.2 = 1245.2 — correct: 1245 × 1.2 = 1494
    expect(tracker.calculateTotal(true, 0.2)).toBeCloseTo(1494.00, 2);
  });

  it('returns 0 when there are no expenses even with tax requested', () => {
    // Old bug: 0 + 0.1 = 0.1 — correct: 0 × 1.1 = 0
    const empty = new ExpenseTracker();
    expect(empty.calculateTotal(true, 0.1)).toBe(0);
  });

  it('returns the subtotal unchanged when taxRate is 0 and withTax is true (zero-tax boundary)', () => {
    // subtotal × (1 + 0) = subtotal × 1 = 1245
    expect(tracker.calculateTotal(true, 0)).toBe(1245.00);
  });

  it('result with 10% tax does not equal subtotal + 0.1 (regression: old addition formula)', () => {
    // Old broken result: 1245 + 0.1 = 1245.1
    // Correct result:    1245 × 1.1 = 1369.5
    const withTax = tracker.calculateTotal(true, 0.1);
    expect(withTax).not.toBeCloseTo(1245.1, 2);  // old broken value must not appear
    expect(withTax).toBeCloseTo(1369.5, 2);       // correct multiplied value
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// FIX-003a · filterExpenses — eval removed; object-based filter required
// ─────────────────────────────────────────────────────────────────────────────
describe('filterExpenses — FIX-003a: safe object-based filtering replaces eval', () => {
  let tracker;

  beforeEach(() => {
    tracker = new ExpenseTracker();
    tracker.addExpense('Coffee', 5.00, 'food');
    tracker.addExpense('Laptop', 1200.00, 'tech');
    tracker.addExpense('Book', 25.00, 'education');
    tracker.addExpense('Lunch', 15.00, 'food');
  });

  // ── Happy path ──────────────────────────────────────────────────────────────

  it('returns all expenses whose category matches the filter value', () => {
    const result = tracker.filterExpenses({ field: 'category', value: 'food' });
    expect(result).toHaveLength(2);
    result.forEach(e => expect(e.category).toBe('food'));
  });

  it('returns the single expense that matches the specified name', () => {
    const result = tracker.filterExpenses({ field: 'name', value: 'Laptop' });
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('Laptop');
  });

  it('returns the single expense that matches the specified amount', () => {
    const result = tracker.filterExpenses({ field: 'amount', value: 25.00 });
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('Book');
  });

  // ── Boundary ────────────────────────────────────────────────────────────────

  it('returns an empty array when no expense satisfies the filter', () => {
    const result = tracker.filterExpenses({ field: 'category', value: 'travel' });
    expect(result).toEqual([]);
  });

  it('returns all expenses when every expense matches the filter', () => {
    const t = new ExpenseTracker();
    t.addExpense('Alpha', 10, 'uniform');
    t.addExpense('Beta', 20, 'uniform');
    const result = t.filterExpenses({ field: 'category', value: 'uniform' });
    expect(result).toHaveLength(2);
  });

  // ── Error paths — new code must reject anything that is not a plain object ──

  it('throws when filter is a string (old eval-style expression is no longer accepted)', () => {
    expect(() => tracker.filterExpenses('e.category === "food"'))
      .toThrow('filter must be an object with field and value properties');
  });

  it('throws when filter is null', () => {
    expect(() => tracker.filterExpenses(null))
      .toThrow('filter must be an object with field and value properties');
  });

  it('throws when filter is undefined', () => {
    expect(() => tracker.filterExpenses(undefined))
      .toThrow('filter must be an object with field and value properties');
  });

  it('throws when filter is a number', () => {
    expect(() => tracker.filterExpenses(42))
      .toThrow('filter must be an object with field and value properties');
  });

  it('throws when filter is a boolean', () => {
    expect(() => tracker.filterExpenses(false))
      .toThrow('filter must be an object with field and value properties');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// FIX-003b · ADMIN_KEY — hardcoded credential replaced by process.env.ADMIN_KEY
// ─────────────────────────────────────────────────────────────────────────────
describe('ADMIN_KEY — FIX-003b: credential read from process.env, not hardcoded', () => {
  it('is not equal to the old hardcoded credential "secret123"', () => {
    expect(ADMIN_KEY).not.toBe('secret123');
  });

  it('reflects the value of the ADMIN_KEY environment variable present at module load time', () => {
    const originalKey = process.env.ADMIN_KEY;
    try {
      jest.resetModules();
      process.env.ADMIN_KEY = 'test-key-abc-123';
      // Re-require with the env variable now set to observe the captured value
      const { ADMIN_KEY: freshKey } = require('../src/app');
      expect(freshKey).toBe('test-key-abc-123');
    } finally {
      // Always restore env and module registry regardless of test outcome
      if (originalKey === undefined) {
        delete process.env.ADMIN_KEY;
      } else {
        process.env.ADMIN_KEY = originalKey;
      }
      jest.resetModules();
    }
  });

  it('is undefined when the ADMIN_KEY environment variable is not set at module load time', () => {
    const originalKey = process.env.ADMIN_KEY;
    try {
      jest.resetModules();
      delete process.env.ADMIN_KEY;
      const { ADMIN_KEY: freshKey } = require('../src/app');
      expect(freshKey).toBeUndefined();
    } finally {
      if (originalKey !== undefined) {
        process.env.ADMIN_KEY = originalKey;
      }
      jest.resetModules();
    }
  });
});