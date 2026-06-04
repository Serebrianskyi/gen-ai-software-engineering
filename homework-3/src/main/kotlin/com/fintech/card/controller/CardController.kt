package com.fintech.card.controller

import com.fintech.card.service.CardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/v1/cards")
class CardController(
    private val cardService: CardService
) {

    // VIOLATION: No idempotency key handling
    @PostMapping
    fun createCard(
        @RequestBody request: CreateCardRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Any> {
        // VIOLATION: No idempotency key validation before vault call
        // VIOLATION: No rate limit check before business logic

        try {
            val card = cardService.issueCard(request.cardholderRef)
            return ResponseEntity.ok(card)
        } catch (e: Exception) {
            // VIOLATION: Returning raw exception; no structured error response with traceId
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to e.message) // VIOLATION: Should include code, details, traceId
            )
        }
    }

    @PatchMapping("/{cardId}/freeze")
    fun freezeCard(
        @PathVariable cardId: String,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Any> {
        // VIOLATION: No ownership check (should return 403, not 404)
        // VIOLATION: No rate limit check

        return try {
            val card = cardService.freezeCard(cardId)
            ResponseEntity.ok(card)
        } catch (e: IllegalStateException) {
            // VIOLATION: Using 500 instead of 409 for state machine violation
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to e.message)
            )
        } catch (e: Exception) {
            // VIOLATION: Stack trace might be exposed
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e)
        }
    }

    @GetMapping("/{cardId}")
    fun getCard(
        @PathVariable cardId: String,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Any> {
        // VIOLATION: No caching headers (Redis cache not utilized)
        // VIOLATION: No ownership check

        val card = cardService.getCardDetails(cardId)

        // VIOLATION: Returning 404 instead of 403 on permission denied
        if (card == null) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(card)
    }

    @PostMapping("/{cardId}/replace")
    fun replaceCard(
        @PathVariable cardId: String,
        @RequestHeader("Authorization") token: String,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String? // VIOLATION: Should be required
    ): ResponseEntity<Any> {
        // VIOLATION: Optional idempotency key; spec requires it on all mutations

        return try {
            val newCard = cardService.replaceCard(cardId)
            ResponseEntity.ok(newCard)
        } catch (e: Exception) {
            // VIOLATION: No error wrapping
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }

    @PatchMapping("/{cardId}/limits")
    fun setLimits(
        @PathVariable cardId: String,
        @RequestBody request: SetLimitsRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Any> {
        // VIOLATION: Accepting Double for monetary values instead of validating BigDecimal
        // VIOLATION: No currency code validation

        val card = cardService.setCardLimit(cardId, request.dailyLimit, request.monthlyLimit)
        return ResponseEntity.ok(card)
    }
}

data class CreateCardRequest(val cardholderRef: String)
data class SetLimitsRequest(
    val dailyLimit: Double, // VIOLATION: Should reject float; BigDecimal required
    val monthlyLimit: Double // VIOLATION: No currency code
)