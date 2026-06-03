# Bug 003 — Security: eval() Injection + Hardcoded Credential

## Issue A — Code Injection via eval()

### Summary
`filterExpenses(filterExpr)` passes a user-supplied string directly to `eval()`, enabling arbitrary JavaScript execution.

### Location
**File**: `src/app.js`
**Lines**: 38–40
**Function**: `filterExpenses(filterExpr)`

### Reproduction
```javascript
// Legitimate use
tracker.filterExpenses("e.category === 'food'");

// Malicious use — executes arbitrary code
tracker.filterExpenses("(require('child_process').execSync('rm -rf /'))");
```

### Root Cause
```javascript
// VULNERABLE
return this.expenses.filter(e => eval(filterExpr));

// SAFE — object-based filter, no code execution
filterExpenses(filter) {
  const { field, value } = filter;
  return this.expenses.filter(e => e[field] === value);
}
```

### Impact
- **Remote Code Execution** if `filterExpr` originates from user input or an API request.
- Attacker can read files, exfiltrate data, or destroy system state.

### Severity
**CRITICAL**

---

## Issue B — Hardcoded Credential

### Summary
`ADMIN_KEY` is hardcoded as a string literal, exposing it to anyone with source access.

### Location
**File**: `src/app.js`
**Line**: 51

### Root Cause
```javascript
// VULNERABLE
const ADMIN_KEY = 'secret123';

// SAFE
const ADMIN_KEY = process.env.ADMIN_KEY;
```

### Impact
- Credential exposed in version control history permanently.
- Anyone with repo access can authenticate as admin.
- Rotation requires a code change and redeploy.

### Severity
**HIGH**