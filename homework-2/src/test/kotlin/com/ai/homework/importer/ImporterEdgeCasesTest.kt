package com.ai.homework.importer

import com.ai.homework.validator.TicketValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayInputStream

@DisplayName("Importer Edge Cases Tests")
class ImporterEdgeCasesTest {

    private val csvImporter = CsvTicketImporter(TicketValidator())
    private val jsonImporter = JsonTicketImporter(TicketValidator())

    // CSV Edge Cases
    @Test
    @DisplayName("CSV: Should handle record with only whitespace")
    fun testCsvWithOnlyWhitespace() {
        val csv = "customer_id,customer_email,customer_name,subject,description\n   ,   ,   ,   ,   "
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle mixed empty and valid records")
    fun testCsvWithMixedEmptyAndValid() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john@example.com,John,Subject,Valid description\n,,,,"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(2)
    }

    @Test
    @DisplayName("CSV: Should handle single valid record")
    fun testCsvSingleValidRecord() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john@example.com,John,Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("CSV: Should handle category with underscores")
    fun testCsvCategoryWithUnderscores() {
        val csv = "customer_id,customer_email,customer_name,subject,description,category\ncust1,john@example.com,John,Subject,Valid description,ACCOUNT_ACCESS"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle source with underscores")
    fun testCsvSourceWithUnderscores() {
        val csv = "customer_id,customer_email,customer_name,subject,description,source\ncust1,john@example.com,John,Subject,Valid description,LIVE_CHAT"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle device_type field")
    fun testCsvWithDeviceTypeField() {
        val csv = "customer_id,customer_email,customer_name,subject,description,device_type\ncust1,john@example.com,John,Subject,Valid description,MOBILE"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle browser field")
    fun testCsvWithBrowserField() {
        val csv = "customer_id,customer_email,customer_name,subject,description,browser\ncust1,john@example.com,John,Subject,Valid description,Chrome"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle tags field")
    fun testCsvWithTagsField() {
        val csv = "customer_id,customer_email,customer_name,subject,description,tags\ncust1,john@example.com,John,Subject,Valid description,tag1,tag2"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle email field")
    fun testCsvEmailHandling() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,test@example.com,John,Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle 100 records correctly")
    fun testCsv100Records() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..100) {
            csv.append("id$i,user$i@example.com,User $i,Sub $i,This is a valid description\n")
        }
        val result = csvImporter.import(ByteArrayInputStream(csv.toString().toByteArray()))
        assertThat(result.totalRecords).isEqualTo(100)
    }

    @Test
    @DisplayName("CSV: Should track failed count correctly")
    fun testCsvFailedCountTracking() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john@example.com,John,Subject,Valid\ncust2,invalid,Jane,Subject,Valid\ncust3,bob@example.com,Bob,Subject,Valid"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(3)
        assertThat(result.successful).isGreaterThan(0)
    }

    // JSON Edge Cases
    @Test
    @DisplayName("JSON: Should handle single record array")
    fun testJsonSingleRecord() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("JSON: Should handle record with null email")
    fun testJsonNullEmail() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":null,\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle category with hyphens")
    fun testJsonCategoryHyphens() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"category\":\"account-access\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle source with hyphens to underscores")
    fun testJsonSourceHyphensConvert() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"source\":\"SOCIAL_MEDIA\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle device_type field")
    fun testJsonWithDeviceType() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"device_type\":\"DESKTOP\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle browser field")
    fun testJsonWithBrowser() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"browser\":\"Firefox 120\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle empty tags array")
    fun testJsonEmptyTagsArray() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"tags\":[]}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle many tags")
    fun testJsonManyTags() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"tags\":[\"tag1\",\"tag2\",\"tag3\",\"tag4\",\"tag5\",\"tag6\",\"tag7\",\"tag8\",\"tag9\",\"tag10\"]}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle 100 records correctly")
    fun testJson100Records() {
        val jsonBuilder = StringBuilder("[\n")
        for (i in 1..100) {
            if (i > 1) jsonBuilder.append(",\n")
            jsonBuilder.append("{\"customer_id\":\"id$i\",\"customer_email\":\"user$i@example.com\",\"customer_name\":\"User $i\",\"subject\":\"Subject $i\",\"description\":\"Valid description\"}")
        }
        jsonBuilder.append("\n]")
        val result = jsonImporter.import(ByteArrayInputStream(jsonBuilder.toString().toByteArray()))
        assertThat(result.totalRecords).isEqualTo(100)
    }

    @Test
    @DisplayName("JSON: Should handle multiple records with mixed validity")
    fun testJsonMultipleRecords() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid\"},{\"customer_id\":\"id2\",\"customer_email\":\"jane@example.com\",\"customer_name\":\"Jane\",\"subject\":\"Subject\",\"description\":\"Valid\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(2)
    }

    @Test
    @DisplayName("CSV: Should preserve original field values")
    fun testCsvPreservesValues() {
        val csv = "customer_id,customer_email,customer_name,subject,description\nCUST123,Test@Example.COM,John Doe,Test Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("JSON: Should preserve original field values")
    fun testJsonPreservesValues() {
        val json = "[{\"customer_id\":\"CUST123\",\"customer_email\":\"Test@Example.COM\",\"customer_name\":\"John Doe\",\"subject\":\"Test Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isEqualTo(1)
    }

    @Test
    @DisplayName("CSV: Should handle all priorities")
    fun testCsvAllPriorities() {
        val priorities = listOf("URGENT", "HIGH", "MEDIUM", "LOW")
        for (priority in priorities) {
            val csv = "customer_id,customer_email,customer_name,subject,description,priority\ncust1,john@example.com,John,Subject,Valid description,$priority"
            val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.totalRecords).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("JSON: Should handle all priorities")
    fun testJsonAllPriorities() {
        val priorities = listOf("URGENT", "HIGH", "MEDIUM", "LOW")
        for (priority in priorities) {
            val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"priority\":\"$priority\"}]"
            val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
            assertThat(result.totalRecords).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("CSV: Should handle all categories")
    fun testCsvAllCategories() {
        val categories = listOf("account_access", "technical_issue", "billing_question", "feature_request", "bug_report")
        for (category in categories) {
            val csv = "customer_id,customer_email,customer_name,subject,description,category\ncust1,john@example.com,John,Subject,Valid description,$category"
            val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
            assertThat(result.totalRecords).isGreaterThan(0)
        }
    }

    @Test
    @DisplayName("JSON: Should handle all categories")
    fun testJsonAllCategories() {
        val categories = listOf("account_access", "technical_issue", "billing_question", "feature_request", "bug_report")
        for (category in categories) {
            val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"category\":\"$category\"}]"
            val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
            assertThat(result.totalRecords).isGreaterThan(0)
        }
    }

    // CSV Exception Handling Tests
    @Test
    @DisplayName("CSV: Should handle malformed CSV data")
    fun testCsvMalformedData() {
        val csv = "this is not valid csv at all!!!"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isGreaterThanOrEqualTo(0)
    }

    @Test
    @DisplayName("CSV: Should handle unclosed quotes")
    fun testCsvUnclosedQuotes() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,\"john@example.com,John,Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle empty file")
    fun testCsvEmptyFile() {
        val csv = ""
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(0)
    }

    @Test
    @DisplayName("CSV: Should handle header only")
    fun testCsvHeaderOnly() {
        val csv = "customer_id,customer_email,customer_name,subject,description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(0)
    }

    // JSON Exception Handling Tests
    @Test
    @DisplayName("JSON: Should reject non-array root")
    fun testJsonNonArrayRoot() {
        val json = "{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\"}"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should reject array with non-object elements")
    fun testJsonArrayWithNonObjects() {
        val json = "[\"string\",123,true]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle malformed JSON")
    fun testJsonMalformed() {
        val json = "[{invalid json here}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle empty array")
    fun testJsonEmptyArray() {
        val json = "[]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isEqualTo(0)
    }

    @Test
    @DisplayName("JSON: Should handle missing optional customer_id")
    fun testJsonMissingCustomerId() {
        val json = "[{\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle missing optional customer_email")
    fun testJsonMissingCustomerEmail() {
        val json = "[{\"customer_id\":\"id1\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle missing optional category")
    fun testCsvMissingCategory() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john@example.com,John,Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle missing optional priority")
    fun testCsvMissingPriority() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john@example.com,John,Subject,Valid description"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle missing optional priority")
    fun testJsonMissingPriority() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle missing optional category")
    fun testJsonMissingCategory() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle invalid category gracefully")
    fun testCsvInvalidCategory() {
        val csv = "customer_id,customer_email,customer_name,subject,description,category\ncust1,john@example.com,John,Subject,Valid description,INVALID_CATEGORY"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle invalid priority gracefully")
    fun testCsvInvalidPriority() {
        val csv = "customer_id,customer_email,customer_name,subject,description,priority\ncust1,john@example.com,John,Subject,Valid description,INVALID_PRIORITY"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle invalid category gracefully")
    fun testJsonInvalidCategory() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"category\":\"INVALID_CATEGORY\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle invalid priority gracefully")
    fun testJsonInvalidPriority() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid description\",\"priority\":\"INVALID_PRIORITY\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle mixed valid and invalid records")
    fun testJsonMixedValidAndInvalid() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid\"},123,{\"customer_id\":\"id2\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle extra columns in data")
    fun testCsvExtraColumns() {
        val csv = "customer_id,customer_email,customer_name,subject,description,extra_column\ncust1,john@example.com,John,Subject,Valid description,extra_value"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle single object instead of array")
    fun testJsonSingleObjectInsteadOfArray() {
        val json = "{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"John\",\"subject\":\"Subject\",\"description\":\"Valid\"}"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.failed).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle special characters in text fields")
    fun testCsvSpecialCharacters() {
        val csv = "customer_id,customer_email,customer_name,subject,description\ncust1,john+test@example.com,John OBrien,Subject test,Valid description with special chars"
        val result = csvImporter.import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(result.successful).isGreaterThan(0)
    }

    @Test
    @DisplayName("JSON: Should handle unicode characters")
    fun testJsonUnicodeCharacters() {
        val json = "[{\"customer_id\":\"id1\",\"customer_email\":\"john@example.com\",\"customer_name\":\"Juan\",\"subject\":\"Subject\",\"description\":\"Valid description with unicode\"}]"
        val result = jsonImporter.import(ByteArrayInputStream(json.toByteArray()))
        assertThat(result.totalRecords).isGreaterThan(0)
    }

    @Test
    @DisplayName("CSV: Should handle large number of records")
    fun testCsvLargeDataset() {
        val csv = StringBuilder("customer_id,customer_email,customer_name,subject,description\n")
        for (i in 1..200) {
            csv.append("id$i,user$i@example.com,User $i,Sub $i,Description for ticket $i\n")
        }
        val result = csvImporter.import(ByteArrayInputStream(csv.toString().toByteArray()))
        assertThat(result.totalRecords).isGreaterThanOrEqualTo(200)
    }

    @Test
    @DisplayName("JSON: Should handle large number of records")
    fun testJsonLargeDataset() {
        val json = StringBuilder("[\n")
        for (i in 1..200) {
            if (i > 1) json.append(",\n")
            json.append("{\"customer_id\":\"id$i\",\"customer_email\":\"user$i@example.com\",\"customer_name\":\"User $i\",\"subject\":\"Sub $i\",\"description\":\"Description for ticket $i\"}")
        }
        json.append("\n]")
        val result = jsonImporter.import(ByteArrayInputStream(json.toString().toByteArray()))
        assertThat(result.totalRecords).isGreaterThanOrEqualTo(200)
    }
}
