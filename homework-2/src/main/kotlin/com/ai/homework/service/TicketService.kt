package com.ai.homework.service

import com.ai.homework.dto.TicketCreateRequest
import com.ai.homework.dto.TicketResponse
import com.ai.homework.dto.TicketUpdateRequest
import com.ai.homework.model.DeviceType
import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketMetadata
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketStatus
import com.ai.homework.validator.TicketValidator
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class TicketService(private val validator: TicketValidator) {
    private val tickets = ConcurrentHashMap<String, Ticket>()

    fun createTicket(request: TicketCreateRequest): Ticket {
        val ticket = Ticket(
            customerId = request.customerId,
            customerEmail = request.customerEmail,
            customerName = request.customerName,
            subject = request.subject,
            description = request.description,
            category = request.category ?: TicketCategory.OTHER,
            priority = request.priority ?: TicketPriority.MEDIUM,
            status = TicketStatus.NEW,
            tags = request.tags,
            metadata = request.metadata?.let {
                TicketMetadata(
                    source = it.source,
                    browser = it.browser,
                    deviceType = it.deviceType
                )
            }
        )
        tickets[ticket.id] = ticket
        return ticket
    }

    fun storeTicket(ticket: Ticket): Ticket {
        tickets[ticket.id] = ticket
        return ticket
    }

    fun getTicket(id: String): Ticket? = tickets[id]

    fun getAllTickets(): List<Ticket> = tickets.values.toList()

    fun listTickets(
        category: TicketCategory? = null,
        priority: TicketPriority? = null,
        status: TicketStatus? = null,
        customerId: String? = null
    ): List<Ticket> {
        return tickets.values.filter { ticket ->
            (category == null || ticket.category == category) &&
                (priority == null || ticket.priority == priority) &&
                (status == null || ticket.status == status) &&
                (customerId == null || ticket.customerId == customerId)
        }
    }

    fun updateTicket(id: String, request: TicketUpdateRequest): Ticket? {
        val ticket = tickets[id] ?: return null

        val updatedTicket = ticket.copy(
            status = request.status ?: ticket.status,
            category = request.category ?: ticket.category,
            priority = request.priority ?: ticket.priority,
            assignedTo = request.assignedTo ?: ticket.assignedTo,
            tags = request.tags ?: ticket.tags,
            updatedAt = LocalDateTime.now(),
            resolvedAt = if (request.status == TicketStatus.RESOLVED && ticket.resolvedAt == null) {
                LocalDateTime.now()
            } else {
                ticket.resolvedAt
            }
        )

        tickets[id] = updatedTicket
        return updatedTicket
    }

    fun deleteTicket(id: String): Boolean = tickets.remove(id) != null

    fun ticketExists(id: String): Boolean = tickets.containsKey(id)

    fun countTickets(): Int = tickets.size
}