package com.banking.controller

import com.banking.model.*
import com.banking.model.generated.CreateTransactionRequest
import com.banking.service.TransactionService
import com.banking.service.ValidationException
import com.banking.validator.ValidationErrorDetail
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Banking transaction management API")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping("/transactions")
    @Operation(summary = "Create a new transaction")
    @ApiResponse(responseCode = "201", description = "Transaction created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    fun createTransaction(@RequestBody request: CreateTransactionRequest): ResponseEntity<Any> {
        return try {
            val result = transactionService.createTransaction(request)
            result.fold(
                onSuccess = { transaction ->
                    ResponseEntity.status(HttpStatus.CREATED).body(transaction)
                },
                onFailure = { error ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(error as? ValidationException)
                }
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "internal_error", "message" to (e.message ?: "Unknown error")))
        }
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all transactions with optional filters")
    @ApiResponse(responseCode = "200", description = "List of transactions")
    fun getTransactions(
        @RequestParam(required = false) accountId: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ResponseEntity<List<Transaction>> {
        val transactions = transactionService.getTransactions(accountId, type, from, to)
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get a specific transaction by ID")
    @ApiResponse(responseCode = "200", description = "Transaction details")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    fun getTransactionById(@PathVariable id: String): ResponseEntity<Any> {
        val transaction = transactionService.getTransactionById(id)
        return if (transaction != null) {
            ResponseEntity.ok(transaction)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "not_found", "message" to "Transaction not found"))
        }
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get account balance")
    @ApiResponse(responseCode = "200", description = "Account balance information")
    @ApiResponse(responseCode = "404", description = "Account not found")
    fun getAccountBalance(@PathVariable accountId: String): ResponseEntity<Any> {
        val balance = transactionService.getAccountBalance(accountId)
        return if (balance != null) {
            ResponseEntity.ok(balance)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "not_found", "message" to "Account not found"))
        }
    }

    @GetMapping("/accounts/{accountId}/summary")
    @Operation(summary = "Get transaction summary for an account")
    @ApiResponse(responseCode = "200", description = "Account transaction summary")
    @ApiResponse(responseCode = "404", description = "Account not found")
    fun getAccountSummary(@PathVariable accountId: String): ResponseEntity<Any> {
        val summary = transactionService.getAccountSummary(accountId)
        return if (summary != null) {
            ResponseEntity.ok(summary)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "not_found", "message" to "Account not found"))
        }
    }

    @GetMapping("/accounts/{accountId}/interest")
    @Operation(summary = "Calculate simple interest on account balance")
    @ApiResponse(responseCode = "200", description = "Interest calculation result")
    @ApiResponse(responseCode = "400", description = "Invalid parameters")
    @ApiResponse(responseCode = "404", description = "Account not found")
    fun calculateInterest(
        @PathVariable accountId: String,
        @RequestParam rate: Double,
        @RequestParam days: Int
    ): ResponseEntity<Any> {
        if (rate < 0 || days < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "invalid_params", "message" to "Rate must be non-negative and days must be positive"))
        }

        val calculation = transactionService.calculateInterest(accountId, rate, days)
        return if (calculation != null) {
            ResponseEntity.ok(calculation)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "not_found", "message" to "Account not found"))
        }
    }

    @ExceptionHandler(ValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: ValidationException): Map<String, Any> {
        return mapOf(
            "error" to "Validation failed",
            "details" to ex.errors
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Exception): Map<String, Any> {
        return mapOf(
            "error" to "internal_error",
            "message" to (ex.message ?: "Unknown error"),
            "timestamp" to System.currentTimeMillis()
        )
    }
}