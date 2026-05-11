package com.ai.homework.service

import com.ai.homework.dto.TicketCreateRequest
import com.ai.homework.dto.TicketMetadataDto
import com.ai.homework.dto.TicketUpdateRequest
import com.ai.homework.model.DeviceType
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketSource
import com.ai.homework.model.TicketStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("Ticket Service Tests")
class TicketServiceTest {

    @Autowired
    private lateinit var ticketService: TicketService

    @Test
    @DisplayName("Should create a new ticket")
    fun testCreateTicket() {
        val request = TicketCreateRequest(
            customerId = "cust-001",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(createdTicket).isNotNull
        assertThat(createdTicket.id).isNotBlank()
        assertThat(createdTicket.customerId).isEqualTo("cust-001")
    }

    @Test
    @DisplayName("Should retrieve a ticket by ID")
    fun testGetTicket() {
        val request = TicketCreateRequest(
            customerId = "cust-002",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)
        val retrievedTicket = ticketService.getTicket(createdTicket.id)

        assertThat(retrievedTicket).isNotNull
        assertThat(retrievedTicket!!.customerId).isEqualTo("cust-002")
    }

    @Test
    @DisplayName("Should return null for non-existent ticket")
    fun testGetNonExistentTicket() {
        val ticket = ticketService.getTicket("non-existent-id")

        assertThat(ticket).isNull()
    }

    @Test
    @DisplayName("Should list all tickets")
    fun testListAllTickets() {
        val request1 = TicketCreateRequest(
            customerId = "cust-003",
            customerEmail = "test1@example.com",
            customerName = "Test User 1",
            subject = "Subject 1",
            description = "This is a valid test description 1"
        )

        val request2 = TicketCreateRequest(
            customerId = "cust-004",
            customerEmail = "test2@example.com",
            customerName = "Test User 2",
            subject = "Subject 2",
            description = "This is a valid test description 2"
        )

        ticketService.createTicket(request1)
        ticketService.createTicket(request2)

        val tickets = ticketService.listTickets()

        assertThat(tickets.size).isGreaterThanOrEqualTo(2)
    }

    @Test
    @DisplayName("Should list tickets with status filter")
    fun testListTicketsWithStatusFilter() {
        val request = TicketCreateRequest(
            customerId = "cust-005",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        ticketService.createTicket(request)
        val tickets = ticketService.listTickets(status = TicketStatus.NEW)

        assertThat(tickets.size).isGreaterThanOrEqualTo(1)
        tickets.forEach { assertThat(it.status).isEqualTo(TicketStatus.NEW) }
    }

    @Test
    @DisplayName("Should update a ticket")
    fun testUpdateTicket() {
        val request = TicketCreateRequest(
            customerId = "cust-006",
            customerEmail = "test@example.com",
            customerName = "Original Name",
            subject = "Original Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)
        val updateRequest = TicketUpdateRequest(
            status = TicketStatus.IN_PROGRESS,
            category = null,
            priority = null,
            assignedTo = null,
            tags = null
        )

        ticketService.updateTicket(createdTicket.id, updateRequest)
        val retrievedTicket = ticketService.getTicket(createdTicket.id)

        assertThat(retrievedTicket!!.status).isEqualTo(TicketStatus.IN_PROGRESS)
    }

    @Test
    @DisplayName("Should delete a ticket")
    fun testDeleteTicket() {
        val request = TicketCreateRequest(
            customerId = "cust-007",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)
        ticketService.deleteTicket(createdTicket.id)
        val retrievedTicket = ticketService.getTicket(createdTicket.id)

        assertThat(retrievedTicket).isNull()
    }

    @Test
    @DisplayName("Should list tickets with priority filter")
    fun testListTicketsWithPriorityFilter() {
        val request = TicketCreateRequest(
            customerId = "cust-008",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description",
            priority = TicketPriority.HIGH
        )

        ticketService.createTicket(request)
        val tickets = ticketService.listTickets(priority = TicketPriority.HIGH)

        assertThat(tickets.size).isGreaterThanOrEqualTo(1)
        tickets.forEach { assertThat(it.priority).isEqualTo(TicketPriority.HIGH) }
    }

    @Test
    @DisplayName("Should list tickets with category filter")
    fun testListTicketsWithCategoryFilter() {
        val request = TicketCreateRequest(
            customerId = "cust-009",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description",
            category = TicketCategory.TECHNICAL_ISSUE
        )

        ticketService.createTicket(request)
        val tickets = ticketService.listTickets(category = TicketCategory.TECHNICAL_ISSUE)

        assertThat(tickets.size).isGreaterThanOrEqualTo(1)
        tickets.forEach { assertThat(it.category).isEqualTo(TicketCategory.TECHNICAL_ISSUE) }
    }

    @Test
    @DisplayName("Should create ticket with all fields")
    fun testCreateTicketWithAllFields() {
        val request = TicketCreateRequest(
            customerId = "cust-010",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Complete Ticket",
            description = "This is a complete ticket with all possible fields",
            category = TicketCategory.BUG_REPORT,
            priority = TicketPriority.URGENT,
            tags = listOf("urgent", "important")
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(createdTicket).isNotNull
        assertThat(createdTicket.id).isNotBlank()
        assertThat(createdTicket.category).isEqualTo(TicketCategory.BUG_REPORT)
        assertThat(createdTicket.priority).isEqualTo(TicketPriority.URGENT)
        assertThat(createdTicket.status).isEqualTo(TicketStatus.NEW)
        assertThat(createdTicket.tags).contains("urgent", "important")
    }

    @Test
    @DisplayName("Should generate unique IDs for different tickets")
    fun testUniqueTicketIds() {
        val request1 = TicketCreateRequest(
            customerId = "cust-011",
            customerEmail = "test1@example.com",
            customerName = "Test User 1",
            subject = "Subject 1",
            description = "This is a valid test description 1"
        )

        val request2 = TicketCreateRequest(
            customerId = "cust-012",
            customerEmail = "test2@example.com",
            customerName = "Test User 2",
            subject = "Subject 2",
            description = "This is a valid test description 2"
        )

        val createdTicket1 = ticketService.createTicket(request1)
        val createdTicket2 = ticketService.createTicket(request2)

        assertThat(createdTicket1.id).isNotEqualTo(createdTicket2.id)
    }

    @Test
    @DisplayName("Should handle empty list when no tickets match filter")
    fun testEmptyListWithNoMatchingTickets() {
        val tickets = ticketService.listTickets(status = TicketStatus.CLOSED)

        // May or may not have tickets, just verify we can filter
        assertThat(tickets).isNotNull
        tickets.forEach { assertThat(it.status).isEqualTo(TicketStatus.CLOSED) }
    }

    @Test
    @DisplayName("Should preserve ticket creation timestamp")
    fun testTicketCreationTimestamp() {
        val request = TicketCreateRequest(
            customerId = "cust-013",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(createdTicket.createdAt).isNotNull()
    }

    @Test
    @DisplayName("Should update ticket's updated timestamp on modification")
    fun testTicketUpdatedTimestamp() {
        val request = TicketCreateRequest(
            customerId = "cust-014",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)

        Thread.sleep(100)

        val updateRequest = TicketUpdateRequest(
            status = TicketStatus.RESOLVED,
            category = null,
            priority = null,
            assignedTo = null,
            tags = null
        )
        ticketService.updateTicket(createdTicket.id, updateRequest)

        val retrievedTicket = ticketService.getTicket(createdTicket.id)
        assertThat(retrievedTicket!!.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("Should list all tickets without filters")
    fun testGetAllTickets() {
        val request = TicketCreateRequest(
            customerId = "cust-get-all",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        ticketService.createTicket(request)
        val allTickets = ticketService.listTickets()

        assertThat(allTickets).isNotEmpty()
    }

    @Test
    @DisplayName("Should list tickets with customer ID filter")
    fun testListTicketsWithCustomerIdFilter() {
        val customerId = "cust-filter-by-id"
        val request = TicketCreateRequest(
            customerId = customerId,
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        ticketService.createTicket(request)
        val tickets = ticketService.listTickets(customerId = customerId)

        assertThat(tickets.size).isGreaterThanOrEqualTo(1)
        tickets.forEach { assertThat(it.customerId).isEqualTo(customerId) }
    }

    @Test
    @DisplayName("Should check if ticket exists")
    fun testTicketExists() {
        val request = TicketCreateRequest(
            customerId = "cust-exists-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(ticketService.ticketExists(createdTicket.id)).isTrue()
        assertThat(ticketService.ticketExists("non-existent-id")).isFalse()
    }

    @Test
    @DisplayName("Should count tickets in service")
    fun testCountTickets() {
        val request = TicketCreateRequest(
            customerId = "cust-count-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val initialCount = ticketService.countTickets()
        ticketService.createTicket(request)
        val finalCount = ticketService.countTickets()

        assertThat(finalCount).isGreaterThan(initialCount)
    }

    @Test
    @DisplayName("Should update non-existent ticket returns null")
    fun testUpdateNonExistentTicket() {
        val updateRequest = TicketUpdateRequest(
            status = TicketStatus.IN_PROGRESS,
            priority = TicketPriority.HIGH
        )

        val result = ticketService.updateTicket("non-existent-id", updateRequest)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Should delete non-existent ticket returns false")
    fun testDeleteNonExistentTicket() {
        val result = ticketService.deleteTicket("non-existent-id")

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("Should update all ticket fields")
    fun testUpdateAllTicketFields() {
        val request = TicketCreateRequest(
            customerId = "cust-update-all",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Original Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)
        val updateRequest = TicketUpdateRequest(
            status = TicketStatus.IN_PROGRESS,
            category = TicketCategory.FEATURE_REQUEST,
            priority = TicketPriority.LOW,
            assignedTo = "agent@company.com",
            tags = listOf("updated", "modified")
        )

        val updatedTicket = ticketService.updateTicket(createdTicket.id, updateRequest)

        assertThat(updatedTicket).isNotNull
        assertThat(updatedTicket!!.status).isEqualTo(TicketStatus.IN_PROGRESS)
        assertThat(updatedTicket.category).isEqualTo(TicketCategory.FEATURE_REQUEST)
        assertThat(updatedTicket.priority).isEqualTo(TicketPriority.LOW)
        assertThat(updatedTicket.assignedTo).isEqualTo("agent@company.com")
        assertThat(updatedTicket.tags).contains("updated", "modified")
    }

    @Test
    @DisplayName("Should set resolved timestamp when marking as resolved")
    fun testResolvedTimestampOnStatusChange() {
        val request = TicketCreateRequest(
            customerId = "cust-resolve-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)
        assertThat(createdTicket.resolvedAt).isNull()

        val updateRequest = TicketUpdateRequest(status = TicketStatus.RESOLVED)
        val resolvedTicket = ticketService.updateTicket(createdTicket.id, updateRequest)

        assertThat(resolvedTicket).isNotNull
        assertThat(resolvedTicket!!.resolvedAt).isNotNull()
    }

    @Test
    @DisplayName("Should keep existing resolved timestamp when already resolved")
    fun testPreserveResolvedTimestamp() {
        val request = TicketCreateRequest(
            customerId = "cust-preserve-resolve",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description"
        )

        val createdTicket = ticketService.createTicket(request)

        // First resolve
        val firstUpdate = TicketUpdateRequest(status = TicketStatus.RESOLVED)
        val firstResolved = ticketService.updateTicket(createdTicket.id, firstUpdate)!!
        val firstResolvedAt = firstResolved.resolvedAt

        Thread.sleep(100)

        // Try to update while already resolved
        val secondUpdate = TicketUpdateRequest(status = TicketStatus.RESOLVED)
        val secondResolved = ticketService.updateTicket(createdTicket.id, secondUpdate)!!
        val secondResolvedAt = secondResolved.resolvedAt

        assertThat(firstResolvedAt).isEqualTo(secondResolvedAt)
    }

    @Test
    @DisplayName("Should combine multiple filters in listTickets")
    fun testListTicketsWithMultipleFilters() {
        val request = TicketCreateRequest(
            customerId = "cust-multi-filter",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description",
            category = TicketCategory.BILLING_QUESTION,
            priority = TicketPriority.MEDIUM
        )

        ticketService.createTicket(request)

        val tickets = ticketService.listTickets(
            category = TicketCategory.BILLING_QUESTION,
            priority = TicketPriority.MEDIUM,
            customerId = "cust-multi-filter"
        )

        assertThat(tickets.size).isGreaterThanOrEqualTo(1)
        tickets.forEach {
            assertThat(it.category).isEqualTo(TicketCategory.BILLING_QUESTION)
            assertThat(it.priority).isEqualTo(TicketPriority.MEDIUM)
            assertThat(it.customerId).isEqualTo("cust-multi-filter")
        }
    }

    @Test
    @DisplayName("Should create ticket with metadata")
    fun testCreateTicketWithMetadata() {
        val metadata = TicketMetadataDto(
            source = TicketSource.EMAIL,
            browser = "Chrome 120",
            deviceType = DeviceType.DESKTOP
        )

        val request = TicketCreateRequest(
            customerId = "cust-with-metadata",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description",
            metadata = metadata
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(createdTicket).isNotNull
        assertThat(createdTicket.metadata).isNotNull
        assertThat(createdTicket.metadata!!.source).isEqualTo(TicketSource.EMAIL)
        assertThat(createdTicket.metadata!!.browser).isEqualTo("Chrome 120")
        assertThat(createdTicket.metadata!!.deviceType).isEqualTo(DeviceType.DESKTOP)
    }

    @Test
    @DisplayName("Should create ticket without metadata")
    fun testCreateTicketWithoutMetadata() {
        val request = TicketCreateRequest(
            customerId = "cust-no-metadata",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "This is a valid test description",
            metadata = null
        )

        val createdTicket = ticketService.createTicket(request)

        assertThat(createdTicket).isNotNull
        assertThat(createdTicket.metadata).isNull()
    }
}
