package com.example.expenseease

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isIncome: Boolean = false,
    val paymentMethod: String = "Cash",
    val recurringType: String = "None" // None, Daily, Weekly, Monthly
) {
    fun getFormattedAmount(): String {
        return if (isIncome) {
            "+$${String.format("%.2f", amount)}"
        } else {
            "-$${String.format("%.2f", amount)}"
        }
    }

    fun getAmountValue(): Double {
        return if (isIncome) amount else -amount
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedShortDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        const val TYPE_EXPENSE = false
        const val TYPE_INCOME = true

        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Education", "Other"
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            "Salary", "Investments", "Gifts", "Refunds", "Other"
        )

        val PAYMENT_METHODS = listOf(
            "Cash", "Credit Card", "Debit Card", "Bank Transfer", "Mobile Payment"
        )
    }
}