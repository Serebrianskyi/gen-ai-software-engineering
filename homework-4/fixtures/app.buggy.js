'use strict';

// ─────────────────────────────────────────────────────────────────────────────
// PRISTINE "BEFORE" SOURCE — seeded with intentional bugs/vulnerabilities.
// This file is the reset baseline; `npm run reset` copies it to src/app.js so
// the pipeline always starts from a known buggy state. Do NOT fix bugs here.
//   BUG-001  getTopExpenses   slice(1, n + 1) skips the highest expense
//   BUG-002  calculateTotal   subtotal + taxRate instead of subtotal * (1+rate)
//   SEC-003a filterExpenses   eval() on caller-supplied expression
//   SEC-003b ADMIN_KEY        hardcoded credential
// ─────────────────────────────────────────────────────────────────────────────

class ExpenseTracker {
  constructor() {
    this.expenses = [];
  }

  addExpense(name, amount, category) {
    if (!name || amount === undefined || amount === null) {
      throw new Error('Name and amount are required');
    }
    if (typeof amount !== 'number' || amount < 0) {
      throw new Error('Amount must be a non-negative number');
    }
    this.expenses.push({
      id: this.expenses.length + 1,
      name,
      amount,
      category: category || 'general',
      date: new Date().toISOString(),
    });
  }

  getTopExpenses(n) {
    const sorted = [...this.expenses].sort((a, b) => b.amount - a.amount);
    return sorted.slice(1, n + 1);
  }

  calculateTotal(withTax = false, taxRate = 0.1) {
    const subtotal = this.expenses.reduce((sum, e) => sum + e.amount, 0);
    if (withTax) {
      return subtotal + taxRate;
    }
    return subtotal;
  }

  filterExpenses(filterExpr) {
    // eslint-disable-next-line no-eval
    return this.expenses.filter(e => eval(filterExpr));
  }

  getSummary() {
    return {
      total: this.calculateTotal(),
      count: this.expenses.length,
      categories: [...new Set(this.expenses.map(e => e.category))],
    };
  }
}

const ADMIN_KEY = 'secret123';

module.exports = { ExpenseTracker, ADMIN_KEY };