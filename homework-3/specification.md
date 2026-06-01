# Virtual Card Lifecycle Management — Specification

> Ingest this file, implement the Low-Level Tasks, and produce code that satisfies the High and Mid-Level Objectives.

---

## High-Level Objective

Enable cardholders to self-manage virtual payment cards — creation, state control, spending limits, transaction history, replacement, and dispute intake — through a secure, auditable REST API that meets PCI-DSS and GDPR requirements without exposing full card data outside the payment vault.

---

## Mid-Level Objectives

| # | Objective | Observable outcome |
|---|-----------|-------------------|
| MO-1 | **Card Issuance** | Cardholder requests a card and receives a `cardId` + masked PAN (never the vault `cardToken`); `ISSUED` audit event recorded |
| MO-2 | **State Management** | Cardholder can freeze, unfreeze, or terminate a card; each transition produces an immutable audit event within 500 ms |
| MO-3 | **Spending Limits** | Cardholder sets per-card limits (daily / monthly / per-transaction); limits persist atomically |
| MO-4 | **Transaction History** | Paginated, time-ordered transaction list including declined authorisations; no unmasked PANs |
| MO-5 | **Card Replacement** | Old card terminated atomically, new card issued; `REPLACED` audit event links both card IDs |
| MO-6 | **Dispute Intake** | Dispute persisted with reference number, fraud-flag set on transaction, synchronous confirmation returned |

---

## Non-Functional & Policy

### Security & Privacy
- **PAN/CVV never leave the vault.** API responses use masked format `****-****-****-LLLL` only.
- OAuth 2.0 JWT required on all endpoints; scope-checked per operation (`cards:read`, `cards:write`, `disputes:write`).
- Cardholder may only access cards where `card.cardholderRef == token.cardholderRef` → else `403`.
- TLS 1.2 minimum; no plaintext fallback.
- GDPR right-to-erasure: PII fields in all records replaced with `[REDACTED]`; audit event shell (timestamps, IDs, event type) retained for regulatory purposes.
- CCPA opt-out suppresses PII from analytics event streams.

### Audit & Logging
- Every state mutation produces an **immutable** `CardEvent`: `{ eventId, cardId, cardholderRef, eventType, actorId, metadata, createdAt }`.
- Audit events are append-only — no UPDATE/DELETE on `card_events` table, enforced at DB level via trigger.
- Application logs must **never** include PAN, CVV, card token, or JWT payload. Use `cardId` (UUID) and `traceId` only.
- All events published to `card.events` Kafka topic transactionally (same DB transaction as state change).

### Performance (Assumed Targets)

All numbers below are *assumed targets* justified by FinTech UX and ops requirements.

| Operation | p50 | p99 | Rationale |
|-----------|-----|-----|-----------|
| GET card / transactions | 30 ms | 80 ms | Read-through Redis cache; instant UX expectation |
| POST create card | 150 ms | 400 ms | One vault round-trip + DB write |
| PATCH freeze / unfreeze | 60 ms | 200 ms | DB write + Kafka; no vault call — fraud response SLA |
| PATCH set limits | 80 ms | 200 ms | DB write + re-evaluation; no vault call |
| POST replace card | 200 ms | 500 ms | Two vault calls (terminate old, issue new) |
| POST dispute | 100 ms | 300 ms | DB write + fraud flag + Kafka event |

Rate limits per `cardholderRef`: 60 req/min on reads, 10 req/min on writes. Exceeded → `429` + `Retry-After`.

### Reliability
- API availability: **99.9%** monthly per-endpoint (≤ 43 min downtime/month).
- Payment vault client: circuit breaker (5 failures / 10 s → open 30 s); fallback returns `503` + `Retry-After: 30`.
- Concurrent mutations use optimistic locking (`version` column on `cards` table); conflict → retry once → `409 CONCURRENT_MODIFICATION`.

---

## Implementation Notes

