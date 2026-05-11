package com.ai.homework.service

import com.ai.homework.dto.ImportResult
import com.ai.homework.importer.CsvTicketImporter
import com.ai.homework.importer.JsonTicketImporter
import com.ai.homework.importer.XmlTicketImporter
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Service
class ImportService(
    private val csvImporter: CsvTicketImporter,
    private val jsonImporter: JsonTicketImporter,
    private val xmlImporter: XmlTicketImporter,
    private val ticketService: TicketService
) {

    fun importTickets(file: MultipartFile): ImportResult {
        val fileName = file.originalFilename ?: ""
        val mimeType = file.contentType ?: ""

        val result = when {
            fileName.endsWith(".csv", ignoreCase = true) || mimeType.contains("csv") -> {
                csvImporter.import(file.inputStream)
            }
            fileName.endsWith(".json", ignoreCase = true) || mimeType.contains("json") -> {
                jsonImporter.import(file.inputStream)
            }
            fileName.endsWith(".xml", ignoreCase = true) || mimeType.contains("xml") -> {
                xmlImporter.import(file.inputStream)
            }
            else -> {
                return ImportResult(
                    totalRecords = 0,
                    successful = 0,
                    failed = 1,
                    errors = listOf(
                        com.ai.homework.dto.ImportError(
                            row = 0,
                            field = "file",
                            message = "Unsupported file format. Supported formats: CSV, JSON, XML"
                        )
                    )
                )
            }
        }

        return result
    }
}