package com.ai.homework.service

import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("Classification Service Tests")
class ClassificationTest {

    @Autowired
    private lateinit var classificationService: ClassificationService

    @Test
    @DisplayName("Should classify account_access category")
    fun testAccountAccessClassification() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Cannot login",
            description = "I cannot access my account after password reset"
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.ACCOUNT_ACCESS)
        assertThat(result.keywordsFound).isNotEmpty()
    }

    @Test
    @DisplayName("Should classify technical_issue category")
    fun testTechnicalIssueClassification() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Application crashes",
            description = "The app crashes when I try to upload a file. Getting an error."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.TECHNICAL_ISSUE)
    }

    @Test
    @DisplayName("Should classify billing_question category")
    fun testBillingQuestionClassification() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Double charge",
            description = "I was charged twice for my subscription this month. Need a refund."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.BILLING_QUESTION)
    }

    @Test
    @DisplayName("Should classify feature_request category")
    fun testFeatureRequestClassification() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Feature enhancement",
            description = "I would like to request a new feature. Please add dark mode to the app."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.FEATURE_REQUEST)
    }

    @Test
    @DisplayName("Should classify bug_report category")
    fun testBugReportClassification() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Bug found",
            description = "Found a bug. Steps to reproduce: 1. Do this 2. Then that. Expected: it should work. Actual: it fails."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.BUG_REPORT)
    }

    @Test
    @DisplayName("Should assign urgent priority for critical issues")
    fun testUrgentPriorityAssignment() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Critical issue",
            description = "Production down. Critical error. Cannot access system immediately."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.priority).isEqualTo(TicketPriority.URGENT)
    }

    @Test
    @DisplayName("Should assign high priority for blocking issues")
    fun testHighPriorityAssignment() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Blocking issue - important",
            description = "This is blocking my work. Important fix needed now. Cannot continue without resolution."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.priority).isEqualTo(TicketPriority.HIGH)
    }

    @Test
    @DisplayName("Should assign low priority for minor issues")
    fun testLowPriorityAssignment() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Minor cosmetic issue",
            description = "This is just a cosmetic minor suggestion. Nice to have."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.priority).isEqualTo(TicketPriority.LOW)
    }

    @Test
    @DisplayName("Should calculate confidence score")
    fun testConfidenceScore() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Cannot login to account",
            description = "I cannot access my account. Password reset failed. Authentication error."
        )

        val result = classificationService.classify(ticket)

        assertThat(result.confidence).isGreaterThanOrEqualTo(0.0)
        assertThat(result.confidence).isLessThanOrEqualTo(1.0)
    }

    @Test
    @DisplayName("Should default to medium priority when no priority keywords found")
    fun testDefaultMediumPriority() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "General inquiry",
            description = "I have a question about the general features of the application"
        )

        val result = classificationService.classify(ticket)

        assertThat(result.priority).isEqualTo(TicketPriority.MEDIUM)
    }

    @Test
    @DisplayName("Should match keywords in classification results")
    fun testKeywordMatching() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Login password reset",
            description = "Cannot access account authentication error password invalid"
        )

        val result = classificationService.classify(ticket)

        assertThat(result.keywordsFound).isNotEmpty()
        assertThat(result.confidence).isGreaterThan(0.0)
    }

    @Test
    @DisplayName("Should generate reasoning message for classifications")
    fun testReasoningGeneration() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Bug in the system",
            description = "Found a bug when trying to use the feature"
        )

        val result = classificationService.classify(ticket)

        assertThat(result.reasoning).isNotBlank()
    }

    @Test
    @DisplayName("Should classify billing questions")
    fun testBillingQuestionDetection() {
        val ticket = Ticket(
            customerId = "cust-123",
            customerEmail = "test@example.com",
            customerName = "Test User",
            subject = "Invoice inquiry",
            description = "I have questions about the invoice and payment for my subscription"
        )

        val result = classificationService.classify(ticket)

        assertThat(result.category).isEqualTo(TicketCategory.BILLING_QUESTION)
    }
}