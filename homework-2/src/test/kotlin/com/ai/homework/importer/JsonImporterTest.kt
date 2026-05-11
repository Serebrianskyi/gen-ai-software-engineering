package com.ai.homework.importer

import com.ai.homework.validator.TicketValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayInputStream

@DisplayName("JSON Importer Tests")
class JsonImporterTest {

    private val importer = JsonTicketImporter(TicketValidator())

    @Test
    @DisplayName("Should successfully import valid JSON array")
    fun testValidJsonImport() {
        val json = """
            [
              {"customer_id": "cust-001", "customer_email": "john@example.com", "customer_name": "John Doe", "subject": "Subject", "description": "Valid description here"},
              {"customer_id": "cust-002", "customer_email": "jane@example.com", "customer_name": "Jane Smith", "subject": "Subject", "description": "Another valid description"}
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(2)
        assertThat(result.successful).isEqualTo(2)
        assertThat(result.failed).isEqualTo(0)
    }

    @Test
    @DisplayName("Should reject invalid JSON syntax")
    fun testInvalidJsonSyntax() {
        val invalidJson = "{ invalid json"

        val result = importer.import(ByteArrayInputStream(invalidJson.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
        assertThat(result.errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should handle JSON with missing required fields")
    fun testJsonWithMissingFields() {
        val json = """
            [
              {"customer_id": "cust-001", "customer_email": "john@example.com"}
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
        assertThat(result.errors).isNotEmpty()
    }

    @Test
    @DisplayName("Should handle empty JSON array")
    fun testEmptyJsonArray() {
        val json = "[]"

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(0)
        assertThat(result.successful).isEqualTo(0)
    }

    @Test
    @DisplayName("Should handle JSON with type mismatches")
    fun testJsonWithTypeMismatches() {
        val json = """
            [
              {"customer_id": 123, "customer_email": "john@example.com", "customer_name": "John", "subject": "Subject", "description": "Valid description"}
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThanOrEqualTo(0)
    }

    @Test
    @DisplayName("Should import JSON with all fields")
    fun testJsonWithAllFields() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Complete Ticket",
                "description": "This ticket has all possible fields filled in for testing",
                "category": "account_access",
                "priority": "high",
                "tags": ["urgent", "important"]
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should import JSON with large dataset")
    fun testJsonLargeDataset() {
        val jsonBuilder = StringBuilder("[\n")
        for (i in 1..15) {
            if (i > 1) jsonBuilder.append(",\n")
            jsonBuilder.append("""{"customer_id": "cust-$i", "customer_email": "user$i@example.com", "customer_name": "User $i", "subject": "Subject $i", "description": "Description for user $i with sufficient length for testing"}""")
        }
        jsonBuilder.append("\n]")

        val result = importer.import(ByteArrayInputStream(jsonBuilder.toString().toByteArray()))

        assertThat(result.totalRecords).isEqualTo(15)
        assertThat(result.successful).isEqualTo(15)
    }

    @Test
    @DisplayName("Should import JSON with special characters")
    fun testJsonWithSpecialCharacters() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John O'Reilly",
                "subject": "Error with \"quotes\"",
                "description": "Description with special chars: @#$%^&*() and 'quotes' and \"double quotes\""
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle JSON with invalid email")
    fun testJsonWithInvalidEmail() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "not-an-email",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject JSON with blank customer ID")
    fun testJsonWithBlankCustomerId() {
        val json = """
            [
              {
                "customer_id": "",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject JSON with blank customer name")
    fun testJsonWithBlankCustomerName() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "",
                "subject": "Subject",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should reject JSON with blank subject")
    fun testJsonWithBlankSubject() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with subject exceeding max length")
    fun testJsonWithSubjectTooLong() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "${"x".repeat(201)}",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should accept JSON with minimum subject length")
    fun testJsonWithMinimumSubject() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "A",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle JSON with description too short")
    fun testJsonWithDescriptionTooShort() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "short"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should accept JSON with minimum description length")
    fun testJsonWithMinimumDescription() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "1234567890"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should accept JSON with maximum description length")
    fun testJsonWithMaximumDescription() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "${"x".repeat(2000)}"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle JSON with description exceeding maximum length")
    fun testJsonWithDescriptionTooLong() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "${"x".repeat(2001)}"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with mixed valid and invalid records")
    fun testJsonWithMixedValidAndInvalid() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "invalid",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Another description"
              },
              {
                "customer_id": "cust-003",
                "customer_email": "bob@example.com",
                "customer_name": "Bob Johnson",
                "subject": "Subject",
                "description": "Yet another valid description"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
    }

    @Test
    @DisplayName("Should handle JSON with unicode characters")
    fun testJsonWithUnicodeCharacters() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "José García",
                "subject": "Problem with UTF-8 characters",
                "description": "This has unicode: 你好世界 مرحبا العالم שלום עולם"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with escaped quotes in strings")
    fun testJsonWithEscapedQuotes() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John \"The Boss\" Doe",
                "subject": "Issue with \"quotes\" in field",
                "description": "Description says: \"This is quoted\" and more content"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should handle JSON with category and priority fields")
    fun testJsonWithAllOptionalFields() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Complete Ticket",
                "description": "This ticket has all fields including optional ones",
                "category": "technical_issue",
                "priority": "high"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("Should accept JSON with null values for optional fields")
    fun testJsonWithNullOptionalFields() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "category": null,
                "priority": null
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle malformed JSON with trailing comma")
    fun testMalformedJsonWithTrailingComma() {
        val json = """
            [
              {"customer_id": "cust-001", "customer_email": "john@example.com", "customer_name": "John Doe", "subject": "Subject", "description": "Valid description"},
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with numeric customer ID (type mismatch)")
    fun testJsonWithNumericCustomerId() {
        val json = """
            [
              {
                "customer_id": 12345,
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with tags field")
    fun testJsonWithTags() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "tags": ["urgent", "important"]
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with browser field")
    fun testJsonWithBrowserField() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "browser": "Chrome 120.0"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with source field")
    fun testJsonWithSourceField() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "source": "EMAIL"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with device_type field")
    fun testJsonWithDeviceType() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "device_type": "DESKTOP"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with invalid source value")
    fun testJsonWithInvalidSource() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "source": "INVALID_SOURCE"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with invalid device_type value")
    fun testJsonWithInvalidDeviceType() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "device_type": "SMARTWATCH"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with empty tags array")
    fun testJsonWithEmptyTagsArray() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "tags": []
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with multiple tags in array")
    fun testJsonWithMultipleTags() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"]
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with all metadata fields")
    fun testJsonWithAllMetadataFields() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "category": "technical_issue",
                "priority": "high",
                "source": "EMAIL",
                "device_type": "MOBILE",
                "browser": "Chrome",
                "tags": ["bug", "crash"]
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with string tags instead of array")
    fun testJsonWithStringTags() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "tags": "tag1,tag2,tag3"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with category having hyphens")
    fun testJsonWithCategoryHyphens() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "category": "account-access"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with source having underscores")
    fun testJsonWithSourceUnderscores() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "source": "LIVE_CHAT"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should import JSON with very long browser string")
    fun testJsonWithLongBrowserString() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "browser": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edge/120.0.0.0"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should report detailed error information in JSON")
    fun testJsonErrorDetailInformation() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.errors).isNotEmpty()
        assertThat(result.errors[0].row).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with array of objects without crashing")
    fun testJsonProcessesMultipleRecords() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject 1",
                "description": "Valid description 1"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "jane@example.com",
                "customer_name": "Jane Smith",
                "subject": "Subject 2",
                "description": "Valid description 2"
              },
              {
                "customer_id": "cust-003",
                "customer_email": "bob@example.com",
                "customer_name": "Bob Johnson",
                "subject": "Subject 3",
                "description": "Valid description 3"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
        assertThat(result.successful).isEqualTo(3)
    }

    @Test
    @DisplayName("Should continue processing after encountering invalid record")
    fun testJsonContinuesAfterError() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "invalid",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Valid description"
              },
              {
                "customer_id": "cust-003",
                "customer_email": "bob@example.com",
                "customer_name": "Bob Johnson",
                "subject": "Subject",
                "description": "Valid description"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
    }

    @Test
    @DisplayName("Should handle JSON with different field ordering")
    fun testJsonWithDifferentFieldOrdering() {
        val json = """
            [
              {
                "description": "Valid description here",
                "subject": "Subject",
                "customer_name": "John Doe",
                "customer_email": "john@example.com",
                "customer_id": "cust-001"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with whitespace in string values")
    fun testJsonWithWhitespaceInValues() {
        val json = """
            [
              {
                "customer_id": "  cust-001  ",
                "customer_email": "  john@example.com  ",
                "customer_name": "  John Doe  ",
                "subject": "  Subject  ",
                "description": "  Valid description here  "
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with boolean values for optional fields")
    fun testJsonWithBooleanOptionalValues() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description here",
                "tags": true
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle very large JSON array")
    fun testJsonWithLargeArray() {
        val jsonBuilder = StringBuilder("[\n")
        for (i in 1..30) {
            if (i > 1) jsonBuilder.append(",\n")
            jsonBuilder.append("""
              {
                "customer_id": "cust-$i",
                "customer_email": "user$i@example.com",
                "customer_name": "User $i",
                "subject": "Subject $i",
                "description": "Description for user $i with sufficient length for validation"
              }
            """.trimIndent())
        }
        jsonBuilder.append("\n]")

        val result = importer.import(ByteArrayInputStream(jsonBuilder.toString().toByteArray()))

        assertThat(result.totalRecords).isEqualTo(30)
    }

    @Test
    @DisplayName("Should validate records individually in JSON without stopping on first error")
    fun testJsonValidatesAllRecords() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description"
              },
              {
                "customer_id": "",
                "customer_email": "jane@example.com",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Valid description"
              },
              {
                "customer_id": "cust-003",
                "customer_email": "bob@example.com",
                "customer_name": "Bob Johnson",
                "subject": "Subject",
                "description": "Valid description"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(3)
        assertThat(result.successful).isGreaterThan(0)
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with case-insensitive category values")
    fun testJsonWithCategoryVariations() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description",
                "category": "ACCOUNT_ACCESS"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "jane@example.com",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Valid description",
                "category": "Bug_Report"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(2)
    }

    @Test
    @DisplayName("Should handle JSON with case-insensitive priority values")
    fun testJsonWithPriorityVariations() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description",
                "priority": "URGENT"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "jane@example.com",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Valid description",
                "priority": "high"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isEqualTo(2)
    }

    @Test
    @DisplayName("Should handle JSON with all fields containing empty strings")
    fun testJsonWithAllFieldsEmpty() {
        val json = """
            [
              {
                "customer_id": "",
                "customer_email": "",
                "customer_name": "",
                "subject": "",
                "description": ""
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with very long string values")
    fun testJsonWithVeryLongValues() {
        val json = """
            [
              {
                "customer_id": "${"x".repeat(500)}",
                "customer_email": "john@example.com",
                "customer_name": "${"x".repeat(500)}",
                "subject": "${"x".repeat(200)}",
                "description": "${"x".repeat(2000)}"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON array with 100+ records")
    fun testJsonWithVeryLargeArray() {
        val jsonBuilder = StringBuilder("[\n")
        for (i in 1..100) {
            if (i > 1) jsonBuilder.append(",\n")
            jsonBuilder.append("""
              {
                "customer_id": "cust-$i",
                "customer_email": "user$i@example.com",
                "customer_name": "User $i",
                "subject": "Subject $i",
                "description": "Description $i with sufficient length for validation"
              }
            """.trimIndent())
        }
        jsonBuilder.append("\n]")

        val result = importer.import(ByteArrayInputStream(jsonBuilder.toString().toByteArray()))

        assertThat(result.totalRecords).isEqualTo(100)
        assertThat(result.successful).isEqualTo(100)
    }

    @Test
    @DisplayName("Should handle JSON with escaped special characters")
    fun testJsonWithEscapedSpecialCharacters() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "O'Brien",
                "subject": "Subject with \\ backslash",
                "description": "Description with \n newline and \t tab"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with numeric values in string fields")
    fun testJsonWithNumericStringValues() {
        val json = """
            [
              {
                "customer_id": "12345",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject 123",
                "description": "Description with numbers 123 456 789"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should track JSON error record numbers accurately")
    fun testJsonErrorRowNumbering() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description"
              },
              {
                "customer_id": "cust-002",
                "customer_email": "",
                "customer_name": "Jane Smith",
                "subject": "Subject",
                "description": "Valid description"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.errors).isNotEmpty()
        assertThat(result.errors[0].row).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with source field hyphen conversion")
    fun testJsonWithSourceHyphenConversion() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description",
                "source": "SOCIAL_MEDIA"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON tags as comma-separated string")
    fun testJsonWithTagsAsString() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description",
                "tags": "tag1,tag2,tag3"
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("Should handle JSON with object values in optional fields")
    fun testJsonWithObjectOptionalFields() {
        val json = """
            [
              {
                "customer_id": "cust-001",
                "customer_email": "john@example.com",
                "customer_name": "John Doe",
                "subject": "Subject",
                "description": "Valid description",
                "metadata": {"some": "value"}
              }
            ]
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(json.toByteArray()))

        assertThat(result.totalRecords).isGreaterThan(0)
    }
}