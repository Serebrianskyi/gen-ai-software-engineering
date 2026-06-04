# PR: Draft Implementation with AI Review Findings

## Overview

This PR demonstrates **specification-driven development in action**: We commit intentionally flawed code that violates the `specification.md`, then use AI code review to catch those violations automatically.

**Purpose**: Show how the `specification.md`, `agents.md`, and AI guardrails work together to enforce complex financial constraints that humans would miss.

**Status**: 🔴 **INTENTIONAL VIOLATIONS** — This is *not* a production commit; all findings must be fixed before merge.

---

## What's in This PR

### New Files (Draft Implementation)

```
homework-3/src/main/kotlin/com/fintech/card/
├── controller/CardController.kt      — REST endpoints with violations
├── service/CardService.kt            — Business logic with violations
├── model/Card.kt                     — Entity model with violations
├── repository/CardEventRepository.kt — Audit log repository
└── vault/PaymentVaultClient.kt       — Vault integration with violations
```

### Review Artifact

**`REVIEW-FINDINGS.md`** — Detailed AI code review output with 15 findings:

| Finding | Severity | Category |
|---------|----------|----------|
| F-1: Float instead of BigDecimal | 🔴 CRITICAL | Monetary precision |
| F-2: Vault call inside transaction | 🔴 CRITICAL | Connection pooling |
| F-3: Full PAN in logs | 🔴 CRITICAL | PCI-DSS compliance |
| F-4: Missing idempotency keys | 🔴 CRITICAL | Duplicate prevention |
| F-5: No optimistic locking | 🔴 CRITICAL | Race conditions |
| F-6: No audit event in transaction | 🔴 CRITICAL | Audit integrity |
| F-7–F-15 | 🟠 HIGH | Various |

---

## Key Violations Demonstrated

### 1. Monetary Arithmetic (F-1)

**The Violation**:
```kotlin
// CardService.kt, Card.kt, CardController.kt
var dailyLimit: Double = 0.0  // ← Floating-point error machine
```

**Why It's Wrong**:
- IEEE 754 floating-point cannot represent 0.1 precisely
- Across 1000 transactions: `0.1 + 0.2 + ... + 0.1` ≠ `100.0`
- Reconciliation fails; audit trail is unrecoverable

**Specification Rule**:
> All monetary values: `BigDecimal` scale 2, `HALF_UP`. **Never Float/Double**.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] AI detected floating-point usage on line 12, Card.kt
- [x] Violation traced through 3 files: CardService, Card, CardController
- [x] Root cause: No BigDecimal import; API accepts Double in request

**What an AI Agent Would Say**:
```
⚠️ F-1: Floating-Point Arithmetic for Monetary Values
   File: Card.kt:12-13
   Severity: CRITICAL
   
   Floating-point arithmetic is unsuitable for monetary values.
   This violates § Implementation Notes → Money in the specification.
   
   IEEE 754 represents 0.1 as 0.1000000000000000055511...
   Across 1M transactions, accumulated error reaches $1.00+
   
   Fix: Replace Double with BigDecimal(scale=2, RoundingMode.HALF_UP)
```

---

### 2. Vault Call Inside Transaction (F-2)

**The Violation**:
```kotlin
// CardService.kt:26-36
fun issueCard(cardholderRef: String): Card {
    val newCard = Card(...)
    
    val vaultResponse = vaultClient.createCard(cardholderRef)  // ← 100 ms I/O
    newCard.cardToken = vaultResponse.token
    
    return cardRepository.save(newCard)  // ← Still holding DB connection
}
```

**Why It's Wrong**:
- Each vault call takes 50–150 ms
- During this time, DB connection is held
- With 10-connection pool: 10 requests × 100 ms = 1 second for entire pool to exhaust
- At 100 req/s: connection pool exhausted in <100 ms
- API becomes unresponsive; cascading failure to dependent services

**Specification Rule**:
> Vault calls happen **outside** the DB transaction to avoid holding a connection during network I/O.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] Network I/O detected inside `@Transactional` scope
- [x] vaultClient call at line 33 blocks connection pool
- [x] No retry or timeout configuration visible

**What an AI Agent Would Say**:
```
⚠️ F-2: Vault Call Inside Database Transaction
   File: CardService.kt:26-36
   Severity: CRITICAL
   
   You're calling vaultClient.createCard() while holding a DB transaction.
   This will exhaust the connection pool under load.
   
   Timeline:
   - 10 requests arrive simultaneously
   - Each acquires DB connection
   - Each waits 100 ms for vault
   - 10 connections held for 100 ms
   - 11th request queues indefinitely
   - Connection pool starved; API hangs
   
   Fix: Call vault BEFORE opening transaction. Save card inside transaction.
```

---

### 3. Full PAN in Logs (F-3)

