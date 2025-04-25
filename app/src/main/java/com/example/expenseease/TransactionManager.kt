package com.example.expenseease

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class TransactionManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        TRANSACTION_PREF, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // Add a new transaction
    fun addTransaction(transaction: Transaction): Long {
        val transactions = getAllTransactions().toMutableList()

        // Generate a new ID (timestamp + random component for uniqueness)
        val newId = if (transactions.isEmpty()) {
            1L
        } else {
            transactions.maxByOrNull { it.id }?.id?.plus(1) ?: 1L
        }

        // Create a copy with the generated ID
        val newTransaction = transaction.copy(id = newId)

        transactions.add(newTransaction)
        saveTransactions(transactions)
        return newId
    }

    // Get all transactions
    fun getAllTransactions(): List<Transaction> {
        val transactionsJson = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(transactionsJson, type) ?: emptyList()
    }

    // Get a transaction by ID
    fun getTransactionById(id: Long): Transaction? {
        return getAllTransactions().find { it.id == id }
    }

    // Update an existing transaction
    fun updateTransaction(transaction: Transaction): Boolean {
        val transactions = getAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }

        if (index != -1) {
            transactions[index] = transaction
            saveTransactions(transactions)
            return true
        }
        return false
    }

    // Delete a transaction
    fun deleteTransaction(id: Long): Boolean {
        val transactions = getAllTransactions().toMutableList()
        val removed = transactions.removeIf { it.id == id }

        if (removed) {
            saveTransactions(transactions)
            return true
        }
        return false
    }

    // Get transactions for current month
    fun getCurrentMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return getAllTransactions().filter {
            val transactionCalendar = Calendar.getInstance().apply {
                timeInMillis = it.timestamp
            }
            transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    // Get monthly income total
    fun getCurrentMonthIncome(): Double {
        return getCurrentMonthTransactions()
            .filter { it.isIncome }
            .sumOf { it.amount }
    }

    // Get monthly expense total
    fun getCurrentMonthExpense(): Double {
        return getCurrentMonthTransactions()
            .filter { !it.isIncome }
            .sumOf { it.amount }
    }

    // Get balance (income - expense)
    fun getCurrentMonthBalance(): Double {
        return getCurrentMonthIncome() - getCurrentMonthExpense()
    }

    // Private helper to save the transactions list
    private fun saveTransactions(transactions: List<Transaction>) {
        val transactionsJson = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, transactionsJson).apply()
    }

    fun getLastWeekTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        // Set to 7 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.timeInMillis

        return getAllTransactions().filter {
            it.timestamp in startDate..endDate
        }
    }


    fun getLastMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        // Set to 30 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = calendar.timeInMillis

        return getAllTransactions().filter {
            it.timestamp in startDate..endDate
        }
    }

    fun clearAllTransactions() {
        sharedPreferences.edit().remove(KEY_TRANSACTIONS).apply()
    }

    companion object {
        private const val TRANSACTION_PREF = "transaction_preferences"
        private const val KEY_TRANSACTIONS = "transactions"
    }
}