### Money & Identifiers
- All monetary values: `BigDecimal` scale 2, `HALF_UP`. **Never `Float`/`Double`**. Valid range: `[0, 999_999_999.99]`. Always paired with ISO 4217 currency code.
- All IDs: UUID v4, `CHAR(36)`. `cardId` = internal identifier; `cardToken` = vault opaque token. Never conflate; never log `cardToken`.
- Timestamps: ISO 8601 UTC (`2026-01-15T10:30:00Z`). No epoch integers in responses.

### Idempotency
- `Idempotency-Key` header (UUID v4) required on all mutating requests. Stored in Redis 24 h.
- Cache key: `SHA-256("{cardholderRef}:{Idempotency-Key}")`. Body hash computed alongside.
- Same key + same body hash → cached response + `X-Idempotent-Replayed: true`.
- Same key + different body hash → `422 IDEMPOTENCY_KEY_REUSED`.
- Redis failure → log warning, proceed without idempotency (graceful degrade). Do not cache `5xx` responses.

### Error Responses
All errors: `{ "traceId": "...", "code": "SNAKE_CASE_CODE", "message": "...", "details": [{ "field": "...", "reason": "..." }] }`. `details` only present on validation errors. Never expose stack traces or SQL error messages.

HTTP codes: `400` validation, `401` unauthenticated, `403` forbidden, `404` not found, `409` conflict, `422` idempotency mismatch, `429` rate limit, `503` upstream unavailable, `500` unexpected.

### State Machine
Valid transitions only — reject others with `409 INVALID_STATE_TRANSITION`:
```
PENDING_ACTIVATION → ACTIVE
ACTIVE → FROZEN | TERMINATED
FROZEN → ACTIVE | TERMINATED
TERMINATED  (terminal — no further transitions)
```

### Agent Hard Rules
1. Never log or return full PAN, CVV, or card token in any context.
2. Write audit event in the **same DB transaction** as the state change.
3. Idempotency check must execute before business logic.
4. Rate limit check must execute before idempotency check.
5. Vault calls happen **outside** the DB transaction to avoid holding a connection during network I/O.

---

## Context

### Beginning
- **`UserService`** (external HTTP): `GET /users/{cardholderRef}` → `{ cardholderRef, name, email, kycStatus }`. KYC must be `VERIFIED` before card issuance.
- **`CardProcessorClient`**: `issueCard(cardholderRef)`, `terminateCard(cardToken)`, `getCardDetails(cardToken)`. Returns vault-issued `cardToken` + masked PAN.
- **`AuditService`**: writes `CardEvent` to PostgreSQL and publishes to Kafka.
- **PostgreSQL**: `cardholders` table exists; `cards`, `card_events`, `disputes`, `transactions` tables to be created via Flyway migrations.
- **Redis**: available for idempotency key storage and card detail cache (TTL 30 s).
- **Kafka**: `card.events` topic exists (3 partitions); `dispute.submitted` topic to be created.
- **Spring Security**: JWT filter already in place; populates `SecurityContext` with `cardholderRef` and scopes.

### Ending
- **`cards` table**: `card_id`, `cardholder_ref`, `card_token` (encrypted), `masked_pan`, `status`, `daily_limit`, `monthly_limit`, `per_tx_limit`, `currency`, `created_at`, `updated_at`, `version`.
- **`card_events` table**: append-only; `event_id`, `card_id`, `event_type`, `actor_id`, `metadata` (JSONB, PII-sanitised on GDPR erase), `created_at`.
- **`disputes` table**: `dispute_id`, `card_id`, `transaction_id`, `reason` (AES-256-GCM encrypted), `status`, `created_at`.
- REST endpoints covering all 6 objectives; Swagger UI auto-generated via `springdoc-openapi`.
- Unit + integration test suite; coverage ≥ 85% overall.

---

## Low-Level Tasks

### T-1 — Domain Models & Enums

