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
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.springframework.stereotype.Component
import java.io.InputStream

@JacksonXmlRootElement(localName = "tickets")
data class XmlTickets(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ticket")
    val tickets: List<XmlTicket> = emptyList()
)

data class XmlTicket(
    @JacksonXmlProperty(localName = "customer_id")
    val customerId: String = "",

    @JacksonXmlProperty(localName = "customer_email")
    val customerEmail: String = "",

    @JacksonXmlProperty(localName = "customer_name")
    val customerName: String = "",

    @JacksonXmlProperty(localName = "subject")
    val subject: String = "",

    @JacksonXmlProperty(localName = "description")
    val description: String = "",

    @JacksonXmlProperty(localName = "category")
    val category: String = "other",

    @JacksonXmlProperty(localName = "priority")
    val priority: String = "medium",

    @JacksonXmlProperty(localName = "status")
    val status: String? = null,

    @JacksonXmlProperty(localName = "source")
    val source: String? = null,

    @JacksonXmlProperty(localName = "browser")
    val browser: String? = null,

    @JacksonXmlProperty(localName = "device_type")
    val deviceType: String? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "tag")
    val tags: List<String>? = null
)

@Component
class XmlTicketImporter(
    private val validator: TicketValidator,
    private val ticketService: TicketService
) : TicketImporter {

    private val xmlMapper = XmlMapper()

    override fun import(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<ImportError>()
        val tickets = mutableListOf<Ticket>()

        try {
            val xmlTickets = xmlMapper.readValue(inputStream, XmlTickets::class.java)
            var recordNumber = 0

            for (xmlTicket in xmlTickets.tickets) {
                recordNumber++
                val importedTicket = parseTicketFromXml(xmlTicket, recordNumber)
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
                        message = "Failed to parse XML: ${e.message}"
                    )
                )
            )
        }
    }

    private fun parseTicketFromXml(xmlTicket: XmlTicket, rowNumber: Int): ImportedTicket {
        val errors = mutableListOf<String>()

        if (xmlTicket.customerId.isBlank()) errors.add("customer_id: required field")
        if (xmlTicket.customerEmail.isBlank()) errors.add("customer_email: required field")
        if (xmlTicket.customerName.isBlank()) errors.add("customer_name: required field")
        if (xmlTicket.subject.isBlank()) errors.add("subject: required field")
        if (xmlTicket.description.isBlank()) errors.add("description: required field")

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val category = try {
            TicketCategory.valueOf(xmlTicket.category.uppercase().replace("-", "_"))
        } catch (e: Exception) {
            errors.add("category: invalid value")
            TicketCategory.OTHER
        }

        val priority = try {
            TicketPriority.valueOf(xmlTicket.priority.uppercase())
        } catch (e: Exception) {
            errors.add("priority: invalid value")
            TicketPriority.MEDIUM
        }

        if (errors.isNotEmpty()) {
            return ImportedTicket(null, errors)
        }

        val source = try {
            xmlTicket.source?.let {
                TicketSource.valueOf(it.uppercase().replace("-", "_"))
            }
        } catch (e: Exception) {
            null
        }

        val deviceType = try {
            xmlTicket.deviceType?.let {
                DeviceType.valueOf(it.uppercase())
            }
        } catch (e: Exception) {
            null
        }

        val metadata = TicketMetadata(
            source = source,
            browser = xmlTicket.browser,
            deviceType = deviceType
        )

        val ticket = Ticket(
            customerId = xmlTicket.customerId,
            customerEmail = xmlTicket.customerEmail,
            customerName = xmlTicket.customerName,
            subject = xmlTicket.subject,
            description = xmlTicket.description,
            category = category,
            priority = priority,
            metadata = metadata,
            tags = xmlTicket.tags ?: emptyList()
        )

        return ImportedTicket(ticket, emptyList())
    }
}