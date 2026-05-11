package com.ai.homework.validator

import com.ai.homework.dto.TicketCreateRequest
import com.ai.homework.model.Ticket
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class TicketValidator {
    companion object {
        private val EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )
        private const val SUBJECT_MIN_LENGTH = 1
        private const val SUBJECT_MAX_LENGTH = 200
        private const val DESCRIPTION_MIN_LENGTH = 10
        private const val DESCRIPTION_MAX_LENGTH = 2000
    }

    fun validate(request: TicketCreateRequest): List<String> {
        val errors = mutableListOf<String>()

        if (request.customerId.isBlank()) {
            errors.add("customer_id: required field cannot be empty")
        }

        if (request.customerEmail.isBlank()) {
            errors.add("customer_email: required field cannot be empty")
        } else if (!isValidEmail(request.customerEmail)) {
            errors.add("customer_email: invalid email format")
        }

        if (request.customerName.isBlank()) {
            errors.add("customer_name: required field cannot be empty")
        }

        if (request.subject.isBlank()) {
            errors.add("subject: required field cannot be empty")
        } else if (request.subject.length < SUBJECT_MIN_LENGTH || request.subject.length > SUBJECT_MAX_LENGTH) {
            errors.add("subject: must be between $SUBJECT_MIN_LENGTH and $SUBJECT_MAX_LENGTH characters")
        }

        if (request.description.isBlank()) {
            errors.add("description: required field cannot be empty")
        } else if (request.description.length < DESCRIPTION_MIN_LENGTH || request.description.length > DESCRIPTION_MAX_LENGTH) {
            errors.add("description: must be between $DESCRIPTION_MIN_LENGTH and $DESCRIPTION_MAX_LENGTH characters")
        }

        return errors
    }

    fun validate(ticket: Ticket): List<String> {
        val errors = mutableListOf<String>()

        if (ticket.customerId.isBlank()) {
            errors.add("customer_id: required field cannot be empty")
        }

        if (ticket.customerEmail.isBlank()) {
            errors.add("customer_email: required field cannot be empty")
        } else if (!isValidEmail(ticket.customerEmail)) {
            errors.add("customer_email: invalid email format")
        }

        if (ticket.customerName.isBlank()) {
            errors.add("customer_name: required field cannot be empty")
        }

        if (ticket.subject.isBlank()) {
            errors.add("subject: required field cannot be empty")
        } else if (ticket.subject.length < SUBJECT_MIN_LENGTH || ticket.subject.length > SUBJECT_MAX_LENGTH) {
            errors.add("subject: must be between $SUBJECT_MIN_LENGTH and $SUBJECT_MAX_LENGTH characters")
        }

        if (ticket.description.isBlank()) {
            errors.add("description: required field cannot be empty")
        } else if (ticket.description.length < DESCRIPTION_MIN_LENGTH || ticket.description.length > DESCRIPTION_MAX_LENGTH) {
            errors.add("description: must be between $DESCRIPTION_MIN_LENGTH and $DESCRIPTION_MAX_LENGTH characters")
        }

        return errors
    }

    private fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }
}