# Copilot Instructions — Virtual Card Lifecycle Service

These instructions apply to all AI completions and suggestions in this project. They encode FinTech-specific defaults that prevent compliance and security mistakes.

---

## Naming Conventions

| Concept | Correct name | Never use |
|---------|-------------|-----------|
| Internal card identifier | `cardId` | `id`, `card_number` |
| Masked card number (last 4) | `maskedPan` | `pan`, `cardNumber`, `number` |
| Vault opaque token | `cardToken` | `token` (ambiguous), `pan` |
| Cardholder reference | `cardholderRef` | `userId`, `customerId` |
| Monetary amount | pair: `amount: BigDecimal` + `currency: String` | bare `amount`, `price`, `value` |
| Audit record | `CardEvent` | `Log`, `Record`, `History` |

---

## Always Do

- Use `BigDecimal` for all monetary arithmetic, scale 2, `HALF_UP` rounding.
- Use `ConcurrentHashMap` for any in-memory singleton state in Spring beans (tests may use simpler structures).
- Write `CardEvent` in the same `@Transactional` block as the state mutation it describes.
- Include `traceId` (from MDC) in every API error response body and in the `X-Trace-Id` response header.
- Validate ownership: confirm `card.cardholderRef == SecurityContext.cardholderRef` before any operation. Return `403` on mismatch.
- Collect all validation errors before returning — no fail-fast validators.
- Check idempotency key before executing business logic (order: rate limit → idempotency → logic).

## Never Do

- Include `cardToken`, `cvv`, `pan`, or `fullCardNumber` in any API response DTO.
- Log `cardToken`, full PAN, CVV, JWT payload, or any PII in application log lines.
- Use `Float` or `Double` for monetary values.
- Use `HashMap` in a Spring singleton — use `ConcurrentHashMap`.
- Write UPDATE or DELETE queries against the `card_events` table.
- Put business logic in a controller method — delegate to a service.
- Expose internal exception class names or stack traces in error responses.
- Add a comment explaining what the code does — names carry that; comment only the why.

---

## FinTech-Sensitive Defaults

**When generating a new service method that mutates card state:**
1. First check: is there an audit event write in the same transaction?
2. Second check: is the vault call placed outside the transaction?
3. Third check: does the method handle optimistic lock conflict (retry once, then `409`)?

**When generating a new DTO:**
- Scan for any field that could contain PAN, CVV, or card token. If found, remove it or replace with the masked equivalent.
- All monetary fields must be `BigDecimal` and paired with a `currency: String` field.

**When generating error responses:**
- Always use the structured format: `{ traceId, code, message, details[] }`.
- HTTP 500 responses log full stack trace at `ERROR` level but return only `{ traceId, code: "INTERNAL_ERROR", message: "An unexpected error occurred" }`.

**When generating tests:**
- Assert that `cardToken` is absent from every card-containing response.
- Assert masked PAN matches `^\*{4}-\*{4}-\*{4}-\d{4}$` for every transaction/card in responses.
- Include a cross-cardholder ownership test (`403`) for every endpoint that takes a `{cardId}` path variable.

---

## Patterns to Avoid

- **Sort injection**: transaction list sort order is fixed (`authorisedAt DESC`). Do not make it a query parameter.
- **PII in query parameters**: cardholder name, email, or address must never appear in a URL; use request body.
- **Silent fail on idempotency**: if Redis is unavailable, log a warning and proceed — do not throw or block the request.
- **Re-activating a terminated card**: once `TERMINATED`, the vault is authoritative. Never write code that moves a card out of `TERMINATED` state.
