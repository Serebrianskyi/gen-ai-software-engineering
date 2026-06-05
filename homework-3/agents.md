# Agent Guidelines — Virtual Card Lifecycle Service

This file governs how AI coding assistants should behave when working in this codebase. Follow these rules without deviation; they exist because violations in FinTech contexts cause compliance failures, not just bugs.

---

## Tech Stack Assumptions

- **Language**: Kotlin (JVM 21)
- **Framework**: Spring Boot 3 (Spring MVC, Spring Data JPA, Spring Security)
- **Database**: PostgreSQL 15 with Flyway migrations
- **Cache / Idempotency store**: Redis (Lettuce client)
- **Messaging**: Apache Kafka (Spring Kafka)
- **Testing**: JUnit 5, Mockito-Kotlin, MockMvc, embedded H2 (tests), embedded Redis (tests), embedded Kafka (tests)
- **Documentation**: springdoc-openapi (auto-generated Swagger UI at `/swagger-ui.html`)
- **Build**: Gradle (Kotlin DSL)

Do not introduce new dependencies without explicit instruction.

---

## Domain Rules

### PCI-DSS Compliance
- **PAN (Primary Account Number)**: Never store, log, or return a full PAN. Only the last 4 digits may appear in responses, in the format `****-****-****-LLLL`.
- **CVV/CVV2**: Never store, log, or transmit after the point of card issuance. Vault only.
- **Card token** (`cardToken`): The vault-issued opaque token. Use it only to call vault APIs. Never include it in API responses, log lines, or Kafka events.
- If in doubt about whether a field is PCI-in-scope, treat it as if it is.

### Audit Integrity
- Every state mutation (card create, freeze, unfreeze, terminate, limit change, replace, dispute) must write a `CardEvent` record **in the same DB transaction** as the state change. Never write state first and audit after; a failure between the two would leave an unaudited mutation.
- `card_events` is append-only. Never generate code that UPDATEs or DELETEs from this table.

### Money Handling
- Use `BigDecimal` for all monetary values. Never use `Double`, `Float`, or `Int` to represent currency amounts.
- Always round with `RoundingMode.HALF_UP` and scale 2.
- Always pair an amount with its ISO 4217 currency code. Never store a bare number and assume a currency.

### Idempotency
- All mutating service methods must accept an idempotency key and check it before executing business logic.
- Idempotency check order: (1) rate limit, (2) idempotency key, (3) business logic. Do not reorder.

### Error Handling
- Validators collect all errors before returning — no fail-fast. A request with 3 bad fields must report all 3.
- Never expose stack traces, SQL error messages, or internal class names in API error responses.
- All errors include a `traceId` drawn from the MDC.

---

## Code Style & Conventions

- **Controllers are thin**: HTTP mapping, header extraction, SecurityContext access, service delegation — nothing else.
- **Services are thick**: all business rules, state machine enforcement, optimistic lock retry, and audit event dispatch live here.
- **No comments on what the code does** — names carry that meaning. Add a comment only when the *why* is non-obvious (e.g., vault call outside transaction to avoid holding DB connection).
- **Thread safety**: Use `ConcurrentHashMap` only in test fixtures. Production state lives in PostgreSQL with optimistic locking.
- **Enums**: values in `SCREAMING_SNAKE_CASE` in Kotlin; serialise to `snake_case` JSON via `@JsonProperty`.
- **Nullability**: prefer non-null; use `?` only for genuinely optional fields. Avoid `!!` in production code.
- **No wildcard imports** in any file (ktlint rule).

---

## Testing Expectations

- **Service layer**: ≥ 90% line coverage. Test every state machine transition (valid and invalid), every validation branch, and every edge case in the EC table in `specification.md`.
- **Controller layer**: ≥ 70% line coverage via MockMvc integration tests.
- **Validator**: ≥ 90% coverage; test boundary values (e.g., `pageSize = 1`, `100`, `101`).
- **No mocks for in-memory state in unit tests** — use real objects when the dependency is a pure function or in-memory store.
- **Every test has `@DisplayName`** describing the scenario in plain English.
- **Tests are isolated**: each test creates its own data; no reliance on execution order.
- Assert `cardToken` absence on every response that contains a card object.

---

## Security & Compliance Constraints

- **No PII in query parameters**: cardholder name, email, and address must never appear in URLs (they end up in server logs). Use request body or path variables only.
- **Rate limit before everything**: the rate limit check must be the first thing that executes for any mutating request, before even parsing the body.
- **Encrypted at rest**: `reason` in disputes uses AES-256-GCM. Never persist plaintext sensitive free-text.
- **GDPR erasure**: when handling a GDPR delete, replace PII fields with `[REDACTED]` across all tables including audit payloads. Retain audit shell (event type, timestamps, IDs).
- **Scope enforcement**: always verify `cardholderRef` from the JWT token matches the card's owner before any operation. Return `403`, not `404`, on ownership mismatch (do not reveal that the resource exists).

---

## How the Agent Should Treat Edge Cases

Before writing any card mutation method, ask:
1. What happens if the external vault is unavailable? (Answer: vault call outside transaction; no DB write on vault failure.)
2. What happens if two requests race on the same card? (Answer: optimistic locking; retry once; then `409`.)
3. What happens if this is called twice with the same idempotency key? (Answer: return cached response.)
4. Does this operation log any sensitive field? (Answer: if yes, remove it immediately.)

When the spec and these rules conflict, follow the spec. When neither covers the case, default to the more conservative, compliant behaviour and flag the gap for review.