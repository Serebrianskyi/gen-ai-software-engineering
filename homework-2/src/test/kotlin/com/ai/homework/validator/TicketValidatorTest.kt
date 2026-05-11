package com.ai.homework.validator

import com.ai.homework.dto.TicketCreateRequest
import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat

@DisplayName("Ticket Validator Tests")
class TicketValidatorTest {

    private val validator = TicketValidator()

    @Test
    @DisplayName("Should accept valid ticket request")
    fun testValidTicketRequest() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "This is a valid description with sufficient length"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject blank customer_id")
    fun testBlankCustomerId() {
        val request = TicketCreateRequest(
            customerId = "",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("customer_id")
    }

    @Test
    @DisplayName("Should reject invalid email format")
    fun testInvalidEmailFormat() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "invalid-email",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("email")
    }

    @Test
    @DisplayName("Should validate subject length constraints")
    fun testSubjectLengthValidation() {
        val tooShortRequest = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "",
            description = "Description with sufficient length"
        )

        val tooLongRequest = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "x".repeat(201),
            description = "Description with sufficient length"
        )

        val shortErrors = validator.validate(tooShortRequest)
        val longErrors = validator.validate(tooLongRequest)

        assertThat(shortErrors).isNotEmpty()
        assertThat(longErrors).isNotEmpty()
    }

    @Test
    @DisplayName("Should validate description length constraints")
    fun testDescriptionLengthValidation() {
        val tooShortRequest = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "short"
        )

        val tooLongRequest = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2001)
        )

        val shortErrors = validator.validate(tooShortRequest)
        val longErrors = validator.validate(tooLongRequest)

        assertThat(shortErrors).isNotEmpty()
        assertThat(longErrors).isNotEmpty()
    }

    @Test
    @DisplayName("Should accept minimum length description")
    fun testMinimumDescriptionLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(10)
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should accept maximum length description")
    fun testMaximumDescriptionLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "user@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2000)
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject multiple validation errors")
    fun testMultipleValidationErrors() {
        val request = TicketCreateRequest(
            customerId = "",
            customerEmail = "invalid",
            customerName = "",
            subject = "",
            description = "short"
        )

        val errors = validator.validate(request)
        assertThat(errors.size).isGreaterThan(3)
    }

    @Test
    @DisplayName("Should validate email with special characters")
    fun testEmailWithSpecialCharacters() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john+tag@example.co.uk",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject email without domain")
    fun testEmailWithoutDomain() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "johnatexample",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should accept email with dots in local part")
    fun testEmailWithDotsInLocalPart() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john.doe@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should accept email with hyphens in domain")
    fun testEmailWithHyphensInDomain() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@my-example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject email with space")
    fun testEmailWithSpace() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john doe@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should reject email with empty local part")
    fun testEmailWithEmptyLocalPart() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should reject email with empty domain")
    fun testEmailWithEmptyDomain() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should accept email with multiple dots in domain")
    fun testEmailWithMultipleDotsInDomain() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@mail.example.co.uk",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject blank customer ID with spaces")
    fun testBlankCustomerIdWithSpaces() {
        val request = TicketCreateRequest(
            customerId = "   ",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should reject blank customer name")
    fun testBlankCustomerName() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should reject blank customer email")
    fun testBlankCustomerEmail() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should accept subject with exactly 200 characters")
    fun testMaximumSubjectLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "x".repeat(200),
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject subject with 201 characters")
    fun testSubjectExceedsMaximumLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "x".repeat(201),
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("subject")
    }

    @Test
    @DisplayName("Should accept description with exactly 10 characters")
    fun testMinimumDescriptionLengthBoundary() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "1234567890"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject description with 9 characters")
    fun testDescriptionBelowMinimumLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "123456789"
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("description")
    }

    @Test
    @DisplayName("Should accept description with exactly 2000 characters")
    fun testMaximumDescriptionLengthBoundary() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2000)
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject description with 2001 characters")
    fun testDescriptionExceedsMaximumLength() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2001)
        )

        val errors = validator.validate(request)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("description")
    }

    @Test
    @DisplayName("Should validate all required fields are present")
    fun testAllRequiredFieldsValidation() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should collect all validation errors together")
    fun testAllValidationErrorsCollected() {
        val request = TicketCreateRequest(
            customerId = "",
            customerEmail = "invalid",
            customerName = "",
            subject = "",
            description = "short"
        )

        val errors = validator.validate(request)
        assertThat(errors.size).isGreaterThanOrEqualTo(3)
    }

    @Test
    @DisplayName("Should handle customer name with special characters")
    fun testCustomerNameWithSpecialCharacters() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "José García O'Brien",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should handle customer ID with special characters")
    fun testCustomerIdWithSpecialCharacters() {
        val request = TicketCreateRequest(
            customerId = "CUST-123-ABC",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should validate subject with leading/trailing spaces")
    fun testSubjectWithLeadingTrailingSpaces() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "  Subject  ",
            description = "Valid description here"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should validate description with special formatting")
    fun testDescriptionWithSpecialFormatting() {
        val request = TicketCreateRequest(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description:\n  - Point 1\n  - Point 2\n  - Point 3"
        )

        val errors = validator.validate(request)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should validate valid Ticket model")
    fun testValidTicketModel() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Test Subject",
            description = "This is a valid description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject Ticket with blank customer ID")
    fun testTicketWithBlankCustomerId() {
        val ticket = Ticket(
            customerId = "",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("customer_id")
    }

    @Test
    @DisplayName("Should reject Ticket with invalid email")
    fun testTicketWithInvalidEmail() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "invalid-email",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("email")
    }

    @Test
    @DisplayName("Should reject Ticket with blank customer name")
    fun testTicketWithBlankCustomerName() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("customer_name")
    }

    @Test
    @DisplayName("Should reject Ticket with blank subject")
    fun testTicketWithBlankSubject() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("subject")
    }

    @Test
    @DisplayName("Should reject Ticket with subject exceeding max length")
    fun testTicketWithSubjectTooLong() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "x".repeat(201),
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("subject")
    }

    @Test
    @DisplayName("Should reject Ticket with blank description")
    fun testTicketWithBlankDescription() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = ""
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("description")
    }

    @Test
    @DisplayName("Should reject Ticket with description too short")
    fun testTicketWithDescriptionTooShort() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "short"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("description")
    }

    @Test
    @DisplayName("Should accept Ticket with minimum description length")
    fun testTicketWithMinimumDescription() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "1234567890"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should accept Ticket with maximum description length")
    fun testTicketWithMaximumDescription() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2000)
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject Ticket with description exceeding maximum length")
    fun testTicketWithDescriptionTooLong() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Subject",
            description = "x".repeat(2001)
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("description")
    }

    @Test
    @DisplayName("Should validate Ticket with all valid fields")
    fun testTicketWithAllValidFields() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "john@example.com",
            customerName = "John Doe",
            subject = "Complete Ticket",
            description = "This ticket has all required fields with valid values",
            category = TicketCategory.TECHNICAL_ISSUE,
            priority = TicketPriority.HIGH,
            status = TicketStatus.NEW
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isEmpty()
    }

    @Test
    @DisplayName("Should reject Ticket with blank email in model")
    fun testTicketWithBlankEmailModel() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "",
            customerName = "John Doe",
            subject = "Subject",
            description = "Description with sufficient length"
        )

        val errors = validator.validate(ticket)
        assertThat(errors).isNotEmpty()
        assertThat(errors.toString()).contains("customer_email")
    }
}