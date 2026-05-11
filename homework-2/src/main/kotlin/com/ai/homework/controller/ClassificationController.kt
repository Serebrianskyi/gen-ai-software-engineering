package com.ai.homework.controller

import com.ai.homework.dto.ErrorResponse
import com.ai.homework.service.ClassificationService
import com.ai.homework.service.TicketService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tickets")
class ClassificationController(
    private val classificationService: ClassificationService,
    private val ticketService: TicketService,
) {

    @PostMapping("/{id}/auto-classify")
    fun autoClassify(@PathVariable id: String): ResponseEntity<Any> {
        val ticket = ticketService.getTicket(id)
        return if (ticket == null) {
            ResponseEntity(
                ErrorResponse(
                    error = "Not Found",
                    message = "Ticket with id $id not found",
                    path = "/tickets/$id/auto-classify",
                ),
                HttpStatus.NOT_FOUND,
            )
        } else {
            val classificationResult = classificationService.classify(ticket)
            ResponseEntity(classificationResult, HttpStatus.OK)
        }
    }
}