What prompt would you run to complete this task?
> Create Kotlin data classes and enums for the virtual card domain: `VirtualCard`, `CardStatus`, `CardEvent`, `CardEventType`, `Dispute`, `DisputeStatus`.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/model/VirtualCard.kt`
> `src/main/kotlin/com/banking/virtualcard/model/CardEvent.kt`
> `src/main/kotlin/com/banking/virtualcard/model/Dispute.kt`

What function do you want to CREATE or UPDATE?
> `data class VirtualCard`, `enum class CardStatus`, `data class CardEvent`, `enum class CardEventType`, `data class Dispute`, `enum class DisputeStatus`

What are details you want to add to drive the code changes?
> `VirtualCard` fields: `cardId` (UUID), `cardholderRef`, `cardToken` (never serialised), `maskedPan` (`****-****-****-LLLL`), `status`, `dailyLimit`/`monthlyLimit`/`perTxLimit` (all `BigDecimal?`), `currency` (ISO 4217), `createdAt`, `updatedAt`, `version` (Long). `CardStatus`: `PENDING_ACTIVATION`, `ACTIVE`, `FROZEN`, `TERMINATED`. `CardEventType`: `ISSUED`, `ACTIVATED`, `FROZEN`, `UNFROZEN`, `LIMIT_CHANGED`, `REPLACED`, `TERMINATED`, `DISPUTE_SUBMITTED`. All enums serialise to `snake_case` JSON via `@JsonProperty`. `cardToken` has no `@JsonProperty` — must be absent from all serialised output. *Serves: MO-1–6.*

---

### T-2 — Request / Response DTOs

What prompt would you run to complete this task?
> Create Kotlin data classes for all API request and response DTOs. Ensure masked PAN is the only card number field in any response DTO.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/dto/CardDtos.kt`
> `src/main/kotlin/com/banking/virtualcard/dto/DisputeDtos.kt`

What function do you want to CREATE or UPDATE?
> `data class CardCreateRequest`, `data class CardResponse`, `data class CardStateChangeRequest`, `data class CardLimitUpdateRequest`, `data class TransactionResponse`, `data class TransactionListResponse`, `data class DisputeCreateRequest`, `data class DisputeResponse`

What are details you want to add to drive the code changes?
> `CardResponse`: masked PAN only — no `cardToken` field. `CardLimitUpdateRequest`: at least one of `dailyLimit`/`monthlyLimit`/`perTxLimit` must be non-null; custom validator rejects all-null input. `TransactionListResponse` includes `totalCount`, `page`, `pageSize`. `DisputeCreateRequest`: `reason` 10–2000 chars. All monetary fields paired with `currency: String`. No DTO may contain a field named `cardToken`, `cvv`, `pan`, or `fullCardNumber`. *Serves: MO-1–6.*

---

### T-3 — JPA Entities & Repositories

What prompt would you run to complete this task?
> Create Spring Data JPA entities and repositories for cards and disputes, with optimistic locking on the cards table.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/entity/CardEntity.kt`
> `src/main/kotlin/com/banking/virtualcard/repository/CardRepository.kt`
> `src/main/kotlin/com/banking/virtualcard/repository/DisputeRepository.kt`

What function do you want to CREATE or UPDATE?
> `class CardEntity`, `interface CardRepository : JpaRepository`, `interface DisputeRepository : JpaRepository`

What are details you want to add to drive the code changes?
> `CardEntity`: `@Version` on `version` column for optimistic locking; `card_token` column excluded from `toString`. `CardRepository` custom queries: `findByCardholderRefAndStatus(cardholderRef, status)`, `countActiveCardsByCardholderRef(cardholderRef)` — enforces max 10 active cards. `DisputeRepository`: `findByCardIdAndTransactionIdAndStatusNotIn(cardId, transactionId, statuses)` for duplicate detection. Optimistic lock conflict caught in service layer and re-thrown as `ConflictException`. *Serves: MO-1, MO-2, MO-3, MO-5.*

---

### T-4 — Card Issuance Service (MO-1)

What prompt would you run to complete this task?
> Implement `CardIssuanceService.issueCard` that validates cardholder KYC, calls the vault outside the DB transaction, then persists the card and writes an audit event in a single transaction.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/CardIssuanceService.kt`

