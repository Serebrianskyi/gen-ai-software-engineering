package com.ai.homework.importer

import com.ai.homework.dto.ImportError
import com.ai.homework.dto.ImportResult
import com.ai.homework.model.DeviceType
import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketMetadata
import com.ai.homework.model.TicketPriority
import com.ai.homework.model.TicketSource
import com.ai.homework.service.TicketService
import com.ai.homework.validator.TicketValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class JsonTicketImporter(
    private val validator: TicketValidator,
    private val ticketService: TicketService
) : TicketImporter {

    private val objectMapper = ObjectMapper()

    override fun import(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<ImportError>()
        val tickets = mutableListOf<Ticket>()

        try {
            val jsonNode = objectMapper.readTree(inputStream)

            if (!jsonNode.isArray) {
                return ImportResult(
                    totalRecords = 0,
                    successful = 0,
                    failed = 1,
                    errors = listOf(
                        ImportError(
                            row = 0,
                            field = "file",
                            message = "Root element must be an array"
                        )
                    )
                )
            }

            val arrayNode = jsonNode as ArrayNode
            var recordNumber = 0

            for (node in arrayNode) {
                recordNumber++
                if (node !is ObjectNode) {
                    errors.add(
                        ImportError(
                            row = recordNumber,
                            field = "record",
                            message = "Record must be an object"
                        )
                    )
                    continue
                }

                val importedTicket = parseTicketFromJson(node, recordNumber)
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

            return ImportResult(
                totalRecords = recordNumber,
                successful = tickets.size,
                failed = errors.size,
                errors = errors
            )
        } catch (e: Exception) {
            return ImportResult(
                totalRecords = 0,
                successful = 0,
                failed = 1,
                errors = listOf(
                    ImportError(
                        row = 0,
                        field = "file",
                        message = "Failed to parse JSON: ${e.message}"
                    )
                )
            )
        }
    }

    private fun parseTicketFromJson(node: ObjectNode, rowNumber: Int): ImportedTicket {
        val errors = mutableListOf<String>()

        val customerId = node.get("customer_id")?.asText() ?: ""
        val customerEmail = node.get("customer_email")?.asText() ?: ""
        val customerName = node.get("customer_name")?.asText() ?: ""
        val subject = node.get("subject")?.asText() ?: ""
        val description = node.get("description")?.asText() ?: ""

        if (customerId.isBlank()) errors.add("customer_id: required field")
        if (customerEmail.isBlank()) errors.add("customer_email: required field")
        if (customerName.isBlank()) errors.add("customer_name: required field")
        if (subject.isBlank()) errors.add("subject: required field")
        if (description.isBlank()) errors.add("description: required field")

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val category = try {
            node.get("category")?.asText()?.let {
                TicketCategory.valueOf(it.uppercase().replace("-", "_"))
            } ?: TicketCategory.OTHER
        } catch (e: Exception) {
            errors.add("category: invalid value")
            TicketCategory.OTHER
        }

        val priority = try {
            node.get("priority")?.asText()?.let {
                TicketPriority.valueOf(it.uppercase())
            } ?: TicketPriority.MEDIUM
        } catch (e: Exception) {
            errors.add("priority: invalid value")
            TicketPriority.MEDIUM
        }

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val source = try {
            node.get("source")?.asText()?.let {
                TicketSource.valueOf(it.uppercase().replace("-", "_"))
            }
        } catch (e: Exception) {
            null
        }

        val deviceType = try {
            node.get("device_type")?.asText()?.let {
                DeviceType.valueOf(it.uppercase())
            }
        } catch (e: Exception) {
            null
        }

        val browser = node.get("browser")?.asText()

        val metadata = TicketMetadata(
            source = source,
            browser = browser,
            deviceType = deviceType
        )

        val tagsNode = node.get("tags")
        val tags = if (tagsNode?.isArray == true) {
            tagsNode.map { it.asText() }
        } else {
            emptyList()
        }

        val ticket = Ticket(
            customerId = customerId,
            customerEmail = customerEmail,
            customerName = customerName,
            subject = subject,
            description = description,
            category = category,
            priority = priority,
            metadata = metadata,
            tags = tags
        )

        return ImportedTicket(ticket, emptyList())
    }
}