**The Violation**:
```kotlin
// PaymentVaultClient.kt:22-23
logger.info("Card issued: PAN=${response.pan}, token=${response.token}")

// CardService.kt:44-46
logger.info("Fetching card $cardId with PAN")
```

**Why It's Wrong**:
- PAN goes to log aggregation (CloudWatch, Datadog, Splunk)
- Logs retained 90 days in S3 backups
- If log service is compromised: 1000s of PANs exposed
- **PCI-DSS § 3.4 violation**: "Rendering PAN Unreadable"
- **Fine**: $10,000/month per PCI compliance framework

**Specification Rule**:
> Application logs must **never** include PAN, CVV, card token, or JWT payload. Use `cardId` (UUID) and `traceId` only.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] String interpolation with `response.pan` detected
- [x] Log message references "PAN" (semantic violation)
- [x] Vault response contains full PAN (not masked)

**What an AI Agent Would Say**:
```
🚨 F-3: Full PAN Exposed in Logs (PCI-DSS Violation)
   File: PaymentVaultClient.kt:22-23
   Severity: CRITICAL
   
   You're logging the full PAN from vault response.
   This exposes cardholder data to log aggregation systems.
   
   Risk:
   - Log server compromise → 1000s of PANs exposed
   - Backup in S3 → 90 days of PAN history accessible
   - PCI audit → automatic FAIL + $10k/month fine
   
   The spec says:
   "PAN/CVV never leave the vault. Logs must never include PAN, CVV,
    card token, or JWT. Use cardId (UUID) and traceId only."
   
   Fix: Log only cardId and traceId. Request panMasked (not pan) from vault.
```

---

### 4. Missing Idempotency Key Validation (F-4)

**The Violation**:
```kotlin
// CardController.kt:76
@RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?

// Lines 78–88: No idempotency cache check
fun replaceCard(...) {
    return try {
        val newCard = cardService.replaceCard(cardId)  // ← Idempotency?
        ResponseEntity.ok(newCard)
    }
}
```

**Why It's Wrong**:
- Client POST times out (network flake)
- Retry sends same request with same data
- Second request creates second card
- Ledger imbalance: 2 cards where customer expects 1
- Refund dispute: "I only requested one card"

**Specification Rule**:
> `Idempotency-Key` header (UUID v4) required on all mutating requests. Stored in Redis 24 h.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] Idempotency-Key marked as `required = false`
- [x] No cache lookup before vault call (lines 78–88)
- [x] Response never includes `X-Idempotent-Replayed` header

**What an AI Agent Would Say**:
```
⚠️ F-4: Missing Idempotency Key Validation
   File: CardController.kt:76-88
   Severity: CRITICAL
   
   You've made Idempotency-Key optional and don't check it before
   calling cardService.replaceCard(). This enables duplicate issuances.
   
   Failure scenario:
   1. Client POST /replace with body {"cardId": "123"}
   2. Network times out; client doesn't know if request succeeded
   3. Client retries with same body
   4. Server creates SECOND replacement card (idempotency key not checked)
   5. Ledger imbalance; customer sees 2 active cards
   
   Stripe, Marqeta, and all payment processors require idempotency keys.
   
   Fix:
   1. Make header required: @RequestHeader("Idempotency-Key") (no default)
   2. Check Redis cache before vault call
   3. Cache response for 24 hours
   4. Return 422 if same key + different body
```

---

### 5. Missing Optimistic Locking (F-5)

**The Violation**:
```kotlin
// CardService.kt:56-68
fun freezeCard(cardId: String): Card {
    val card = cardRepository.findById(cardId)
    
    if (card.state != "ACTIVE") {
        throw IllegalStateException("Cannot freeze card in ${card.state} state")
    }
    
    card.state = "FROZEN"
    return cardRepository.save(card)  // ← No version check; race condition possible
}

// Card.kt: No version field
```

**Why It's Wrong**:
- Thread A: reads card, version=1, state=ACTIVE
- Thread B: reads card, version=1, state=ACTIVE
- Thread A: saves card, state=FROZEN, version → 2
- Thread B: saves card, state=ACTIVE (overwrites Thread A!) version → 2
- **Result**: Card is ACTIVE; fraud succeeds; freeze didn't work

**Specification Rule**:
> Concurrent mutations use optimistic locking (`version` column on `cards` table); conflict → retry once → `409 CONCURRENT_MODIFICATION`.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] No version field in Card.kt
- [x] No version check in cardRepository.save()
- [x] cardRepository interface extends CrudRepository (no optimistic locking config)

