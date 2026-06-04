package com.fintech.card.service

import com.fintech.card.model.Card
import com.fintech.card.repository.CardRepository
import com.fintech.card.vault.PaymentVaultClient
import org.springframework.stereotype.Service
import java.util.*

@Service
class CardService(
    private val cardRepository: CardRepository,
    private val vaultClient: PaymentVaultClient
) {

    // VIOLATION: Using Double instead of BigDecimal for monetary values
    fun setCardLimit(cardId: String, dailyLimit: Double, monthlyLimit: Double): Card {
        val card = cardRepository.findById(cardId) ?: throw IllegalArgumentException("Card not found")

        // VIOLATION: No idempotency check before business logic
        card.dailyLimit = dailyLimit
        card.monthlyLimit = monthlyLimit

        return cardRepository.save(card)
    }

    // VIOLATION: Vault call inside DB transaction (holding connection during network I/O)
    fun issueCard(cardholderRef: String): Card {
        val newCard = Card(
            cardId = UUID.randomUUID().toString(),
            cardholderRef = cardholderRef,
            state = "ISSUED"
        )

        // This vault call blocks the DB transaction
        val vaultResponse = vaultClient.createCard(cardholderRef)
        newCard.cardToken = vaultResponse.token
        newCard.panMasked = vaultResponse.pan // VIOLATION: Full PAN might be exposed

        val saved = cardRepository.save(newCard)

        // VIOLATION: No audit event created in same transaction
        // Audit event created separately — if this fails, transaction is orphaned

        return saved
    }

    // VIOLATION: PAN logged directly; no trace ID in error handling
    fun getCardDetails(cardId: String): Card? {
        logger.info("Fetching card $cardId with PAN") // VIOLATION: PAN mentioned in log
        val card = cardRepository.findById(cardId)

        if (card == null) {
            logger.error("Card $cardId not found, returning 404") // VIOLATION: No traceId
        }

        return card
    }

    // VIOLATION: No optimistic locking for concurrent state changes
    fun freezeCard(cardId: String): Card {
        val card = cardRepository.findById(cardId) ?: throw IllegalArgumentException("Card not found")

        // Race condition: another request might update card.state here
        if (card.state != "ACTIVE") {
            throw IllegalStateException("Cannot freeze card in ${card.state} state")
        }

        card.state = "FROZEN"
        return cardRepository.save(card) // VIOLATION: No version check; concurrent writes silently lost
    }

    // VIOLATION: Error response not structured with traceId, code, details
    fun replaceCard(cardId: String): Card {
        val card = cardRepository.findById(cardId) ?: return null // VIOLATION: Wrong HTTP code semantics

        // VIOLATION: No error response wrapping
        if (card.state == "TERMINATED") {
            throw Exception("Card already terminated") // VIOLATION: Exposing stack trace
        }

        val newCardToken = vaultClient.createCard(card.cardholderRef).token
        card.state = "TERMINATED"
        cardRepository.save(card) // VIOLATION: Orphaned if next line fails

        val newCard = Card(
            cardId = UUID.randomUUID().toString(),
            cardholderRef = card.cardholderRef,
            state = "ACTIVE",
            cardToken = newCardToken
        )

        return cardRepository.save(newCard)
    }
}