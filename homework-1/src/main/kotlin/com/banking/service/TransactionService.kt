package com.banking.service

import com.banking.model.AccountBalance
import com.banking.model.AccountSummary
import com.banking.model.InterestCalculation
import com.banking.model.Transaction
import com.banking.model.TransactionStatus
import com.banking.model.TransactionType
import com.banking.model.generated.CreateTransactionRequest
import com.banking.validator.TransactionValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class TransactionService(
    private val validator: TransactionValidator,
) {

    private val transactions: MutableList<Transaction> = mutableListOf()
    private val objectMapper = ObjectMapper()

    init {
        loadTransactionsFromJson()
    }

    private fun loadTransactionsFromJson() {
        // Try multiple paths
        val paths = listOf(
            File("transactions.json"),
            File("../transactions.json"),
            File(System.getProperty("user.dir"), "transactions.json"),
            File(System.getProperty("user.dir"), "../transactions.json"),
        )

        val file = paths.firstOrNull { it.exists() }
        if (file != null) {
            try {
                val loadedTransactions: List<Map<String, Any>> = objectMapper.readValue(file)
                loadedTransactions.forEach { map ->
                    try {
                        val transaction = Transaction(
                            id = map["id"] as String,
                            fromAccount = map["fromAccount"] as String,
                            toAccount = map["toAccount"] as String,
                            amount = (map["amount"] as Number).toDouble(),
                            currency = map["currency"] as String,
                            type = TransactionType.valueOf((map["type"] as String).uppercase()),
                            timestamp = Instant.parse(map["timestamp"] as String),
                            status = TransactionStatus.valueOf((map["status"] as String).uppercase()),
                            description = map["description"] as? String,
                        )
                        transactions.add(transaction)
                    } catch (e: Exception) {
                        System.err.println("Error parsing transaction: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                System.err.println("Error loading transactions.json: ${e.message}")
            }
        }
    }

    fun createTransaction(request: CreateTransactionRequest): Result<Transaction> {
        val errors = validator.validateCreateTransaction(request)
        if (errors.isNotEmpty()) {
            return Result.failure(ValidationException(errors))
        }

        val typeEnum =
            when (request.type) {
                CreateTransactionRequest.Type.DEPOSIT -> TransactionType.DEPOSIT
                CreateTransactionRequest.Type.WITHDRAWAL -> TransactionType.WITHDRAWAL
                CreateTransactionRequest.Type.TRANSFER -> TransactionType.TRANSFER
            }

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            fromAccount = request.fromAccount,
            toAccount = request.toAccount,
            amount = request.amount,
            currency = request.currency.uppercase(),
            type = typeEnum,
            timestamp = Instant.now(),
            status = TransactionStatus.COMPLETED,
            description = request.description,
        )

        transactions.add(transaction)
        return Result.success(transaction)
    }

    fun getTransactions(
        accountId: String? = null,
        type: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): List<Transaction> {
        return transactions.filter { transaction ->
            var matches = true

            if (accountId != null) {
                matches = matches && (transaction.fromAccount == accountId || transaction.toAccount == accountId)
            }

            if (type != null) {
                matches = matches && transaction.type.name.equals(type, ignoreCase = true)
            }

            if (from != null) {
                val fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                matches = matches && transaction.timestamp.isAfter(fromInstant)
            }

            if (to != null) {
                val toInstant = to.atStartOfDay(ZoneId.systemDefault()).plusSeconds(86400).toInstant()
                matches = matches && transaction.timestamp.isBefore(toInstant)
            }

            matches
        }
    }

    fun getTransactionById(id: String): Transaction? {
        return transactions.find { it.id == id }
    }

    fun getAccountBalance(accountId: String): AccountBalance? {
        val balance = transactions
            .filter { it.status == TransactionStatus.COMPLETED }
            .filter { it.toAccount == accountId || it.fromAccount == accountId }
            .fold(0.0) { acc, transaction ->
                when {
                    transaction.toAccount == accountId -> acc + transaction.amount
                    transaction.fromAccount == accountId -> acc - transaction.amount
                    else -> acc
                }
            }

        return if (balance != 0.0 || transactions.any { it.fromAccount == accountId || it.toAccount == accountId }) {
            AccountBalance(
                accountId = accountId,
                balance = balance,
                currency = "USD",
                lastUpdated = Instant.now(),
            )
        } else {
            null
        }
    }

    fun getAccountSummary(accountId: String): AccountSummary? {
        val accountTransactions = transactions.filter {
            it.fromAccount == accountId || it.toAccount == accountId
        }

        if (accountTransactions.isEmpty()) {
            return null
        }

        val totalDeposits = accountTransactions
            .filter { it.toAccount == accountId && it.status == TransactionStatus.COMPLETED }
            .sumOf { it.amount }

        val totalWithdrawals = accountTransactions
            .filter { it.fromAccount == accountId && it.status == TransactionStatus.COMPLETED }
            .sumOf { it.amount }

        val lastTransaction = accountTransactions
            .filter { it.status == TransactionStatus.COMPLETED }
            .maxByOrNull { it.timestamp }

        return AccountSummary(
            accountId = accountId,
            totalDeposits = totalDeposits,
            totalWithdrawals = totalWithdrawals,
            transactionCount = accountTransactions.size,
            lastTransactionDate = lastTransaction?.timestamp,
        )
    }

    fun calculateInterest(accountId: String, rate: Double, days: Int): InterestCalculation? {
        val balance = getAccountBalance(accountId)?.balance ?: return null

        val principalBalance = balance
        val calculatedInterest = (principalBalance * rate * days) / 365
        val futureValue = principalBalance + calculatedInterest

        return InterestCalculation(
            accountId = accountId,
            principalBalance = principalBalance,
            rate = rate,
            days = days,
            calculatedInterest = calculatedInterest,
            futureValue = futureValue,
        )
    }
}

class ValidationException(val errors: List<com.banking.validator.ValidationErrorDetail>) : Exception()
