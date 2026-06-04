package com.fintech.card.model

import java.util.*

// VIOLATION: No version column for optimistic locking
data class Card(
    val cardId: String,
    val cardholderRef: String,
    var cardToken: String = "",
    var panMasked: String = "",
    var state: String, // VIOLATION: No enum, allows invalid states
    var dailyLimit: Double = 0.0, // VIOLATION: Float instead of BigDecimal
    var monthlyLimit: Double = 0.0, // VIOLATION: Float instead of BigDecimal
    var createdAt: Date = Date(),
    var updatedAt: Date = Date()
    // VIOLATION: No currencyCode paired with limits
    // VIOLATION: No version field for optimistic locking
    // VIOLATION: No auditEvents relationship
)