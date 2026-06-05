# Claude Instructions — Virtual Card Lifecycle Service

These instructions apply to all Claude sessions working in this project. They encode domain constraints and workflow expectations for this regulated FinTech context.

---

## Domain Context

This is a **virtual card lifecycle** service operating in a PCI-DSS and GDPR regulated environment. The full spec is in `specification.md`; the agent ruleset is in `agents.md`. Read both before making any code changes.

Primary constraints that override all other defaults:
1. Full PAN, CVV, and card token (`cardToken`) must never appear in API responses, log lines, or Kafka events.
2. Every card state mutation writes an audit `CardEvent` in the **same DB transaction**.
3. All monetary values use `BigDecimal` — never `Double` or `Float`.

---

## Before You Code

- Read `specification.md` § Implementation Notes and § Agent Hard Rules.
- Confirm the file you are about to touch is in the correct layer (controller = HTTP only, service = all logic).
- If the task involves a card mutation, answer these before writing: (1) Is vault call outside the transaction? (2) Is the audit event in the transaction? (3) Is the idempotency key checked first?

---

## Code Style

- Kotlin; Spring Boot 3; JVM 21.
- No wildcard imports anywhere — ktlint enforces this and the pre-commit hook will reject the commit.
- No comments explaining *what* — only *why* (non-obvious constraints, workarounds, hidden invariants).
- Controllers delegate to services immediately; zero business logic in controller methods.
- Validators return `List<FieldError>` — never throw; never fail on first error.

---

## Security Rules (Non-Negotiable)

| Rule | Why |
|------|-----|
| `cardToken` excluded from every response DTO | PCI-DSS: vault token must not leave the payment zone |
| PAN masked as `****-****-****-LLLL` only | PCI-DSS: full PAN is in-scope cardholder data |
| No PII in URL query parameters | PII in URLs ends up in server access logs |
| `403` on ownership mismatch, not `404` | `404` would reveal existence of another cardholder's resource |
| `card_events` table is append-only | Audit integrity: mutations must be irrefutable |
| `reason` in disputes encrypted at rest | GDPR: free-text submitted by user is personal data |

---

## Testing Expectations

When writing or modifying tests:
- **Assert `cardToken` absent** on every response that contains a card object — add this assertion even when not explicitly asked.
- **Assert masked PAN format** `^\*{4}-\*{4}-\*{4}-\d{4}$` on every transaction/card in responses.
- **Include a `403` test** for every endpoint with a `{cardId}` path variable (cross-cardholder access).
- **Include an invalid state transition test** for every `PATCH` that changes card state.
- Service tests: `@DisplayName` in plain English on every test method.
- Integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc`; isolated — no shared state between methods.

---

## Workflow

1. **Plan before coding**: for any task spanning more than one file, state the approach in one short paragraph and list the files to be changed before writing code.
2. **One concern per commit**: do not mix domain model changes with controller changes in a single diff.
3. **Quality gates**: before declaring a task done — `./gradlew clean build` passes, `./gradlew spotlessCheck` passes, test coverage ≥ 85% overall.
4. **Flag gaps**: if the spec does not cover an edge case encountered during implementation, surface it explicitly rather than silently choosing a behaviour.

---

## What Not to Do

- Do not add features, abstractions, or refactors beyond what the task requires.
- Do not add error handling for scenarios that cannot happen given the framework guarantees.
- Do not create new files when editing an existing one suffices.
- Do not push to remote or open a PR without explicit user confirmation.
- Do not skip the pre-commit hook (`--no-verify`) — fix the underlying issue instead.