What function do you want to CREATE or UPDATE?
> `fun issueCard(request: CardCreateRequest, idempotencyKey: String): CardResponse`

What are details you want to add to drive the code changes?
> Steps in order: (1) assert KYC `VERIFIED` via `UserServiceClient` — throw `ValidationException(KYC_NOT_VERIFIED)` otherwise; (2) assert < 10 active cards — throw `ValidationException(MAX_CARDS_REACHED)` otherwise; (3) check idempotency key in Redis — return cached response on hit; (4) call `CardProcessorClient.issueCard()` **outside** `@Transactional`; (5) persist `CardEntity` + write `ISSUED` `CardEvent` in one transaction; (6) publish Kafka event (best-effort — not in critical path). Vault failure must leave no `CardEntity` persisted. *Serves: MO-1.*

---

### T-5 — Card State Management (MO-2)

What prompt would you run to complete this task?
> Implement `CardStateService` with freeze, unfreeze, and terminate operations that enforce the state machine and write audit events in the same transaction as the state change.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/CardStateService.kt`

What function do you want to CREATE or UPDATE?
> `fun freeze(cardId: UUID, request: CardStateChangeRequest): CardResponse`
> `fun unfreeze(cardId: UUID, request: CardStateChangeRequest): CardResponse`
> `fun terminate(cardId: UUID, request: CardStateChangeRequest): CardResponse`

What are details you want to add to drive the code changes?
> Each method: verify `cardholderRef` ownership, validate transition against the state machine (invalid → `ConflictException(INVALID_STATE_TRANSITION)`), apply optimistic lock retry once on conflict then throw `ConflictException(CONCURRENT_MODIFICATION)`. `terminate` calls `CardProcessorClient.terminateCard()` after DB update; vault failure → rollback DB update. Audit event type must match the operation exactly: `FROZEN`, `UNFROZEN`, `TERMINATED`. Freeze/unfreeze target p99 < 200 ms. *Serves: MO-2.*

---

### T-6 — Spending Limits (MO-3)

What prompt would you run to complete this task?
> Implement `CardLimitService.updateLimits` that atomically updates per-card spending limits and records a structured audit event listing only the changed fields.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/CardLimitService.kt`

What function do you want to CREATE or UPDATE?
> `fun updateLimits(cardId: UUID, request: CardLimitUpdateRequest): CardResponse`

What are details you want to add to drive the code changes?
> Only `ACTIVE` or `FROZEN` cards allowed; `TERMINATED` → `ConflictException`. Each provided value must be in `[0, 999_999_999.99]`; 0 is valid (blocks that transaction type). Write `LIMIT_CHANGED` audit event with `metadata` containing `changedFields`, `previousValues`, `newValues` — **do not** log actual amounts in application log lines, only in the structured event. A limit below today's spend is allowed — it applies to future authorisations only. *Serves: MO-3.*

---

### T-7 — Transaction History (MO-4)

