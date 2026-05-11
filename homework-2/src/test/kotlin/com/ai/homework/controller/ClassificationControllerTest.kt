package com.ai.homework.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.fasterxml.jackson.databind.ObjectMapper
import com.ai.homework.dto.TicketCreateRequest

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Classification Controller Tests")
class ClassificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("Should return 404 for non-existent ticket classification")
    fun testAutoClassifyNonExistentTicket() {
        mockMvc.perform(post("/tickets/nonexistent123/auto-classify"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("Should return error response structure for missing ticket")
    fun testAutoClassifyMissingTicketErrorFormat() {
        mockMvc.perform(post("/tickets/missing-id-12345/auto-classify"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.path").value("/tickets/missing-id-12345/auto-classify"))
    }

    @Test
    @DisplayName("Should classify ticket when ticket exists")
    fun testAutoClassifyWithExistingTicket() {
        // Create a ticket first
        val request = TicketCreateRequest(
            customerId = "classify-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Account access problem",
            description = "I cannot access my account due to authentication issues"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        // Now classify it
        mockMvc.perform(post("/tickets/$ticketId/auto-classify"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category").exists())
    }

    @Test
    @DisplayName("Should return classification result with priority")
    fun testAutoClassifyResultHasPriority() {
        val request = TicketCreateRequest(
            customerId = "priority-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Urgent billing issue",
            description = "Billing charge is incorrect and needs to be resolved immediately"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        mockMvc.perform(post("/tickets/$ticketId/auto-classify"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.priority").exists())
    }

    @Test
    @DisplayName("Should return classification result with confidence")
    fun testAutoClassifyResultHasConfidence() {
        val request = TicketCreateRequest(
            customerId = "confidence-test",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Need feature",
            description = "I would like a new feature for dark mode support"
        )

        val createResponse = mockMvc.perform(post("/tickets")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andReturn()

        val responseBody = createResponse.response.contentAsString
        val ticketId = objectMapper.readTree(responseBody).get("id").asText()

        mockMvc.perform(post("/tickets/$ticketId/auto-classify"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.confidence").exists())
    }
}
