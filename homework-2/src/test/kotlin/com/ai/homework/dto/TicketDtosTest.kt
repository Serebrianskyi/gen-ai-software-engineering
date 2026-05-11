package com.ai.homework.dto

import com.ai.homework.model.DeviceType
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketSource
import com.ai.homework.model.TicketStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

@DisplayName("DTO Tests")
class TicketDtosTest {

    @Test
    @DisplayName("Should create TicketMetadataDto with all fields")
    fun testTicketMetadataDtoAllFields() {
        val metadata = TicketMetadataDto(
            source = TicketSource.EMAIL,
            browser = "Chrome 120",
            deviceType = DeviceType.DESKTOP
        )

        assertThat(metadata.source).isEqualTo(TicketSource.EMAIL)
        assertThat(metadata.browser).isEqualTo("Chrome 120")
        assertThat(metadata.deviceType).isEqualTo(DeviceType.DESKTOP)
    }

    @Test
    @DisplayName("Should create TicketMetadataDto with null fields")
    fun testTicketMetadataDtoNullFields() {
        val metadata = TicketMetadataDto()

        assertThat(metadata.source).isNull()
        assertThat(metadata.browser).isNull()
        assertThat(metadata.deviceType).isNull()
    }

    @Test
    @DisplayName("Should create TicketMetadataDto with partial fields")
    fun testTicketMetadataDtoPartialFields() {
        val metadata = TicketMetadataDto(
            source = TicketSource.CHAT,
            browser = null,
            deviceType = DeviceType.MOBILE
        )

        assertThat(metadata.source).isEqualTo(TicketSource.CHAT)
        assertThat(metadata.browser).isNull()
        assertThat(metadata.deviceType).isEqualTo(DeviceType.MOBILE)
    }

    @Test
    @DisplayName("Should create TicketCreateRequest with all fields")
    fun testTicketCreateRequestAllFields() {
        val request = TicketCreateRequest(
            customerId = "cust123",
            customerEmail = "test@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "Test Description",
            category = TicketCategory.BUG_REPORT,
            priority = TicketPriority.HIGH,
            tags = listOf("urgent", "critical"),
            metadata = TicketMetadataDto(source = TicketSource.EMAIL, browser = "Firefox")
        )

        assertThat(request.customerId).isEqualTo("cust123")
        assertThat(request.customerEmail).isEqualTo("test@example.com")
        assertThat(request.customerName).isEqualTo("John Doe")
        assertThat(request.subject).isEqualTo("Test Subject")
        assertThat(request.description).isEqualTo("Test Description")
        assertThat(request.category).isEqualTo(TicketCategory.BUG_REPORT)
        assertThat(request.priority).isEqualTo(TicketPriority.HIGH)
        assertThat(request.tags).hasSize(2)
        assertThat(request.metadata).isNotNull
    }

    @Test
    @DisplayName("Should create TicketCreateRequest with minimal fields")
    fun testTicketCreateRequestMinimalFields() {
        val request = TicketCreateRequest(
            customerId = "cust123",
            customerEmail = "test@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "Test Description"
        )

        assertThat(request.customerId).isEqualTo("cust123")
        assertThat(request.category).isNull()
        assertThat(request.priority).isNull()
        assertThat(request.tags).isEmpty()
        assertThat(request.metadata).isNull()
    }

    @Test
    @DisplayName("Should create TicketUpdateRequest with all fields")
    fun testTicketUpdateRequestAllFields() {
        val request = TicketUpdateRequest(
            status = TicketStatus.RESOLVED,
            category = TicketCategory.BILLING_QUESTION,
            priority = TicketPriority.LOW,
            assignedTo = "user123",
            tags = listOf("resolved", "closed")
        )

        assertThat(request.status).isEqualTo(TicketStatus.RESOLVED)
        assertThat(request.category).isEqualTo(TicketCategory.BILLING_QUESTION)
        assertThat(request.priority).isEqualTo(TicketPriority.LOW)
        assertThat(request.assignedTo).isEqualTo("user123")
        assertThat(request.tags).hasSize(2)
    }