**What an AI Agent Would Say**:
```
⚠️ F-5: Missing Optimistic Locking (Race Condition)
   File: CardService.kt:56-68, Card.kt
   Severity: CRITICAL
   
   Concurrent freeze + spend requests can race on card state.
   
   Timeline:
   T=0: Cardholder reports fraud; app calls freeze
   T=0: Auth request for $100 arrives (already in flight)
   T=0.1: Thread A (freeze) reads card: version=1, state=ACTIVE
   T=0.2: Thread B (spend) reads card: version=1, state=ACTIVE
   T=0.3: Thread A saves: state=FROZEN, version → 2
   T=0.4: Thread B saves: state=ACTIVE (overwrites!), version → 2
   T=0.5: Card in DB: state=ACTIVE
   
   Result: Freeze failed silently. Fraud charge goes through.
   Spec violation: Freeze must complete with p99 < 200 ms guarantee.
   
   Fix: Add version column to Card. Check version on update.
   On conflict, retry once. If still conflicts, return 409.
```

---

### 6. No Audit Event in Same Transaction (F-6)

**The Violation**:
```kotlin
// CardService.kt:39-41
val saved = cardRepository.save(newCard)

// ← Audit event created here
// ← If this fails, transaction is orphaned

return saved
```

**Why It's Wrong**:
- Card is saved to DB
- Network timeout on audit event creation
- Database has card; vault has card; but audit log is missing
- Daily reconciliation finds orphaned card (spec § Edge Case EC-2)
- But card already issued; ledger is split-brain
- Unrecoverable inconsistency

**Specification Rule**:
> Write audit event in the **same DB transaction** as the state change.

**AI Review Finding**: ✅ **CONFIRMED**
- [x] cardRepository.save() executed before audit event creation
- [x] No @Transactional scope visible
- [x] No try-catch for audit event failure
- [x] Orphaned card possible if next operation fails

**What an AI Agent Would Say**:
```
⚠️ F-6: No Audit Event in Same Transaction
   File: CardService.kt:39-41
   Severity: CRITICAL
   
   You're creating the audit event AFTER saving the card.
   If audit event creation fails, you have an orphaned card.
   
   Scenario:
   1. cardRepository.save(newCard) succeeds → DB has card
   2. eventRepository.save(event) times out → network error
   3. Exception is thrown; method returns nothing
   4. Client doesn't know if card was issued
   5. Client retries with same Idempotency-Key
   6. Cache miss (no response stored) → second card issued
   
   Audit trail is the source of truth. If the state change commits
   but the audit event doesn't, the system state is unrecoverable.
   
   Fix:
   @Transactional
   fun issueCard() {
     // 1. Call vault BEFORE transaction
     // 2. Save card AND audit event in SAME transaction
     // 3. If anything fails, ENTIRE transaction rolls back
   }
```

---

## How This Demonstrates Specification-Driven Design

### The Workflow

1. **Specification First** (`specification.md`)
   - Define high-level, mid-level, low-level objectives
   - State non-functional requirements explicitly (PCI-DSS, GDPR, performance)
   - Write implementation notes with hard rules

2. **Agent Guidelines** (`agents.md`)
   - Extract domain-specific rules from specification
   - Write them as guardrails for AI agents (Claude, Copilot)
   - Name constraints explicitly: "Never use Float for money"

3. **Draft Implementation** (This PR)
   - Write intentionally flawed code
   - Demonstrate what violations look like

4. **AI Code Review** (`REVIEW-FINDINGS.md`)
   - AI reads specification
   - AI analyzes code against spec
   - AI produces detailed findings with fixes

5. **Corrected Implementation** (Next PR)
   - Fix all findings
   - Reference specification rules in commit messages
   - Verify each mid-level objective

### What Makes This Work

The `specification.md` is written for AI agents, not just humans:

- **Explicit constraints** (not vague): "BigDecimal scale 2, HALF_UP" not "use proper money types"
- **Named systems** (not abstract): `card_events` table, `PaymentVault` service, Kafka `card.events` topic
- **Separated concerns**: Implementation notes, edge cases, and verification are distinct sections
- **Traceability**: Every best practice maps to specification sections (see README § Industry Best Practices)

This is why AI can catch violations automatically — the specification is precise enough to be machine-readable.

---

## Files & Screenshots

### Review Findings Document
📄 **`REVIEW-FINDINGS.md`** — Full details of 15 findings:
- Each finding has: file, line, severity, specification quote, failure scenario, AI review output, code fix
- Organized by severity (critical first)
- Includes remediation priority phases

### What Screenshots Would Show

**Screenshot 1: Claude Code session reading specification**
```
[Claude Code opens repo]
Session instructions loaded: .claude/CLAUDE.md
├─ Rule 1: Never log PAN
├─ Rule 2: Audit events in same transaction
├─ Rule 3: BigDecimal for money
├─ Rule 4: Vault calls outside transaction
└─ ...
```

