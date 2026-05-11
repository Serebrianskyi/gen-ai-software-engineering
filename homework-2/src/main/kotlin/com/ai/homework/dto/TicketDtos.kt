package com.ai.homework.dto

import com.ai.homework.model.DeviceType
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketSource
import com.ai.homework.model.TicketStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class TicketMetadataDto(
    @JsonProperty("source")
    val source: TicketSource? = null,

    @JsonProperty("browser")
    val browser: String? = null,

    @JsonProperty("device_type")
    val deviceType: DeviceType? = null
)

data class TicketCreateRequest(
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
    val category: TicketCategory? = null,

    @JsonProperty("priority")
    val priority: TicketPriority? = null,

    @JsonProperty("tags")
    val tags: List<String> = emptyList(),

    @JsonProperty("metadata")
    val metadata: TicketMetadataDto? = null
)

data class TicketUpdateRequest(
    @JsonProperty("status")
    val status: TicketStatus? = null,

    @JsonProperty("category")
    val category: TicketCategory? = null,

    @JsonProperty("priority")
    val priority: TicketPriority? = null,

    @JsonProperty("assigned_to")
    val assignedTo: String? = null,

    @JsonProperty("tags")
    val tags: List<String>? = null
)

data class TicketResponse(
    @JsonProperty("id")
    val id: String,

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
    val category: TicketCategory,

    @JsonProperty("priority")
    val priority: TicketPriority,

    @JsonProperty("status")
    val status: TicketStatus,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    @JsonProperty("resolved_at")
    val resolvedAt: LocalDateTime? = null,

    @JsonProperty("assigned_to")
    val assignedTo: String? = null,

    @JsonProperty("tags")
    val tags: List<String>,

    @JsonProperty("metadata")
    val metadata: TicketMetadataDto? = null
)

data class ImportError(
    @JsonProperty("row")
    val row: Int,

    @JsonProperty("field")
    val field: String,

    @JsonProperty("message")
    val message: String
)

data class ImportResult(
    @JsonProperty("total_records")
    val totalRecords: Int,

    @JsonProperty("successful")
    val successful: Int,

    @JsonProperty("failed")
    val failed: Int,

    @JsonProperty("errors")
    val errors: List<ImportError> = emptyList()
)

data class ClassificationResult(
    @JsonProperty("category")
    val category: TicketCategory,

    @JsonProperty("priority")
    val priority: TicketPriority,

    @JsonProperty("confidence")
    val confidence: Double,

    @JsonProperty("keywords_found")
    val keywordsFound: List<String>,

    @JsonProperty("reasoning")
    val reasoning: String
)

data class TicketCreateWithClassificationResponse(
    @JsonProperty("ticket")
    val ticket: TicketResponse,

    @JsonProperty("classification")
    val classification: ClassificationResult
)

data class TicketListResponse(
    @JsonProperty("count")
    val count: Int,

    @JsonProperty("tickets")
    val tickets: List<TicketResponse>
)

data class ErrorResponse(
    @JsonProperty("error")
    val error: String,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("path")
    val path: String? = null
)