    @Test
    @DisplayName("Should create TicketUpdateRequest with null fields")
    fun testTicketUpdateRequestNullFields() {
        val request = TicketUpdateRequest()

        assertThat(request.status).isNull()
        assertThat(request.category).isNull()
        assertThat(request.priority).isNull()
        assertThat(request.assignedTo).isNull()
        assertThat(request.tags).isNull()
    }

    @Test
    @DisplayName("Should create TicketResponse with all fields")
    fun testTicketResponseAllFields() {
        val now = LocalDateTime.now()
        val response = TicketResponse(
            id = "ticket123",
            customerId = "cust123",
            customerEmail = "test@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "Test Description",
            category = TicketCategory.TECHNICAL_ISSUE,
            priority = TicketPriority.MEDIUM,
            status = TicketStatus.NEW,
            createdAt = now,
            updatedAt = now,
            resolvedAt = null,
            assignedTo = null,
            tags = listOf("tag1", "tag2"),
            metadata = TicketMetadataDto(source = TicketSource.PHONE)
        )

        assertThat(response.id).isEqualTo("ticket123")
        assertThat(response.customerId).isEqualTo("cust123")
        assertThat(response.customerEmail).isEqualTo("test@example.com")
        assertThat(response.customerName).isEqualTo("John Doe")
        assertThat(response.subject).isEqualTo("Test Subject")
        assertThat(response.description).isEqualTo("Test Description")
        assertThat(response.category).isEqualTo(TicketCategory.TECHNICAL_ISSUE)
        assertThat(response.priority).isEqualTo(TicketPriority.MEDIUM)
        assertThat(response.status).isEqualTo(TicketStatus.NEW)
        assertThat(response.createdAt).isEqualTo(now)
        assertThat(response.updatedAt).isEqualTo(now)
        assertThat(response.resolvedAt).isNull()
        assertThat(response.assignedTo).isNull()
        assertThat(response.tags).hasSize(2)
        assertThat(response.metadata).isNotNull
    }

    @Test
    @DisplayName("Should create TicketResponse with minimal required fields")
    fun testTicketResponseMinimalFields() {
        val now = LocalDateTime.now()
        val response = TicketResponse(
            id = "ticket123",
            customerId = "cust123",
            customerEmail = "test@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "Test Description",
            category = TicketCategory.OTHER,
            priority = TicketPriority.MEDIUM,
            status = TicketStatus.NEW,
            createdAt = now,
            updatedAt = now,
            tags = emptyList()
        )

        assertThat(response.id).isNotEmpty()
        assertThat(response.tags).isEmpty()
        assertThat(response.resolvedAt).isNull()
        assertThat(response.assignedTo).isNull()
        assertThat(response.metadata).isNull()
    }

    @Test
    @DisplayName("Should create ImportError with all fields")
    fun testImportErrorAllFields() {
        val error = ImportError(
            row = 5,
            field = "customer_email",
            message = "Invalid email format"
        )

        assertThat(error.row).isEqualTo(5)
        assertThat(error.field).isEqualTo("customer_email")
        assertThat(error.message).isEqualTo("Invalid email format")
    }

    @Test
    @DisplayName("Should create ImportResult with all fields")
    fun testImportResultAllFields() {
        val errors = listOf(
            ImportError(1, "email", "Invalid"),
            ImportError(2, "name", "Empty")
        )
        val result = ImportResult(
            totalRecords = 10,
            successful = 8,
            failed = 2,
            errors = errors
        )

        assertThat(result.totalRecords).isEqualTo(10)
        assertThat(result.successful).isEqualTo(8)
        assertThat(result.failed).isEqualTo(2)
        assertThat(result.errors).hasSize(2)
    }

