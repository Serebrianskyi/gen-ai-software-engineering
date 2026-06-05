# research/verified-research.md

## 1. Verification Summary

**Verdict: PASS** — No claim is INVALID. All four cited issues exist in the source with verbatim snippets and accurate root causes; however, every line number is shifted by ~10 lines, so no claim qualifies as fully VERIFIED.

**Overall Research Quality: PARTIAL — every snippet is verbatim and every root cause is correct, but all line-number citations are off by more than ±2 (consistently ~10 lines low).**

---

## 2. Verified Claims

### BUG-001 — Off-by-One in getTopExpenses
- **File:line checked**: `src/app.js:36` (research claimed line 26)
- **Snippet match**: exact — `return sorted.slice(1, n + 1);`
- **Root cause accuracy**: correct — descending sort places the max at index 0; `slice(1, n + 1)` drops it.

### BUG-002 — Incorrect Tax Calculation
- **File:line checked**: `src/app.js:42` (research claimed line 32)
- **Snippet match**: exact — `return subtotal + taxRate;`
- **Root cause accuracy**: correct — `taxRate` is a fractional multiplier added as a flat amount; the worked example (1245.00 → 1245.1 vs. 1369.5) is sound.

### SEC-003a — eval() Code Injection
- **File:line checked**: `src/app.js:48–49` (research claimed lines 38–40)
- **Snippet match**: exact — both the `// eslint-disable-next-line no-eval` comment and `return this.expenses.filter(e => eval(filterExpr));` match character-for-character.
- **Root cause accuracy**: correct — caller-supplied `filterExpr` flows directly into `eval()`.

### SEC-003b — Hardcoded Credential
- **File:line checked**: `src/app.js:61` (research claimed line 51)
- **Snippet match**: exact — `const ADMIN_KEY = 'secret123';`
- **Root cause accuracy**: correct — plaintext credential committed to source/git history.

**Secondary file**: `src/utils.js` confirmed clean — `formatCurrency` and `formatDate` contain no logic or security issues, matching the research's "Clean" status.

---

## 3. Discrepancies Found

| # | Research stated | Source actually contains | Severity |
|---|-----------------|--------------------------|----------|
| 1 | BUG-001 at line 26 | Snippet is at line 36 (off by 10) | major (per rule: >±2 ⇒ PARTIAL) |
| 2 | BUG-002 at line 32 | Snippet is at line 42 (off by 10) | major |
| 3 | SEC-003a at lines 38–40 | Snippet spans lines 48–49 (off by ~10; also a 2-line span, not 3) | major |
| 4 | SEC-003b at line 51 | Snippet is at line 61 (off by 10) | major |
| 5 | Executive Summary text: "two logic bugs and two security vulnerabilities (grouped as one issue)" | Wording is internally inconsistent ("Three issues found" then lists 2+2); content of issues themselves is accurate | minor |

All snippet text and root-cause explanations are correct; the discrepancies are exclusively in line-number citations (a consistent ~10-line downward offset, likely from an earlier version of the file lacking the header comment block).

---

## 4. Research Quality Assessment

- **Level: PARTIAL (Level 3)**
- **Reasoning**: Per the rubric, VERIFIED requires that *all* file:line references match the source. Every snippet here is verbatim and every root cause is accurate (which would otherwise push toward VERIFIED), but all four line numbers are wrong by ~10 — far beyond the ±2 tolerance — which the stated rules require be downgraded to PARTIAL rather than VERIFIED. No reference is to the wrong file or wrong function, and no snippet is missing or fabricated, so nothing drops to UNVERIFIED or INVALID. The consistent offset suggests the research was performed against a stale copy of the file.
- **Recommendation for the Bug Planner**: **Safe to proceed, but re-anchor line numbers manually.** The identified bugs, snippets, fixes, and severities are reliable; only the line citations need correction (add ~10 to each) before editing.

---

## 5. References

| Claim | File | Claimed Line | Actual Line | Match |
|-------|------|--------------|-------------|-------|
| BUG-001 getTopExpenses | src/app.js | 26 | 36 | PARTIAL (snippet exact, line off by 10) |
| BUG-002 calculateTotal | src/app.js | 32 | 42 | PARTIAL (snippet exact, line off by 10) |
| SEC-003a eval() | src/app.js | 38–40 | 48–49 | PARTIAL (snippet exact, line off by ~10) |
| SEC-003b ADMIN_KEY | src/app.js | 51 | 61 | PARTIAL (snippet exact, line off by 10) |
| utils.js clean | src/utils.js | — | 1–15 | VERIFIED (no issues present) |