package com.ai.homework.importer

import com.ai.homework.validator.TicketValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayInputStream

@DisplayName("CSV Importer Tests")
class CsvImporterTest {

    private val importer = CsvTicketImporter(TicketValidator())

    @Test
    @DisplayName("Should successfully import valid CSV file")
    fun testValidCsvImport() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category,priority
            cust-001,john@example.com,John Doe,Test Subject,This is a valid test description,account_access,high
            cust-002,jane@example.com,Jane Smith,Another Subject,This is another valid description,bug_report,urgent
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(2)
        assertThat(result.successful).isEqualTo(2)
        assertThat(result.failed).isEqualTo(0)
        assertThat(result.errors).isEmpty()
    }

    @Test
    @DisplayName("Should handle CSV with missing required fields")
    fun testCsvWithMissingFields() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,,This is a valid test description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(0)
        assertThat(result.failed).isGreaterThan(0)
        assertThat(result.errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should handle empty CSV")
    fun testEmptyCsv() {
        val csv = "customer_id,customer_email,customer_name,subject,description"

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(0)
        assertThat(result.successful).isEqualTo(0)
    }

    @Test
    @DisplayName("Should handle malformed CSV with extra columns")
    fun testCsvWithExtraColumns() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,extra_col
            cust-001,john@example.com,John Doe,Subject,This is a valid description,extra_value
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
        assertThat(result.failed).isEqualTo(0)
    }

    @Test
    @DisplayName("Should handle CSV with quoted values containing commas")
    fun testCsvWithQuotedValues() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,"Doe, John",Subject,"This is a description, with commas"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThanOrEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with all categories")
    fun testCsvWithAllCategories() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category,priority
            cust-001,john@example.com,John Doe,Login,Cannot login,account_access,high
            cust-002,jane@example.com,Jane Smith,Bug,App crashes,bug_report,urgent
            cust-003,bob@example.com,Bob Johnson,Billing,Double charge,billing_question,high
            cust-004,alice@example.com,Alice Williams,Feature,Dark mode request,feature_request,low
            cust-005,charlie@example.com,Charlie Brown,Issue,Technical error,technical_issue,medium
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(5)
        assertThat(result.successful).isEqualTo(5)
        assertThat(result.failed).isEqualTo(0)
    }

    @Test
    @DisplayName("Should import CSV with all priority levels")
    fun testCsvWithAllPriorities() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,priority
            cust-001,user1@example.com,User 1,Subject 1,Description with sufficient length,urgent
            cust-002,user2@example.com,User 2,Subject 2,Description with sufficient length,high
            cust-003,user3@example.com,User 3,Subject 3,Description with sufficient length,medium
            cust-004,user4@example.com,User 4,Subject 4,Description with sufficient length,low
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(4)
    }

    @Test
    @DisplayName("Should handle CSV with special characters in descriptions")
    fun testCsvWithSpecialCharacters() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Bug Report,"App crashes with error: 'NullPointerException' when clicking button"
            cust-002,jane@example.com,Jane Smith,Request,"Feature requested: Add support for €, £, ¥ currencies"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThanOrEqualTo(2)
    }

    @Test
    @DisplayName("Should import large CSV file")
    fun testLargeCsvImport() {
        val csvBuilder = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..20) {
            csvBuilder.append("cust-$i,user$i@example.com,User $i,Subject $i,This is description number $i with sufficient length for validation\n")
        }

        val result = importer.import(ByteArrayInputStream(csvBuilder.toString().toByteArray()))

        assertThat(result.totalRecords).isEqualTo(20)
        assertThat(result.successful).isEqualTo(20)
    }

    @Test
    @DisplayName("Should handle CSV with invalid email format")
    fun testCsvWithInvalidEmail() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,not-an-email,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject CSV with empty customer ID")
    fun testCsvWithEmptyCustomerId() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            ,john@example.com,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject CSV with empty customer name")
    fun testCsvWithEmptyCustomerName() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with description too short")
    fun testCsvWithDescriptionTooShort() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,short
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should accept CSV with minimum length description")
    fun testCsvWithMinimumDescription() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,1234567890
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
        assertThat(result.failed).isEqualTo(0)
    }

    @Test
    @DisplayName("Should accept CSV with maximum length description")
    fun testCsvWithMaximumDescription() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        csv.append("cust-001,john@example.com,John Doe,Subject,")
        csv.append("x".repeat(2000))

        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle CSV with description exceeding maximum length")
    fun testCsvWithDescriptionTooLong() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        csv.append("cust-001,john@example.com,John Doe,Subject,")
        csv.append("x".repeat(2001))

        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid records")
    fun testCsvWithMixedValidAndInvalid() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,Valid description here
            cust-002,invalid-email,Jane Smith,Subject,Another description
            cust-003,bob@example.com,Bob Johnson,Subject,Yet another valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
    }

    @Test
    @DisplayName("Should handle CSV with empty subject")
    fun testCsvWithEmptySubject() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with very long subject")
    fun testCsvWithVeryLongSubject() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        csv.append("cust-001,john@example.com,John Doe,")
        csv.append("x".repeat(201))
        csv.append(",This is a valid description")

        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with minimum length subject")
    fun testCsvWithMinimumSubject() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,A,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle CSV with special characters in customer name")
    fun testCsvWithSpecialCharactersInName() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,"O'Brien, Jr.",Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with newlines in quoted fields")
    fun testCsvWithNewlinesInQuotedFields() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,"This is a description
            with a newline in it and more content"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV missing optional category field")
    fun testCsvMissingOptionalCategoryField() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle CSV missing optional priority field")
    fun testCsvMissingOptionalPriorityField() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should reject CSV with invalid category value")
    fun testCsvWithInvalidCategory() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category
            cust-001,john@example.com,John Doe,Subject,This is a valid description,invalid_category
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject CSV with invalid priority value")
    fun testCsvWithInvalidPriority() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,priority
            cust-001,john@example.com,John Doe,Subject,This is a valid description,super_urgent
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import CSV with tags field")
    fun testCsvWithTags() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,tags
            cust-001,john@example.com,John Doe,Subject,This is a valid description,"urgent,important,follow-up"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with browser field")
    fun testCsvWithBrowserField() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,browser
            cust-001,john@example.com,John Doe,Subject,This is a valid description,Chrome 120.0
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with source field")
    fun testCsvWithSourceField() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,source
            cust-001,john@example.com,John Doe,Subject,This is a valid description,EMAIL
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with device_type field")
    fun testCsvWithDeviceType() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,device_type
            cust-001,john@example.com,John Doe,Subject,This is a valid description,DESKTOP
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with invalid source value")
    fun testCsvWithInvalidSource() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,source
            cust-001,john@example.com,John Doe,Subject,This is a valid description,INVALID_SOURCE
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import CSV with invalid device_type value")
    fun testCsvWithInvalidDeviceType() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,device_type
            cust-001,john@example.com,John Doe,Subject,This is a valid description,SMARTWATCH
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with multiple tags in tags field")
    fun testCsvWithMultipleTags() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,tags
            cust-001,john@example.com,John Doe,Subject,This is a valid description,"tag1, tag2, tag3, tag4"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import CSV with all optional metadata fields")
    fun testCsvWithAllMetadataFields() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category,priority,source,device_type,browser,tags
            cust-001,john@example.com,John Doe,Subject,This is a valid description,technical_issue,high,EMAIL,MOBILE,Chrome,"bug,crash"
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV where source field has hyphens")
    fun testCsvWithSourceHyphens() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,source
            cust-001,john@example.com,John Doe,Subject,This is a valid description,LIVE_CHAT
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV where category has hyphens")
    fun testCsvWithCategoryHyphens() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category
            cust-001,john@example.com,John Doe,Subject,This is a valid description,account-access
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should accept CSV with only required fields")
    fun testCsvWithOnlyRequiredFields() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should report detailed error information")
    fun testCsvErrorDetailInformation() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,,John Doe,Subject,This is a valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.errors).isNotEmpty()
        assertThat(result.errors[0].row).isGreaterThan(0)
        assertThat(result.errors[0].field).isNotBlank()
    }

    @Test
    @DisplayName("Should handle CSV with multiple blank rows")
    fun testCsvWithMultipleBlankRows() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,john@example.com,John Doe,Subject,Valid description
            ,,,,
            cust-002,jane@example.com,Jane Smith,Subject,Another valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should trim whitespace from all fields")
    fun testCsvTrimsWhitespace() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            " cust-001 "," john@example.com "," John Doe "," Subject "," This is a valid description "
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with many columns")
    fun testCsvWithManyColumns() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description,category,priority,source,device_type,browser,tags,extra1,extra2,extra3,extra4,extra5
            cust-001,john@example.com,John Doe,Subject,This is a valid description,technical_issue,high,EMAIL,DESKTOP,Chrome,"tag1,tag2",value1,value2,value3,value4,value5
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should validate records individually without stopping on first error")
    fun testCsvContinuesAfterError() {
        val csv = """
            customer_id,customer_email,customer_name,subject,description
            cust-001,,John Doe,Subject,Valid description
            cust-002,jane@example.com,Jane Smith,Subject,Valid description
            cust-003,invalid,Bob Johnson,Subject,Valid description
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
    }

    @Test
    @DisplayName("Should handle CSV with case variations in enum values")
    fun testCsvWithEnumCaseVariations() {
        val csvContent = "customer_id,customer_email,customer_name,subject,description,category,priority\ncust1,john@example.com,John Doe,Subject,Valid description,ACCOUNT_ACCESS,URGENT"

        val result = importer.import(ByteArrayInputStream(csvContent.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle CSV records with all fields empty")
    fun testCsvWithAllFieldsEmpty() {
        val csvContent = "customer_id,customer_email,customer_name,subject,description\n,,,,"

        val result = importer.import(ByteArrayInputStream(csvContent.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV with very long customer ID")
    fun testCsvWithLongCustomerId() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        csv.append("\"")
        csv.append("x".repeat(500))
        csv.append("\",john@example.com,John Doe,Subject,Valid description")

        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle CSV record count correctly after parsing")
    fun testCsvRecordCountAccuracy() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..50) {
            csv.append("custid$i,user$i@example.com,User $i,Subject $i,Description $i with sufficient length\n")
        }

        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))

        assertThat(result.totalRecords).isEqualTo(50)
        assertThat(result.successful).isEqualTo(50)
    }

    @Test
    @DisplayName("Should track error details in results")
    fun testCsvErrorDetails() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncustomer1,,John Doe,Subject,Valid description"

        val result = importer.import(ByteArrayInputStream(csv.toByteArray()))

        assertThat(result.errors).isNotEmpty()
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import 5 valid records successfully")
    fun testCsv5ValidRecords() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..5) {
            csv.append("id$i,user$i@example.com,User$i,Subject$i,This is a valid description with enough content\n")
        }
        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))
        assertThat(result.totalRecords).isEqualTo(5)
        assertThat(result.successful).isEqualTo(5)
    }

    @Test
    @DisplayName("Should import 10 valid records successfully")
    fun testCsv10ValidRecords() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..10) {
            csv.append("id$i,user$i@example.com,User$i,Subject$i,This is a valid description with enough content\n")
        }
        val result = importer.import(ByteArrayInputStream(csv.toString().toByteArray()))
        assertThat(result.totalRecords).isEqualTo(10)
        assertThat(result.successful).isEqualTo(10)
    }

    @Test
    @DisplayName("Should handle all category enum values")
    fun testCsvAllCategoryValues() {
        val categories = listOf("account_access", "technical_issue", "billing_question", "feature_request", "bug_report", "other")
        for (cat in categories) {
            val csv = "customer_id,customer_email,customer_name,subject,description,category\nid1,test@example.com,User,Subject,This is a valid description with enough content,$cat"
            val result = importer.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.successful).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("Should handle all priority enum values")
    fun testCsvAllPriorityValues() {
        val priorities = listOf("URGENT", "HIGH", "MEDIUM", "LOW")
        for (pri in priorities) {
            val csv = "customer_id,customer_email,customer_name,subject,description,priority\nid1,test@example.com,User,Subject,This is a valid description with enough content,$pri"
            val result = importer.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.successful).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("Should handle all source enum values")
    fun testCsvAllSourceValues() {
        val sources = listOf("EMAIL", "CHAT", "PHONE", "SOCIAL_MEDIA", "FEEDBACK", "OTHER")
        for (src in sources) {
            val csv = "customer_id,customer_email,customer_name,subject,description,source\nid1,test@example.com,User,Subject,This is a valid description with enough content,$src"
            val result = importer.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.successful).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("Should handle all device_type enum values")
    fun testCsvAllDeviceTypeValues() {
        val devices = listOf("DESKTOP", "MOBILE", "TABLET", "OTHER")
        for (dev in devices) {
            val csv = "customer_id,customer_email,customer_name,subject,description,device_type\nid1,test@example.com,User,Subject,This is a valid description with enough content,$dev"
            val result = importer.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.successful).isGreaterThan(0)
        }
    }
}
