package com.ai.homework.importer

import com.ai.homework.dto.ImportError
import com.ai.homework.dto.ImportResult
import com.ai.homework.model.DeviceType
import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketMetadata
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketSource
import com.ai.homework.model.TicketStatus
import com.ai.homework.service.TicketService
import com.ai.homework.validator.TicketValidator
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader

@Component
class CsvTicketImporter(
    private val validator: TicketValidator,
    private val ticketService: TicketService
) : TicketImporter {

    companion object {
        private val CSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim()
    }

    override fun import(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<ImportError>()
        val tickets = mutableListOf<Ticket>()
        var totalRecords = 0

        try {
            val reader = InputStreamReader(inputStream)
            val csvParser = CSVParser(reader, CSV_FORMAT)

            csvParser.use { parser ->
                var recordNumber = 1
                for (record in parser) {
                    recordNumber++
                    totalRecords++

                    val importedTicket = parseTicketFromCsv(record.toMap(), recordNumber)
                    if (importedTicket.ticket != null) {
                        tickets.add(importedTicket.ticket)
                        ticketService.storeTicket(importedTicket.ticket)
                    } else {
                        importedTicket.errors.forEach { errorMsg ->
                            errors.add(
                                ImportError(
                                    row = recordNumber,
                                    field = errorMsg.split(":")[0],
                                    message = errorMsg
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult(
                totalRecords = 0,
                successful = 0,
                failed = 1,
                errors = listOf(
                    ImportError(
                        row = 0,
                        field = "file",
                        message = "Failed to parse CSV: ${e.message}"
                    )
                )
            )
        }

        return ImportResult(
            totalRecords = totalRecords,
            successful = tickets.size,
            failed = errors.size,
            errors = errors
        )
    }

    private fun parseTicketFromCsv(record: Map<String, String>, rowNumber: Int): ImportedTicket {
        val errors = mutableListOf<String>()

        val customerId = record["customer_id"]?.trim() ?: ""
        val customerEmail = record["customer_email"]?.trim() ?: ""
        val customerName = record["customer_name"]?.trim() ?: ""
        val subject = record["subject"]?.trim() ?: ""
        val description = record["description"]?.trim() ?: ""

        if (customerId.isBlank()) errors.add("customer_id: required field")
        if (customerEmail.isBlank()) errors.add("customer_email: required field")
        if (customerName.isBlank()) errors.add("customer_name: required field")
        if (subject.isBlank()) errors.add("subject: required field")
        if (description.isBlank()) errors.add("description: required field")

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val category = try {
            record["category"]?.let { TicketCategory.valueOf(it.uppercase().replace("-", "_")) }
                ?: TicketCategory.OTHER
        } catch (e: Exception) {
            errors.add("category: invalid value")
            TicketCategory.OTHER
        }

        val priority = try {
            record["priority"]?.let { TicketPriority.valueOf(it.uppercase()) }
                ?: TicketPriority.MEDIUM
        } catch (e: Exception) {
            errors.add("priority: invalid value")
            TicketPriority.MEDIUM
        }

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val source = try {
            record["source"]?.let { TicketSource.valueOf(it.uppercase().replace("-", "_")) }
        } catch (e: Exception) {
            null
        }

        val deviceType = try {
            record["device_type"]?.let { DeviceType.valueOf(it.uppercase()) }
        } catch (e: Exception) {
            null
        }

        val metadata = TicketMetadata(
            source = source,
            browser = record["browser"],
            deviceType = deviceType
        )

        val ticket = Ticket(
            customerId = customerId,
            customerEmail = customerEmail,
            customerName = customerName,
            subject = subject,
            description = description,
            category = category,
            priority = priority,
            metadata = metadata,
            tags = record["tags"]?.split(",")?.map { it.trim() } ?: emptyList()
        )

        return ImportedTicket(ticket, emptyList())
    }
}