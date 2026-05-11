package com.banking.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val fromAccount: String,
    val toAccount: String,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val timestamp: Instant = Instant.now(),
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String? = null,
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
}

data class AccountBalance(
    val accountId: String,
    val balance: Double,
    val currency: String,
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val lastUpdated: Instant = Instant.now(),
)

data class AccountSummary(
    val accountId: String,
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val transactionCount: Int,
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val lastTransactionDate: Instant? = null,
)

data class InterestCalculation(
    val accountId: String,
    val principalBalance: Double,
    val rate: Double,
    val days: Int,
    val calculatedInterest: Double,
    val futureValue: Double,
)
