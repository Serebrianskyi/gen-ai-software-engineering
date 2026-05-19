package com.ai.homework.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

@DisplayName("Ticket Model Tests")
class TicketModelTest {

    @Test
    @DisplayName("Should create a valid ticket with all fields")
    fun testCreateValidTicket() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "This is a test description with sufficient length"
        )

        assertThat(ticket.id).isNotBlank()
        assertThat(ticket.customerId).isEqualTo("cust-123")
        assertThat(ticket.customerEmail).isEqualTo("user@example.com")
        assertThat(ticket.category).isEqualTo(TicketCategory.OTHER)
        assertThat(ticket.priority).isEqualTo(TicketPriority.MEDIUM)
        assertThat(ticket.status).isEqualTo(TicketStatus.NEW)
    }

    @Test
    @DisplayName("Should validate subject length constraints")
    fun testSubjectLengthValidation() {
        val validSubject = "x".repeat(200)
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = validSubject,
            description = "This is a test description"
        )

        assertThat(ticket.subject.length).isEqualTo(200)
    }

    @Test
    @DisplayName("Should validate description length constraints")
    fun testDescriptionLengthValidation() {
        val shortDescription = "x".repeat(10)
        val longDescription = "x".repeat(2000)

        val shortTicket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = shortDescription
        )

        val longTicket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = longDescription
        )

        assertThat(shortTicket.description.length).isEqualTo(10)
        assertThat(longTicket.description.length).isEqualTo(2000)
    }

    @Test
    @DisplayName("Should handle all ticket categories")
    fun testAllTicketCategories() {
        val categories = listOf(
            TicketCategory.ACCOUNT_ACCESS,
            TicketCategory.TECHNICAL_ISSUE,
            TicketCategory.BILLING_QUESTION,
            TicketCategory.FEATURE_REQUEST,
            TicketCategory.BUG_REPORT,
            TicketCategory.OTHER
        )

        for (category in categories) {
            val ticket = Ticket(
                customerId = "cust-123",
                customerEmail = "user@example.com",
                customerName = "John Doe",
                subject = "Subject",
                description = "Description with sufficient length",
                category = category
            )
            assertThat(ticket.category).isEqualTo(category)
        }
    }

    @Test
    @DisplayName("Should handle all priority levels")
    fun testAllPriorityLevels() {
        val priorities = listOf(
            TicketPriority.URGENT,
            TicketPriority.HIGH,
            TicketPriority.MEDIUM,
            TicketPriority.LOW
        )

        for (priority in priorities) {
            val ticket = Ticket(
                customerId = "cust-123",
                customerEmail = "user@example.com",
                customerName = "John Doe",
                subject = "Subject",
                description = "Description with sufficient length",
                priority = priority
            )
            assertThat(ticket.priority).isEqualTo(priority)
        }
    }

    @Test
    @DisplayName("Should handle all status values")
    fun testAllStatusValues() {
        val statuses = listOf(
            TicketStatus.NEW,
            TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_CUSTOMER,
            TicketStatus.RESOLVED,
            TicketStatus.CLOSED
        )

        for (status in statuses) {
            val ticket = Ticket(
                customerId = "cust-123",
                customerEmail = "user@example.com",
                customerName = "John Doe",
                subject = "Subject",
                description = "Description with sufficient length",
                status = status
            )
            assertThat(ticket.status).isEqualTo(status)
        }
    }

    @Test
    @DisplayName("Should handle metadata correctly")
    fun testTicketMetadata() {
        val metadata = TicketMetadata(
            source = TicketSource.WEB_FORM,
            browser = "Chrome",
            deviceType = DeviceType.DESKTOP
        )

        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length",
            metadata = metadata
        )

        assertThat(ticket.metadata?.source).isEqualTo(TicketSource.WEB_FORM)
        assertThat(ticket.metadata?.browser).isEqualTo("Chrome")
        assertThat(ticket.metadata?.deviceType).isEqualTo(DeviceType.DESKTOP)
    }

    @Test
    @DisplayName("Should handle tags list correctly")
    fun testTicketTags() {
        val tags = listOf("urgent", "important", "follow-up")
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length",
            tags = tags
        )

        assertThat(ticket.tags).isEqualTo(tags)
        assertThat(ticket.tags).hasSize(3)
    }

    @Test
    @DisplayName("Should handle special characters in fields")
    fun testSpecialCharactersInFields() {
        val ticket = Ticket(
            customerId = "cust-123-special!@#",
            customerEmail = "user+tag@example.com",
            customerName = "John O'Reilly",
            subject = "Subject with \"quotes\" and 'apostrophes'",
            description = "Description with special chars: !@#$%^&*()"
        )

        assertThat(ticket.customerId).contains("-")
        assertThat(ticket.customerEmail).contains("+")
        assertThat(ticket.customerName).contains("'")
        assertThat(ticket.subject).contains("\"")
        assertThat(ticket.description).contains("!")
    }
}