**Screenshot 2: AI catching PAN in logs**
```
CardService.kt:44-46

AI: "⚠️ PCI-DSS violation detected
   You're logging PAN directly.
   
   Spec § Security says:
   'Application logs must never include PAN, CVV, card token'
   
   Fix: Use cardId (UUID) and traceId only."
```

**Screenshot 3: AI catching float arithmetic**
```
Card.kt:12-13

AI: "⚠️ F-1: Floating-Point Arithmetic for Monetary Values
   var dailyLimit: Double = 0.0
   
   Specification § Implementation Notes → Money:
   'All monetary values: BigDecimal scale 2, HALF_UP.
    Never Float/Double.'
   
   Why: IEEE 754 loses precision. Across 1M transactions,
   accumulated error = unreconcilable $1.00+"
```

**Screenshot 4: AI catching missing idempotency**
```
CardController.kt:76

AI: "⚠️ F-4: Missing Idempotency Key Validation
   @RequestHeader("Idempotency-Key", required = false) ← Should be required
   
   No cache lookup before vault call.
   This enables duplicate card issuances.
   
   Stripe, Marqeta, and all payment processors require this."
```

---

## Verification Checklist

- [x] Draft implementation commits violations of specification.md
- [x] REVIEW-FINDINGS.md documents 15 findings with AI analysis
- [x] Each finding shows: file, line, severity, spec rule, failure scenario, code fix
- [x] Findings are categorized: Security/Compliance, Spec Violation, Reliability
- [x] Fixes are provided for all findings (see REVIEW-FINDINGS.md)
- [ ] (Next PR) All findings are fixed
- [ ] (Next PR) Commit messages reference specification rules
- [ ] (Next PR) Each mid-level objective (MO-1 through MO-6) has test coverage
- [ ] (Next PR) All edge cases (EC-1 through EC-15) are addressed
- [ ] (Next PR) Security audit: PCI-DSS compliance verified

---

## Related Files

- **`specification.md`** — Full specification with hard rules, non-functional requirements, edge cases
- **`agents.md`** — AI agent guidelines extracted from specification
- **`.claude/CLAUDE.md`** — Claude Code session instructions
- **`.github/copilot-instructions.md`** — GitHub Copilot editor rules
- **`REVIEW-FINDINGS.md`** — This PR's detailed code review findings (AI output)

---

## Why This Matters

This PR demonstrates that **specification-driven design prevents bugs that code review alone would miss**:

### Without Specification
- Code review asks: "Does this look right?"
- Answer: "Sure, looks fine." ← Float is a valid number type in Kotlin
- Result: Bug ships; financial reconciliation fails 6 months later

### With Specification + AI Guardrails
- Code review asks: "Is this compliant with specification?"
- AI reads: "§ Implementation Notes → Money: 'BigDecimal scale 2, HALF_UP. Never Float.'"
- AI says: "⚠️ F-1: This violates the monetary precision requirement"
- Result: Violation caught before commit; fix is explicit in the spec

### For Financial Software
Violations of regulatory constraints (PCI-DSS, GDPR) are **not optional**. A specification that AI agents understand is the difference between:
- ✅ Automatic compliance checking (fast, cheap, repeatable)
- ❌ Manual audit 6 months later (expensive, damaging, reactive)

---

## Next Steps

1. **Review findings**: Read `REVIEW-FINDINGS.md` to understand the violations and fixes
2. **Implement fixes** (next PR): Apply all recommended fixes from findings F-1 through F-15
3. **Reference specification**: Each commit should cite the spec rule it fixes
4. **Verify objectives**: Ensure all mid-level objectives (MO-1–MO-6) pass
5. **Test edge cases**: Implement test fixtures for all 15 edge cases (EC-1–EC-15)
6. **Security audit**: Verify no plaintext PII, PAN always masked, audit events append-only

---

## Statistics

| Metric | Value |
|--------|-------|
| New Files | 5 (controller, service, model, repository, vault client) |
| Total SLOC | ~284 |
| Violations | 15 (6 critical, 9 high) |
| Categories | Security/Compliance (5), Specification (7), Reliability (3) |
| Findings with Code Fixes | 15/15 (100%) |

---

## Summary

This PR intentionally demonstrates violations of the Virtual Card Lifecycle Specification. Each violation is documented in `REVIEW-FINDINGS.md` with:
- File location and line number
- Severity (🔴 critical or 🟠 high)
- Specification rule being violated
- Failure scenario (concrete inputs → wrong behavior)
- Detailed explanation (like an AI code review)
- Code fix with corrected implementation

**The goal**: Show that specification-driven development + AI agent guardrails can catch complex financial constraints automatically, preventing bugs that would otherwise ship.

🎬 **Screenshots of AI interactions coming in comments below** ↓
