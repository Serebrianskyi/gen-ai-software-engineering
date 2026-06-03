'use strict';

const { ExpenseTracker, ADMIN_KEY } = require('../src/app');

describe('ExpenseTracker', () => {
  let tracker;

  beforeEach(() => {
    tracker = new ExpenseTracker();
    tracker.addExpense('Coffee', 5.00, 'food');
    tracker.addExpense('Laptop', 1200.00, 'tech');
    tracker.addExpense('Book', 25.00, 'education');
    tracker.addExpense('Lunch', 15.00, 'food');
  });

  describe('addExpense', () => {
    it('adds an expense to the list', () => {
      const t = new ExpenseTracker();
      t.addExpense('Test', 10, 'misc');
      expect(t.expenses).toHaveLength(1);
    });

    it('assigns incrementing id', () => {
      expect(tracker.expenses[0].id).toBe(1);
      expect(tracker.expenses[3].id).toBe(4);
    });

    it('throws when name is missing', () => {
      expect(() => tracker.addExpense('', 10, 'misc')).toThrow('Name and amount are required');
    });

    it('throws when amount is negative', () => {
      expect(() => tracker.addExpense('Bad', -5, 'misc')).toThrow('Amount must be a non-negative number');
    });

    it('defaults category to general when omitted', () => {
      const t = new ExpenseTracker();
      t.addExpense('X', 1);
      expect(t.expenses[0].category).toBe('general');
    });
  });

  describe('getTopExpenses', () => {
    it('returns correct number of results', () => {
      const top2 = tracker.getTopExpenses(2);
      expect(top2).toHaveLength(2);
    });

    it('returns the highest expense first — FAILS before fix (BUG-001)', () => {
      // BUG-001: slice(1, n+1) skips index 0 (the highest), so Laptop is never returned
      const top1 = tracker.getTopExpenses(1);
      expect(top1[0].name).toBe('Laptop'); // highest amount = 1200
    });

    it('sorts by amount descending', () => {
      const top3 = tracker.getTopExpenses(3);
      expect(top3[0].amount).toBeGreaterThanOrEqual(top3[1].amount);
      expect(top3[1].amount).toBeGreaterThanOrEqual(top3[2].amount);
    });
  });

  describe('calculateTotal', () => {
    it('returns correct subtotal without tax', () => {
      expect(tracker.calculateTotal()).toBe(1245.00);
    });

    it('returns correct total with 10% tax — FAILS before fix (BUG-002)', () => {
      // BUG-002: subtotal + 0.1 = 1245.1, not subtotal * 1.1 = 1369.5
      const total = tracker.calculateTotal(true, 0.1);
      expect(total).toBeCloseTo(1369.50, 2);
    });

    it('returns subtotal unchanged when withTax is false', () => {
      expect(tracker.calculateTotal(false, 0.2)).toBe(1245.00);
    });
  });

  describe('getSummary', () => {
    it('returns correct expense count', () => {
      expect(tracker.getSummary().count).toBe(4);
    });

    it('returns unique categories only', () => {
      const { categories } = tracker.getSummary();
      expect(categories).toContain('food');
      expect(categories).toContain('tech');
      expect(categories).toContain('education');
      expect(new Set(categories).size).toBe(categories.length);
    });

    it('includes total in summary', () => {
      expect(tracker.getSummary().total).toBe(1245.00);
    });
  });
});

describe('ADMIN_KEY', () => {
  it('is exported', () => {
    // The binding must always be exported. Its value is 'secret123' before the
    // security fix and process.env.ADMIN_KEY (possibly undefined) after — so we
    // assert the export exists rather than that it holds a truthy value.
    const appModule = require('../src/app');
    expect('ADMIN_KEY' in appModule).toBe(true);
  });
});