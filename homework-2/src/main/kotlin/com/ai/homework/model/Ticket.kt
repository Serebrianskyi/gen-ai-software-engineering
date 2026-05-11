package com.ai.homework.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.*

enum class TicketCategory {
    @JsonProperty("account_access")
    ACCOUNT_ACCESS,

    @JsonProperty("technical_issue")
    TECHNICAL_ISSUE,

    @JsonProperty("billing_question")
    BILLING_QUESTION,

    @JsonProperty("feature_request")
    FEATURE_REQUEST,

    @JsonProperty("bug_report")
    BUG_REPORT,

    @JsonProperty("other")
    OTHER,
}

enum class TicketPriority {
    @JsonProperty("urgent")
    URGENT,

    @JsonProperty("high")
    HIGH,

    @JsonProperty("medium")
    MEDIUM,

    @JsonProperty("low")
    LOW,
}

enum class TicketStatus {
    @JsonProperty("new")
    NEW,

    @JsonProperty("in_progress")
    IN_PROGRESS,

    @JsonProperty("waiting_customer")
    WAITING_CUSTOMER,

    @JsonProperty("resolved")
    RESOLVED,

    @JsonProperty("closed")
    CLOSED,
}

enum class TicketSource {
    @JsonProperty("web_form")
    WEB_FORM,

    @JsonProperty("email")
    EMAIL,

    @JsonProperty("api")
    API,

    @JsonProperty("chat")
    CHAT,

    @JsonProperty("phone")
    PHONE,
}

enum class DeviceType {
    @JsonProperty("desktop")
    DESKTOP,

    @JsonProperty("mobile")
    MOBILE,

    @JsonProperty("tablet")
    TABLET,
}

data class TicketMetadata(
    @JsonProperty("source")
    val source: TicketSource? = null,

    @JsonProperty("browser")
    val browser: String? = null,

    @JsonProperty("device_type")
    val deviceType: DeviceType? = null,
)

data class Ticket(
    @JsonProperty("id")
    val id: String = UUID.randomUUID().toString(),

    @JsonProperty("customer_id")
    val customerId: String,

    @JsonProperty("customer_email")
    val customerEmail: String,

    @JsonProperty("customer_name")
    val customerName: String,

    @JsonProperty("subject")
    val subject: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("category")
    val category: TicketCategory = TicketCategory.OTHER,

    @JsonProperty("priority")
    val priority: TicketPriority = TicketPriority.MEDIUM,

    @JsonProperty("status")
    val status: TicketStatus = TicketStatus.NEW,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("resolved_at")
    val resolvedAt: LocalDateTime? = null,

    @JsonProperty("assigned_to")
    val assignedTo: String? = null,

    @JsonProperty("tags")
    val tags: List<String> = emptyList(),

    @JsonProperty("metadata")
    val metadata: TicketMetadata? = null,
)
