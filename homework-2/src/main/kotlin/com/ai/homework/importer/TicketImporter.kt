package com.ai.homework.importer

import com.ai.homework.dto.ImportResult
import com.ai.homework.model.Ticket
import java.io.InputStream

interface TicketImporter {
    fun import(inputStream: InputStream): ImportResult
}

data class ImportedTicket(
    val ticket: Ticket?,
    val errors: List<String> = emptyList(),
)
