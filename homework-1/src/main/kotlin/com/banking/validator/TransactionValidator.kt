package com.banking.validator

import com.banking.model.generated.CreateTransactionRequest
import org.springframework.stereotype.Component

data class ValidationErrorDetail(
    val field: String,
    val message: String,
)

@Component
class TransactionValidator {

    private val validCurrencies = setOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY",
        "INR", "RUB", "ZAR", "KRW", "MXN", "SGD", "HKD", "NOK",
        "SEK", "DKK", "NZD", "BRL", "TRY", "AED", "SAR",
    )

    fun validateCreateTransaction(request: CreateTransactionRequest): List<ValidationErrorDetail> {
        val errors = mutableListOf<ValidationErrorDetail>()

        // Amount validation
        if (request.amount <= 0) {
            errors.add(ValidationErrorDetail("amount", "Amount must be positive"))
        }
        if (!isValidDecimalPlaces(request.amount, 2)) {
            errors.add(ValidationErrorDetail("amount", "Amount must have maximum 2 decimal places"))
        }

        // Account validation
        if (!isValidAccountFormat(request.fromAccount)) {
            errors.add(ValidationErrorDetail("fromAccount", "Invalid source account format. Expected: ACC-XXXXX"))
        }
        if (!isValidAccountFormat(request.toAccount)) {
            errors.add(ValidationErrorDetail("toAccount", "Invalid destination account format. Expected: ACC-XXXXX"))
        }

        // Currency validation
        if (!validCurrencies.contains(request.currency.uppercase())) {
            errors.add(ValidationErrorDetail("currency", "Invalid ISO 4217 currency code"))
        }

        // Same account check for transfers
        if (request.type.name == "TRANSFER" && request.fromAccount == request.toAccount) {
            errors.add(ValidationErrorDetail("toAccount", "Cannot transfer to the same account"))
        }

        return errors
    }

    private fun isValidAccountFormat(account: String): Boolean {
        return account.matches(Regex("^ACC-[A-Z0-9]{5}$"))
    }

    private fun isValidDecimalPlaces(value: Double, places: Int): Boolean {
        val scaled = value * Math.pow(10.0, places.toDouble())
        return scaled == scaled.toLong().toDouble()
    }
}
