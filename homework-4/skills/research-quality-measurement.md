# Skill: Research Quality Measurement

Use this skill to assess and label the quality of bug research. Apply it when writing a `verified-research.md` report.

---

## Quality Levels

| Level | Label | Criteria |
|-------|-------|----------|
| 4 | **VERIFIED** | All file:line references exist and match source; all code snippets are verbatim; root cause explanation is accurate; reproduction steps are reproducible |
| 3 | **PARTIAL** | Most references correct; minor discrepancies (wrong line number ±2, slightly paraphrased snippet); root cause is directionally correct |
| 2 | **UNVERIFIED** | References point to correct files but wrong functions or lines; snippets are approximations; root cause is plausible but not confirmed against source |
| 1 | **INVALID** | References do not exist; snippets do not appear in source; root cause is incorrect or contradicts the code |

---

## How to Apply

1. For each claim in the research document:
   - Locate the referenced file and line number in the actual source.
   - Compare the quoted snippet character-for-character against the source.
   - Evaluate whether the described root cause matches the code.

2. Assign an individual level (VERIFIED / PARTIAL / UNVERIFIED / INVALID) per claim.

3. Compute an **overall Research Quality** using the lowest level assigned to any critical claim:
   - If any claim is INVALID → overall = INVALID
   - If any critical claim is UNVERIFIED → overall = UNVERIFIED
   - If all claims are VERIFIED or PARTIAL → overall = PARTIAL or VERIFIED based on majority

4. In the report, state:
   ```
   Research Quality: <LEVEL> — <one-sentence justification>
   ```

---

## Required Sections in verified-research.md

1. **Verification Summary** — pass/fail verdict + overall Research Quality label
2. **Verified Claims** — list of claims confirmed correct with evidence
3. **Discrepancies Found** — list of mismatches (expected vs actual), or "None"
4. **Research Quality Assessment** — level label + reasoning per the rubric above
5. **References** — exact file:line citations checked