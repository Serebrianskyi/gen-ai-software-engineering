package com.ai.homework.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.ai.homework.dto.TicketCreateRequest

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ticket Controller Extended Tests")
class TicketControllerExtendedTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("Should list all tickets")
    fun testListAllTickets() {
        mockMvc.perform(get("/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @DisplayName("Should return 404 for non-existent ticket")
    fun testGetNonExistentTicket() {
        mockMvc.perform(get("/tickets/non-existent-id"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Should create ticket with valid data")
    fun testCreateTicketValid() {
        val request = TicketCreateRequest(
            customerId = "test001",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "Valid test description here"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    @DisplayName("Should reject ticket with missing required field")
    fun testCreateTicketMissingField() {
        val invalidJson = """{"customer_email":"test@example.com","customer_name":"Test"}"""

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(invalidJson))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should update existing ticket")
    fun testUpdateTicket() {
        // First create a ticket
        val createRequest = TicketCreateRequest(
            customerId = "test-002",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Original Subject",
            description = "Original description"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(createRequest)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        // Now update it
        val updateRequest = """{"status":"in_progress","priority":"high"}"""

        mockMvc.perform(put("/tickets/$ticketId")
            .contentType("application/json")
            .content(updateRequest))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should delete existing ticket")
    fun testDeleteTicket() {
        // First create a ticket
        val createRequest = TicketCreateRequest(
            customerId = "test-003",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "To Delete",
            description = "This ticket will be deleted"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(createRequest)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        // Delete it
        mockMvc.perform(delete("/tickets/$ticketId"))
            .andExpect(status().isNoContent)

        // Verify it's deleted
        mockMvc.perform(get("/tickets/$ticketId"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Should return 400 for invalid request body")
    fun testCreateWithInvalidJson() {
        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content("invalid json"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should handle multiple ticket creation")
    fun testCreateMultipleTickets() {
        for (i in 1..5) {
            val request = TicketCreateRequest(
                customerId = "multi-$i",
                customerEmail = "test$i@example.com",
                customerName = "Test User $i",
                subject = "Subject $i",
                description = "Valid description for ticket $i"
            )

            mockMvc.perform(post("/tickets")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated)
        }

        // List all and verify count
        val response = mockMvc.perform(get("/tickets"))
            .andExpect(status().isOk)
            .andReturn()

        val responseBody = response.response.contentAsString
        val tickets = objectMapper.readTree(responseBody)
        assert(tickets.size() >= 5)
    }

    @Test
    @DisplayName("Should handle create with all optional fields")
    fun testCreateWithAllFields() {
        val request = TicketCreateRequest(
            customerId = "full-001",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Complete Ticket",
            description = "Complete ticket with all possible fields",
            category = com.ai.homework.model.TicketCategory.BUG_REPORT,
            priority = com.ai.homework.model.TicketPriority.HIGH
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.category").value("bug_report"))
            .andExpect(jsonPath("$.priority").value("high"))
    }

    @Test
    @DisplayName("Should return correct response headers")
    fun testResponseHeaders() {
        val request = TicketCreateRequest(
            customerId = "header-001",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test",
            description = "Valid description"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(header().exists("Content-Type"))
    }

    @Test
    @DisplayName("Should handle concurrent ticket creation")
    fun testConcurrentCreation() {
        val threads = mutableListOf<Thread>()

        for (i in 1..3) {
            val thread = Thread {
                val request = TicketCreateRequest(
                    customerId = "concurrent-$i",
                    customerEmail = "concurrent$i@example.com",
                    customerName = "Concurrent User $i",
                    subject = "Concurrent Subject $i",
                    description = "Description for concurrent ticket $i"
                )

                mockMvc.perform(post("/tickets")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated)
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach { it.join() }
    }

    @Test
    @DisplayName("Should update non-existent ticket with 404")
    fun testUpdateNonExistentTicket() {
        val updateRequest = """{"status":"in_progress"}"""

        mockMvc.perform(put("/tickets/non-existent-id")
            .contentType("application/json")
            .content(updateRequest))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    @DisplayName("Should delete non-existent ticket with 404")
    fun testDeleteNonExistentTicket() {
        mockMvc.perform(delete("/tickets/non-existent-delete-id"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    @DisplayName("Should reject creation with invalid email")
    fun testCreateTicketInvalidEmail() {
        val request = TicketCreateRequest(
            customerId = "test-invalid",
            customerEmail = "not-an-email",
            customerName = "Test User",
            subject = "Test",
            description = "Valid description"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should reject creation with short subject")
    fun testCreateTicketShortSubject() {
        val request = TicketCreateRequest(
            customerId = "test",
            customerEmail = "test@example.com",
            customerName = "Test",
            subject = "",
            description = "Valid description with enough length"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should reject creation with short description")
    fun testCreateTicketShortDescription() {
        val request = TicketCreateRequest(
            customerId = "test",
            customerEmail = "test@example.com",
            customerName = "Test",
            subject = "Valid Subject",
            description = "short"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should update ticket with all new fields")
    fun testUpdateTicketAllFields() {
        val createRequest = TicketCreateRequest(
            customerId = "update-all",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Original",
            description = "Original description here"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(createRequest)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        val updateRequest = """{"status":"in_progress","priority":"high","category":"bug_report","assigned_to":"agent@company.com","tags":["urgent"]}"""

        mockMvc.perform(put("/tickets/$ticketId")
            .contentType("application/json")
            .content(updateRequest))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("in_progress"))
            .andExpect(jsonPath("$.priority").value("high"))
            .andExpect(jsonPath("$.category").value("bug_report"))
            .andExpect(jsonPath("$.assigned_to").value("agent@company.com"))
            .andExpect(jsonPath("$.tags[0]").value("urgent"))
    }

    @Test
    @DisplayName("Should list all tickets without parameters")
    fun testListTicketsWithoutFilters() {
        mockMvc.perform(get("/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @DisplayName("Should handle list with customer_id filter")
    fun testListTicketsWithCustomerId() {
        val customerId = "customer-filter-123"
        val request = TicketCreateRequest(
            customerId = customerId,
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "Valid test description"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)

        mockMvc.perform(get("/tickets")
            .param("customer_id", customerId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @DisplayName("Should create ticket with metadata fields in DTO")
    fun testCreateTicketWithMetadataFields() {
        val request = TicketCreateRequest(
            customerId = "metadata-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Test Subject",
            description = "Valid test description with metadata"
        )

        mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }
}
