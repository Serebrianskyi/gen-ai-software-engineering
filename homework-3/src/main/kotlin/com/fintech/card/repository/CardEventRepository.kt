package com.fintech.card.repository

import com.fintech.card.model.CardEvent
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CardEventRepository : CrudRepository<CardEvent, String> {
    fun findByCardId(cardId: String): List<CardEvent>
}

// VIOLATION: No trigger enforcement in repository layer; mutable audit logs
// VIOLATION: No custom delete prevention — audit events can be deleted via JPA
// VIOLATION: No Kafka publishing to card.events topic
// VIOLATION: No transactional semantics enforcement