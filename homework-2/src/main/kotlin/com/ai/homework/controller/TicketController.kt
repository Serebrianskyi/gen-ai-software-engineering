package com.ai.homework.controller

import com.ai.homework.dto.ErrorResponse
import com.ai.homework.dto.TicketCreateRequest
import com.ai.homework.dto.TicketCreateWithClassificationResponse
import com.ai.homework.dto.TicketListResponse
import com.ai.homework.dto.TicketResponse
import com.ai.homework.dto.TicketUpdateRequest
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketStatus
import com.ai.homework.service.ClassificationService
import com.ai.homework.service.TicketService
import com.ai.homework.validator.TicketValidator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/tickets")
class TicketController(
    private val ticketService: TicketService,
    private val classificationService: ClassificationService,
    private val validator: TicketValidator
) {

    @PostMapping
    fun createTicket(
        @RequestBody request: TicketCreateRequest,
        @RequestParam(name = "auto_classify", defaultValue = "false") autoClassify: Boolean
    ): ResponseEntity<Any> {
        val validationErrors = validator.validate(request)
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(
                ErrorResponse(
                    error = "Validation Error",
                    message = validationErrors.joinToString("; "),
                    path = "/tickets"
                ),
                HttpStatus.BAD_REQUEST
            )
        }

        val ticket = ticketService.createTicket(request)
        if (autoClassify) {
            val classification = classificationService.classify(ticket)
            return ResponseEntity(
                TicketCreateWithClassificationResponse(ticket.toResponse(), classification),
                HttpStatus.CREATED
            )
        }
        return ResponseEntity(ticket.toResponse(), HttpStatus.CREATED)
    }

    @GetMapping
    fun listTickets(
        @RequestParam category: TicketCategory? = null,
        @RequestParam priority: TicketPriority? = null,
        @RequestParam status: TicketStatus? = null,
        @RequestParam(name = "customer_id") customerId: String? = null
    ): ResponseEntity<TicketListResponse> {
        val tickets = ticketService.listTickets(category, priority, status, customerId)
        val response = tickets.map { it.toResponse() }
        return ResponseEntity(TicketListResponse(count = response.size, tickets = response), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun getTicket(@PathVariable id: String): ResponseEntity<Any> {
        val ticket = ticketService.getTicket(id)
        return if (ticket != null) {
            ResponseEntity(ticket.toResponse(), HttpStatus.OK)
        } else {
            ResponseEntity(
                ErrorResponse(
                    error = "Not Found",
                    message = "Ticket with id $id not found",
                    path = "/tickets/$id"
                ),
                HttpStatus.NOT_FOUND
            )
        }
    }

    @PutMapping("/{id}")
    fun updateTicket(
        @PathVariable id: String,
        @RequestBody request: TicketUpdateRequest
    ): ResponseEntity<Any> {
        if (!ticketService.ticketExists(id)) {
            return ResponseEntity(
                ErrorResponse(
                    error = "Not Found",
                    message = "Ticket with id $id not found",
                    path = "/tickets/$id"
                ),
                HttpStatus.NOT_FOUND
            )
        }

        val updatedTicket = ticketService.updateTicket(id, request)
        return ResponseEntity(updatedTicket?.toResponse(), HttpStatus.OK)
    }

    @DeleteMapping("/{id}")
    fun deleteTicket(@PathVariable id: String): ResponseEntity<Any> {
        return if (ticketService.deleteTicket(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity(
                ErrorResponse(
                    error = "Not Found",
                    message = "Ticket with id $id not found",
                    path = "/tickets/$id"
                ),
                HttpStatus.NOT_FOUND
            )
        }
    }
}

private fun com.ai.homework.model.Ticket.toResponse(): TicketResponse {
    return TicketResponse(
        id = this.id,
        customerId = this.customerId,
        customerEmail = this.customerEmail,
        customerName = this.customerName,
        subject = this.subject,
        description = this.description,
        category = this.category,
        priority = this.priority,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        resolvedAt = this.resolvedAt,
        assignedTo = this.assignedTo,
        tags = this.tags,
        metadata = this.metadata?.let {
            com.ai.homework.dto.TicketMetadataDto(
                source = it.source,
                browser = it.browser,
                deviceType = it.deviceType
            )
        }
    )
}