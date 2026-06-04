package com.fintech.card.vault

import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.logging.Logger

@Component
class PaymentVaultClient(
    private val restTemplate: RestTemplate
) {
    private val logger = Logger.getLogger(PaymentVaultClient::class.java.name)

    fun createCard(cardholderRef: String): VaultResponse {
        val url = "https://vault.internal/v1/cards"

        // VIOLATION: No circuit breaker pattern
        // VIOLATION: No retry logic
        // VIOLATION: No timeout configuration

        val response = restTemplate.postForObject(url, mapOf("holder_id" to cardholderRef), VaultResponse::class.java)
            ?: throw Exception("Vault returned null")

        // VIOLATION: Logging full PAN from vault response
        logger.info("Card issued: PAN=${response.pan}, token=${response.token}")

        return response
    }

    fun terminateCard(cardToken: String): Boolean {
        val url = "https://vault.internal/v1/cards/$cardToken/terminate"

        // VIOLATION: No circuit breaker
        // VIOLATION: No retry logic
        // VIOLATION: Exposing card token in URL (should be in body)

        return restTemplate.postForObject(url, null, Boolean::class.java) ?: false
    }
}

// VIOLATION: Full PAN exposed in response object; spec requires never leaving vault
data class VaultResponse(
    val token: String,
    val pan: String, // VIOLATION: Should only return masked format
    val cvv: String? // VIOLATION: CVV should never be returned
)