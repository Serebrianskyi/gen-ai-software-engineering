# Homework 3 — Specification-Driven Design

**Student**: Roman Serebrianskyi  
**Feature**: Virtual Card Lifecycle Management

---

## Task Summary

Produced a layered specification package for a virtual card lifecycle feature suitable for a PCI-DSS and GDPR regulated environment. No code was implemented. The deliverables are:

| File | Purpose |
|------|---------|
| `specification.md` | Full layered spec: objectives, non-functional requirements, implementation notes, context, low-level tasks, edge cases, and verification |
| `agents.md` | Domain-specific AI agent guidelines for this codebase |
| `.github/copilot-instructions.md` | Editor/AI rules for GitHub Copilot |
| `.claude/CLAUDE.md` | Claude Code session instructions |

---

## Rationale

### Why virtual card lifecycle?

Virtual card management hits all the spec-layer requirements naturally: issuance requires external system integration (vault), state transitions require a strict state machine (audit-integrity constraint), limits require monetary precision rules, and disputes touch encryption and fraud compliance. This gives every spec section real content — none of the non-functional requirements are hypothetical filler.

### Why this layered structure?

The spec follows the template from `specification-TEMPLATE-example.md` but reorganises it around *what an AI agent needs to not guess*. The Implementation Notes section leads with hard constraints (money types, ID semantics, idempotency order) because these are the decisions an agent gets wrong by default. The Context section is concrete (named services, named tables) so an agent has a precise workspace model rather than vague "existing systems."

### How performance targets were chosen

All targets are labeled *assumed* in the spec. The values are derived from two FinTech references:
- **Fraud response SLA**: freeze/unfreeze p99 < 200 ms is a cardholder-protection expectation — if a user spots fraud and taps "freeze," the card must be blocked before the next authorisation attempt, which can arrive within seconds.
- **Payment API benchmarks**: POST card creation at p99 < 400 ms reflects typical vault round-trip (50–150 ms) + DB write budget, which matches published latency profiles for card-issuing platforms (Marqeta, Stripe Issuing documentation).
- **Read endpoints**: p99 < 80 ms assumes Redis read-through cache with a 30-second TTL, which is standard for card detail endpoints where freshness requirements are loose.

### Why this verification depth?

Verification is stated per mid-level objective (not just globally) so a reviewer or agent can independently confirm each objective without reading the entire spec. The reconciliation checks (orphaned vault issuances, stale disputes) are included because they reflect real ops concerns in payment systems — daily batch checks are a standard FinTech control.

---

## Industry Best Practices — Traceability

| Practice | Where it appears |
|----------|-----------------|
| PAN masking (`****-****-****-LLLL`) | `specification.md` § Non-Functional → Security; `agents.md` § PCI-DSS; `.claude/CLAUDE.md` § Security Rules |
| `BigDecimal` for monetary values | `specification.md` § Implementation Notes → Money; `agents.md` § Money Handling; `.github/copilot-instructions.md` § Never Do |
| Idempotency keys on all mutations | `specification.md` § Implementation Notes → Idempotency; `agents.md` § Idempotency; `specification.md` T-4, T-8, T-11 |
| Append-only audit log | `specification.md` § Non-Functional → Audit; `agents.md` § Audit Integrity; `.claude/CLAUDE.md` § Security Rules |
| Optimistic locking for concurrency | `specification.md` § Non-Functional → Reliability; T-3, T-5; Edge Case EC-1 |
| Vault call outside DB transaction | `specification.md` § Agent Hard Rules; `agents.md` § Edge Case questions |
| GDPR right-to-erasure | `specification.md` § Non-Functional → Privacy; Edge Case table (GDPR row); `agents.md` § GDPR erasure |
| `403` on ownership mismatch (not `404`) | `specification.md` § Non-Functional → Security; `.claude/CLAUDE.md` § Security Rules |
| Circuit breaker for upstream dependency | `specification.md` § Non-Functional → Reliability |
| Structured error responses with `traceId` | `specification.md` § Implementation Notes → Error Responses; T-13; `.github/copilot-instructions.md` § FinTech-Sensitive Defaults |
| Encrypted free-text PII at rest | `specification.md` T-9; `agents.md` § Encrypted at rest; `.claude/CLAUDE.md` § Security Rules |
| Fraud-pattern rate limiting with escalation | `specification.md` § Non-Functional → Performance (rate limits); Edge Case table (fraud pattern row) |