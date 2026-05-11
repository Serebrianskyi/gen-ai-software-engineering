package com.ai.homework.controller

import com.ai.homework.dto.ImportResult
import com.ai.homework.service.ImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tickets")
class ImportController(private val importService: ImportService) {

    @PostMapping("/import", consumes = ["multipart/form-data"])
    @Operation(
        summary = "Bulk import tickets from CSV/JSON/XML",
        description = "Upload a file (CSV, JSON, or XML) to import multiple tickets. Returns import summary with success/failure counts."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Import completed (check failed count for errors)"
    )
    @ApiResponse(
        responseCode = "400",
        description = "File is empty or invalid format"
    )
    fun importTickets(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ImportResult> {
        if (file.isEmpty) {
            return ResponseEntity(
                ImportResult(
                    totalRecords = 0,
                    successful = 0,
                    failed = 1,
                    errors = listOf(
                        com.ai.homework.dto.ImportError(
                            row = 0,
                            field = "file",
                            message = "File is empty"
                        )
                    )
                ),
                HttpStatus.BAD_REQUEST
            )
        }

        val result = importService.importTickets(file)

        return if (result.failed == 0) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(result, HttpStatus.BAD_REQUEST)
        }
    }
}