What prompt would you run to complete this task?
> Implement `TransactionQueryService.getTransactions` with ownership check, fixed sort order, and pagination constraints.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/TransactionQueryService.kt`
> `src/main/kotlin/com/banking/virtualcard/repository/TransactionRepository.kt`

What function do you want to CREATE or UPDATE?
> `fun getTransactions(cardId: UUID, page: Int, pageSize: Int, filters: TransactionFilters): TransactionListResponse`

What are details you want to add to drive the code changes?
> Verify `cardholderRef` ownership — return `ForbiddenException` (not empty list) on mismatch. `pageSize` capped at 100, default 20; `pageSize > 100` → `ValidationException`. Sort fixed to `authorisedAt DESC` — not a query parameter (prevents sort-injection data leakage). Date range filter max 366 days; range exceeded → `ValidationException(DATE_RANGE_TOO_LARGE)`. Every `maskedPan` in the response must match `^\*{4}-\*{4}-\*{4}-\d{4}$`. Response always includes `totalCount` even when 0. *Serves: MO-4.*

---

### T-8 — Card Replacement (MO-5)

What prompt would you run to complete this task?
> Implement `CardReplacementService.replaceCard` that atomically terminates the old card and issues a new one, with compensation handling if vault issuance fails.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/CardReplacementService.kt`

What function do you want to CREATE or UPDATE?
> `fun replaceCard(cardId: UUID, idempotencyKey: String): CardResponse`

What are details you want to add to drive the code changes?
> Steps: (1) terminate old card + write `TERMINATED` event in one transaction; (2) call vault to issue new card **outside** transaction; (3) persist new `CardEntity` + write `REPLACED` event with `metadata.replacedCardId = <old cardId>`. If vault fails after step 1, create a compensation record for ops — do **not** re-activate the old card (vault is authoritative). Idempotent: same `Idempotency-Key` must return the same new `cardId`. *Serves: MO-5.*

---

### T-9 — Dispute Intake (MO-6)

What prompt would you run to complete this task?
> Implement `DisputeService.submitDispute` that validates uniqueness, encrypts the reason, writes an audit event, and publishes a Kafka event for the fraud team.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/service/DisputeService.kt`

What function do you want to CREATE or UPDATE?
> `fun submitDispute(cardId: UUID, request: DisputeCreateRequest): DisputeResponse`

What are details you want to add to drive the code changes?
> Validate: `transactionId` belongs to `cardId`; no open or resolved dispute exists for that transaction — `ConflictException(DISPUTE_ALREADY_EXISTS)` otherwise. Encrypt `reason` with AES-256-GCM before persistence. Write `DISPUTE_SUBMITTED` audit event with `metadata` containing `disputeId` only — **no plaintext reason** in the event. Publish `dispute.submitted` Kafka event. `reason` must never be returned in any API response after submission (write-only field). *Serves: MO-6.*

---

### T-10 — REST Controllers

What prompt would you run to complete this task?
> Create thin Spring MVC controllers for card and dispute operations. All business logic stays in services; controllers only handle HTTP mapping and header extraction.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/controller/CardController.kt`
> `src/main/kotlin/com/banking/virtualcard/controller/DisputeController.kt`

What function do you want to CREATE or UPDATE?
> `CardController`: `createCard`, `getCard`, `freezeCard`, `unfreezeCard`, `terminateCard`, `updateLimits`, `getTransactions`, `replaceCard`
> `DisputeController`: `submitDispute`, `getDispute`

What are details you want to add to drive the code changes?
> Extract `Idempotency-Key` header on all mutating requests; missing header → `400 IDEMPOTENCY_KEY_REQUIRED`. Inject `traceId` from MDC into `X-Trace-Id` response header on every response. Read `cardholderRef` from `SecurityContext` (JWT already validated by Spring Security filter). Endpoint-to-scope mapping: reads → `cards:read`, mutations → `cards:write`, disputes → `disputes:write` / `disputes:read`. No business logic in any controller method. *Serves: MO-1–6.*

---

### T-11 — Idempotency Filter & Exception Handler