    @Test
    @DisplayName("Should create ImportResult with empty errors")
    fun testImportResultEmptyErrors() {
        val result = ImportResult(
            totalRecords = 5,
            successful = 5,
            failed = 0
        )

        assertThat(result.totalRecords).isEqualTo(5)
        assertThat(result.successful).isEqualTo(5)
        assertThat(result.failed).isEqualTo(0)
        assertThat(result.errors).isEmpty()
    }

    @Test
    @DisplayName("Should create ClassificationResult with all fields")
    fun testClassificationResultAllFields() {
        val result = ClassificationResult(
            category = TicketCategory.ACCOUNT_ACCESS,
            priority = TicketPriority.HIGH,
            confidence = 0.95,
            keywordsFound = listOf("account", "access", "locked"),
            reasoning = "Multiple account-related keywords found"
        )

        assertThat(result.category).isEqualTo(TicketCategory.ACCOUNT_ACCESS)
        assertThat(result.priority).isEqualTo(TicketPriority.HIGH)
        assertThat(result.confidence).isEqualTo(0.95)
        assertThat(result.keywordsFound).hasSize(3)
        assertThat(result.reasoning).contains("account")
    }

    @Test
    @DisplayName("Should create ErrorResponse with defaults")
    fun testErrorResponseWithDefaults() {
        val response = ErrorResponse(
            error = "Not Found",
            message = "Ticket not found"
        )

        assertThat(response.error).isEqualTo("Not Found")
        assertThat(response.message).isEqualTo("Ticket not found")
        assertThat(response.timestamp).isNotNull()
        assertThat(response.path).isNull()
    }

    @Test
    @DisplayName("Should create ErrorResponse with all fields")
    fun testErrorResponseAllFields() {
        val response = ErrorResponse(
            error = "Bad Request",
            message = "Invalid input",
            path = "/tickets/invalid-id"
        )

        assertThat(response.error).isEqualTo("Bad Request")
        assertThat(response.message).isEqualTo("Invalid input")
        assertThat(response.path).isEqualTo("/tickets/invalid-id")
    }

    @Test
    @DisplayName("Should handle TicketMetadataDto with various source types")
    fun testTicketMetadataDtoVariousSources() {
        val sources = listOf(
            TicketSource.EMAIL,
            TicketSource.CHAT,
            TicketSource.PHONE,
            TicketSource.WEB_FORM,
            TicketSource.API
        )

        for (source in sources) {
            val metadata = TicketMetadataDto(source = source)
            assertThat(metadata.source).isEqualTo(source)
        }
    }

    @Test
    @DisplayName("Should handle TicketMetadataDto with various device types")
    fun testTicketMetadataDtoVariousDeviceTypes() {
        val deviceTypes = listOf(
            DeviceType.DESKTOP,
            DeviceType.MOBILE,
            DeviceType.TABLET
        )

        for (deviceType in deviceTypes) {
            val metadata = TicketMetadataDto(deviceType = deviceType)
            assertThat(metadata.deviceType).isEqualTo(deviceType)
        }
    }

    @Test
    @DisplayName("Should handle TicketResponse with resolved ticket")
    fun testTicketResponseResolved() {
        val now = LocalDateTime.now()
        val resolvedAt = now.plusHours(24)
        val response = TicketResponse(
            id = "resolved-123",
            customerId = "cust123",
            customerEmail = "test@example.com",
            customerName = "John",
            subject = "Fixed Issue",
            description = "This issue has been resolved",
            category = TicketCategory.BUG_REPORT,
            priority = TicketPriority.MEDIUM,
            status = TicketStatus.RESOLVED,
            createdAt = now,
            updatedAt = now,
            resolvedAt = resolvedAt,
            assignedTo = "agent@company.com",
            tags = listOf("resolved", "closed")
        )

        assertThat(response.status).isEqualTo(TicketStatus.RESOLVED)
        assertThat(response.resolvedAt).isEqualTo(resolvedAt)
        assertThat(response.assignedTo).isNotNull()
    }
}
