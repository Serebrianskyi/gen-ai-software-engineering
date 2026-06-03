# security-report.md

```markdown
# Security Audit Report

## 1. Executive Summary

- **Audit scope**: `src/app.js` (post-fix), cross-referenced against `fix-summary.md`.
- **Total findings by severity**:
  - CRITICAL: 0
  - HIGH: 0
  - MEDIUM: 0
  - LOW: 2
  - INFO: 1
- **Overall verdict**: ISSUES FOUND (low severity / defence-in-depth only). All previously identified CRITICAL/HIGH vulnerabilities (eval injection, hardcoded credential) are confirmed fully resolved.

---

## 2. Findings

#### [LOW] Finding 1 — `filterExpenses` allows reads against prototype/special keys
- **File**: src/app.js
- **Line(s)**: 52–53
- **Code**:
  ```javascript
  const { field, value } = filter;
  return this.expenses.filter(e => e[field] === value);
  ```
- **Description**: `field` is fully caller-controlled and used directly as a dynamic property accessor. While this is a *read* (no assignment, so it cannot cause prototype pollution), an attacker can probe inherited/special keys such as `__proto__`, `constructor`, or `toString` against expense objects. There is no validation that `field` is a string or that it corresponds to an expected own data property. This is an information-disclosure / logic-bypass hardening gap rather than an exploitable injection.
- **Remediation**: Whitelist allowed fields and require `field` to be a string:
  ```javascript
  const ALLOWED_FIELDS = new Set(['id', 'name', 'amount', 'category', 'date']);
  if (typeof field !== 'string' || !ALLOWED_FIELDS.has(field)) {
    throw new Error('Invalid filter field');
  }
  return this.expenses.filter(e => Object.prototype.hasOwnProperty.call(e, field) && e[field] === value);
  ```

#### [LOW] Finding 2 — `typeof filter === 'object'` accepts arrays and yields silent no-op
- **File**: src/app.js
- **Line(s)**: 49–53
- **Code**:
  ```javascript
  if (typeof filter !== 'object' || filter === null) {
    throw new Error('filter must be an object with field and value properties');
  }
  const { field, value } = filter;
  ```
- **Description**: `typeof []` is `'object'`, so passing an array (or any object lacking `field`) bypasses validation. `field` then resolves to `undefined`, and `e[undefined] === value` silently returns an empty result set instead of a clear error. This is a missing-input-validation / logic-bypass gap; not directly exploitable but can mask caller errors.
- **Remediation**: Tighten validation and require `field` to be present:
  ```javascript
  if (typeof filter !== 'object' || filter === null || Array.isArray(filter) || !('field' in filter)) {
    throw new Error('filter must be a plain object with field and value properties');
  }
  ```

#### [INFO] Finding 3 — `ADMIN_KEY` exported but never used for comparison in this module
- **File**: src/app.js
- **Line(s)**: 61–67
- **Code**:
  ```javascript
  const ADMIN_KEY = process.env.ADMIN_KEY;
  ...
  module.exports = { ExpenseTracker, ADMIN_KEY };
  ```
- **Description**: No security flaw in this file. Note for downstream consumers: if `ADMIN_KEY` is ever compared against user input elsewhere, use a constant-time comparison (`crypto.timingSafeEqual`) to avoid timing side channels. No comparison exists in `src/app.js`, so there is nothing to fix here — recorded for awareness only.
- **Remediation**: None required in this file. Ensure any consumer performing auth comparison uses timing-safe comparison and validates that `ADMIN_KEY` is set before authenticating.

---

## 3. Fix Verification

**FIX-001 (getTopExpenses — `slice(0, n)`)**
- Correctly applied? **Yes** (line 36).
- Resolves issue? **Yes** — logic correctness, no security impact.

**FIX-002 (calculateTotal — `subtotal * (1 + taxRate)`)**
- Correctly applied? **Yes** (line 42).
- Resolves issue? **Yes** — logic correctness, no security impact.

**FIX-003a (filterExpenses — eval removed, object-based filter)**
- Correctly applied? **Yes** — the `eval(filterExpr)` call is fully removed (confirmed: no `eval` in file) and replaced with structured property comparison (lines 48–54).
- Resolves vulnerability? **Yes** — the arbitrary code-execution (injection) vulnerability is fully eliminated. Residual items in Findings 1 & 2 are LOW defence-in-depth hardening, not a re-occurrence of the CRITICAL injection.

**FIX-003b (ADMIN_KEY — moved to environment variable)**
- Correctly applied? **Yes** — hardcoded `'secret123'` removed (confirmed: not present), replaced with `process.env.ADMIN_KEY` plus a not-set warning (lines 61–67).
- Resolves vulnerability? **Yes** — the hardcoded-credential exposure is fully remediated.

---

## 4. Recommendations

Prioritised next steps:

1. **(LOW)** Add a field whitelist + `hasOwnProperty` check in `filterExpenses` (Finding 1) to prevent reads against prototype/special keys.
2. **(LOW)** Tighten the `filter` type guard to reject arrays and objects missing `field` (Finding 2) so misuse fails loudly rather than silently.
3. **(INFO)** Document for consumers of the exported `ADMIN_KEY` that any equality check against user input must use `crypto.timingSafeEqual` (Finding 3).
4. Consider failing fast (throwing) instead of only warning when `ADMIN_KEY` is unset, if the key is required for the application to operate securely.
```