What prompt would you run to complete this task?
> Implement a `OncePerRequestFilter` for idempotency and a `@RestControllerAdvice` that maps all domain exceptions to structured HTTP error responses.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/filter/IdempotencyFilter.kt`
> `src/main/kotlin/com/banking/virtualcard/exception/GlobalExceptionHandler.kt`

What function do you want to CREATE or UPDATE?
> `class IdempotencyFilter : OncePerRequestFilter`, `class GlobalExceptionHandler` with `@ExceptionHandler` methods per domain exception

What are details you want to add to drive the code changes?
> Filter: compute `key = SHA-256("{cardholderRef}:{Idempotency-Key}")` and `bodyHash`. Redis hit + matching hash → return cached response with `X-Idempotent-Replayed: true`. Redis hit + different hash → `422 IDEMPOTENCY_KEY_REUSED`. Redis failure → log warning, proceed (graceful degrade). Never cache `5xx` responses. Handler mappings: `ValidationException` → `400`, `ForbiddenException` → `403`, `NotFoundException` → `404`, `ConflictException` → `409`, `RateLimitException` → `429` + `Retry-After`, `UpstreamUnavailableException` → `503` + `Retry-After: 30`, unhandled → `500` (generic message only — full trace logged at `ERROR` with `traceId`). All error bodies: `{ traceId, code, message, details[] }`. No internal class names in any response body. *Serves: all tasks.*

---

### T-12 — Input Validator

What prompt would you run to complete this task?
> Create a `CardRequestValidator` that collects all field errors before returning — no fail-fast. Return a list of structured `FieldError` objects.

What file do you want to CREATE or UPDATE?
> `src/main/kotlin/com/banking/virtualcard/validator/CardRequestValidator.kt`

What function do you want to CREATE or UPDATE?
> `fun validateCreateRequest(request: CardCreateRequest): List<FieldError>`
> `fun validateLimitRequest(request: CardLimitUpdateRequest): List<FieldError>`
> `fun validateDisputeRequest(request: DisputeCreateRequest): List<FieldError>`
> `fun validatePagination(page: Int, pageSize: Int): List<FieldError>`

What are details you want to add to drive the code changes?
> `FieldError(field: String, code: String, message: String)`. Validate: ISO 4217 currency (3-letter uppercase), limit values in `[0, 999_999_999.99]`, `reason` length 10–2000, `transactionId` UUID format, `pageSize` in `[1, 100]`, date range ≤ 366 days. Named constants for each error code (e.g., `INVALID_CURRENCY`, `LIMIT_OUT_OF_RANGE`). No DB or service calls inside the validator. A request with 3 bad fields must return all 3 errors in `details[]`. *Serves: all tasks.*

---

### T-13 — Unit Tests

What prompt would you run to complete this task?
> Write unit tests for all service classes and the validator covering happy path, all validation branches, every state machine transition, and idempotency replay scenarios.

What file do you want to CREATE or UPDATE?
> `src/test/kotlin/com/banking/virtualcard/service/CardIssuanceServiceTest.kt`
> `src/test/kotlin/com/banking/virtualcard/service/CardStateServiceTest.kt`
> `src/test/kotlin/com/banking/virtualcard/service/CardLimitServiceTest.kt`
> `src/test/kotlin/com/banking/virtualcard/service/DisputeServiceTest.kt`
> `src/test/kotlin/com/banking/virtualcard/validator/CardRequestValidatorTest.kt`

What function do you want to CREATE or UPDATE?
> One test class per source class; `@DisplayName` on each class and each test method.

What are details you want to add to drive the code changes?
> Cover: every valid state machine transition + every invalid one, KYC fail, max-card cap, vault timeout (no entity persisted), idempotency replay (same key + same body → cached; same key + different body → `422`), boundary values for `pageSize` (1, 100, 101) and limits (0, max, max+1), duplicate dispute rejection, GDPR erasure path. Service coverage target ≥ 90%. Validator coverage ≥ 90%. Each test `@DisplayName` in plain English. *Serves: T-4 through T-12.*

---

### T-14 — Integration Tests

What prompt would you run to complete this task?
> Write MockMvc integration tests for all REST endpoints covering happy path, ownership violations, state conflicts, rate limiting, and idempotency replay.

What file do you want to CREATE or UPDATE?
> `src/test/kotlin/com/banking/virtualcard/controller/CardControllerIntegrationTest.kt`
> `src/test/kotlin/com/banking/virtualcard/controller/DisputeControllerIntegrationTest.kt`

What function do you want to CREATE or UPDATE?
> Test classes with `@SpringBootTest` + `@AutoConfigureMockMvc`; one test method per scenario.

What are details you want to add to drive the code changes?
> Test scenarios: happy path per endpoint, `403` cross-cardholder access on every `{cardId}` endpoint, `409` for each invalid state transition, `429` after 11th mutating request in 1 minute, `X-Idempotent-Replayed: true` on duplicate idempotency key. Assert `cardToken` absent from every card object in every response body. Assert masked PAN regex `^\*{4}-\*{4}-\*{4}-\d{4}$` on every transaction in responses. All tests isolated — no shared state between methods. Controller coverage ≥ 70%. *Serves: T-10, T-11.*

---

## Edge Cases & Failure Modes

| Scenario | Expected behaviour |
|----------|--------------------|
| Concurrent freeze + unfreeze on same card | Second request → `409 CONCURRENT_MODIFICATION`; both attempts in audit log |
| Limit set to 0 | Valid — all future authorisations declined at processor; `LIMIT_CHANGED` event recorded |
| Dispute on already-disputed transaction | `409 DISPUTE_ALREADY_EXISTS`; no duplicate record created |
| Replacement of a frozen card | Allowed; old → `TERMINATED`, new → `ACTIVE` |
| Vault timeout during `issueCard` | No `CardEntity` persisted; `503` returned; no audit event written |
| Vault timeout during `terminate` | DB rollback; card stays in previous state; `503` returned |
| Vault timeout during `replaceCard` (after old terminated) | Old stays `TERMINATED`; compensation record created for ops; no new card |
| Idempotency key reused with different body | `422 IDEMPOTENCY_KEY_REUSED`; attempt logged with `cardholderRef` + `traceId` |
| Cardholder at 10-card limit requests new card | `400 MAX_CARDS_REACHED`; no vault call made |
| KYC status not `VERIFIED` | `400 KYC_NOT_VERIFIED`; no vault call made |
| GDPR erasure request | PII → `[REDACTED]` in all records; audit event shell retained; erasure event published to Kafka |
| Date range > 366 days | `400 DATE_RANGE_TOO_LARGE`; no DB query executed |
| 10+ card creates in 1 minute (fraud pattern) | `429` from rate limiter; fraud-team Kafka event published on 5th+ attempt in window |
| Stale read after limit update | Redis TTL 30 s; cardholder may see old limit up to 30 s — documented, not a correctness bug |

---

## Verification

| Objective | How to verify |
|-----------|--------------|
| MO-1 | Unit: vault mock → `CardEntity` `PENDING_ACTIVATION` + `ISSUED` event. Integration: `POST /cards` → `201`, masked PAN in body, `cardToken` absent. |
| MO-2 | Unit: all 5 valid transitions + all invalid transitions. Integration: freeze active → `200`; freeze frozen → `409`. Audit event type matches operation. |
| MO-3 | Unit: boundary values 0, max, max+1. Integration: no-field request → `400`; valid request → `200`. |
| MO-4 | Integration: cross-cardholder → `403`. `pageSize=101` → `400`. Masked PAN regex on every transaction in response. |
| MO-5 | Unit: vault failure after old card terminated → compensation record exists, no new card. Integration: idempotent call → same `cardId`. |
| MO-6 | Unit: duplicate dispute → `409`. Integration: `201` with `disputeId`; `reason` absent from response. Kafka event via embedded Kafka. |

**Reconciliation checks** (daily jobs):
- Count `ISSUED` events vs cards not in `PENDING_ACTIVATION` — gap indicates orphaned vault issuances for ops review.
- Count `SUBMITTED` disputes older than 30 days — escalation trigger for ops team.