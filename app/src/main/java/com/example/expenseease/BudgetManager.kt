package com.example.expenseease

import android.content.Context
import android.content.SharedPreferences
import android.app.NotificationManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import android.content.ContentValues
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

class BudgetManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        BUDGET_PREF, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val transactionManager = TransactionManager(context)

    // Get all budgets
    fun getAllBudgets(): List<Budget> {
        val budgetsJson = sharedPreferences.getString(KEY_BUDGETS, "[]")
        val type = object : TypeToken<List<Budget>>() {}.type
        return gson.fromJson(budgetsJson, type) ?: emptyList()
    }

    // Get active budgets only
    fun getActiveBudgets(): List<Budget> {
        return getAllBudgets().filter { it.isActive }
    }

    // Add a new budget
    fun addBudget(budget: Budget): Boolean {
        val budgets = getAllBudgets().toMutableList()

        // Check if budget for this category already exists
        val existingBudget = budgets.find {
            it.category == budget.category && it.period == budget.period && it.isActive
        }

        if (existingBudget != null) {
            return false // Budget for this category already exists
        }

        budgets.add(budget)
        return saveBudgets(budgets)
    }

    // Update an existing budget
    fun updateBudget(budget: Budget): Boolean {
        val budgets = getAllBudgets().toMutableList()
        val index = budgets.indexOfFirst { it.id == budget.id }

        if (index != -1) {
            budgets[index] = budget
            val success = saveBudgets(budgets)
            if (success) {
                // Clear notification history and force a budget check
                clearNotificationHistory()
                
                // Trigger an immediate budget check
                val notificationManager = ExpenseNotificationManager(context)
                notificationManager.checkBudgetThresholds()
            }
            return success
        }
        return false
    }

    // Delete a budget
    fun deleteBudget(budgetId: String): Boolean {
        val budgets = getAllBudgets().toMutableList()
        val removed = budgets.removeIf { it.id == budgetId }

        if (removed) {
            return saveBudgets(budgets)
        }
        return false
    }

    // Deactivate a budget (instead of deleting)
    fun deactivateBudget(budgetId: String): Boolean {
        val budgets = getAllBudgets().toMutableList()
        val budget = budgets.find { it.id == budgetId } ?: return false

        val updatedBudget = budget.copy(isActive = false)
        return updateBudget(updatedBudget)
    }

    // Get a specific budget
    fun getBudgetById(budgetId: String): Budget? {
        return getAllBudgets().find { it.id == budgetId }
    }

    // Get budget for a specific category
    fun getBudgetForCategory(category: String): Budget? {
        return getActiveBudgets().find { it.category == category }
    }

    // Get budget by category (added method that was outside)
    fun getBudgetByCategory(category: String): Budget? {
        return getActiveBudgets().find {
            it.category.equals(category, ignoreCase = true)
        }
    }

    // Calculate spending for a specific budget
    fun calculateBudgetSpending(budget: Budget): Double {
        Log.d("BudgetManager", "Calculating spending for budget: ${budget.category}")
        
        val transactions = when (budget.period.lowercase()) {
            "monthly" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                Log.d("BudgetManager", "Current month: $currentMonth, year: $currentYear")
                
                val filteredTransactions = transactionManager.getAllTransactions().filter {
                    val transactionCalendar = Calendar.getInstance().apply {
                        timeInMillis = it.timestamp
                    }
                    !it.isIncome && 
                    it.category.equals(budget.category, ignoreCase = true) &&
                    transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
                }
                
                Log.d("BudgetManager", "Found ${filteredTransactions.size} transactions for ${budget.category}")
                filteredTransactions.forEach { 
                    Log.d("BudgetManager", "Transaction: ${it.amount} on ${it.timestamp}")
                }
                
                filteredTransactions
            }
            "weekly" -> getWeeklyTransactions()
            "yearly" -> getYearlyTransactions()
            else -> transactionManager.getCurrentMonthTransactions()
        }

        val total = transactions.sumOf { it.amount }
        Log.d("BudgetManager", "Total spent for ${budget.category}: $total")
        return total
    }

    // Calculate budget remaining
    fun calculateBudgetRemaining(budget: Budget): Double {
        val spent = calculateBudgetSpending(budget)
        return budget.amount - spent
    }

    // Calculate budget usage as percentage
    fun calculateBudgetUsagePercentage(budget: Budget): Int {
        val spent = calculateBudgetSpending(budget)
        return if (budget.amount > 0) {
            ((spent / budget.amount) * 100).toInt().coerceIn(0, 100)
        } else {
            100 // If budget is 0, show as 100% used
        }
    }

    // Private helper methods
    private fun saveBudgets(budgets: List<Budget>): Boolean {
        return try {
            val budgetsJson = gson.toJson(budgets)
            sharedPreferences.edit().putString(KEY_BUDGETS, budgetsJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getWeeklyTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)

        // Calculate start of week (assuming Sunday is first day)
        calendar.add(Calendar.DAY_OF_WEEK, -(today - 1))
        val weekStart = calendar.timeInMillis

        // Calculate end of week
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = calendar.timeInMillis

        return transactionManager.getAllTransactions().filter {
            it.timestamp in weekStart..weekEnd
        }
    }

    fun getCategorySpentAmount(category: String, period: String): Double {
        Log.d("BudgetManager", "Getting spent amount for category: $category, period: $period")
        
        val transactions = when (period.lowercase()) {
            "monthly" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                Log.d("BudgetManager", "Current month: $currentMonth, year: $currentYear")
                
                val filteredTransactions = transactionManager.getAllTransactions().filter {
                    val transactionCalendar = Calendar.getInstance().apply {
                        timeInMillis = it.timestamp
                    }
                    !it.isIncome && 
                    it.category.equals(category, ignoreCase = true) &&
                    transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
                }
                
                Log.d("BudgetManager", "Found ${filteredTransactions.size} transactions for $category")
                filteredTransactions.forEach { 
                    Log.d("BudgetManager", "Transaction: ${it.amount} on ${it.timestamp}")
                }
                
                filteredTransactions
            }
            "weekly" -> getWeeklyTransactions()
            "yearly" -> getYearlyTransactions()
            else -> transactionManager.getCurrentMonthTransactions()
        }

        val total = transactions.sumOf { it.amount }
        Log.d("BudgetManager", "Total spent for $category: $total")
        return total
    }

    private fun getYearlyTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Start of year
        val yearStart = Calendar.getInstance().apply {
            set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis

        // End of year
        val yearEnd = Calendar.getInstance().apply {
            set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        return transactionManager.getAllTransactions().filter {
            it.timestamp in yearStart..yearEnd
        }
    }

    // Clear notification history
    private fun clearNotificationHistory() {
        try {
            // Clear the last notification timestamp and set forced check flag
            context.getSharedPreferences("notification_preferences", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_monthly_notification", 0)
                .putBoolean("force_budget_check", true)
                .apply()
                
            // Cancel any existing notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ExpenseNotificationManager.NOTIFICATION_ID_MONTHLY_BUDGET)
            notificationManager.cancel(ExpenseNotificationManager.NOTIFICATION_ID_BUDGET_WARNING)
            
            Log.d("BudgetManager", "Notification history cleared and forced check enabled")
        } catch (e: Exception) {
            Log.e("BudgetManager", "Error clearing notification history", e)
        }
    }

    fun clearAllBudgets() {
        sharedPreferences.edit().remove(KEY_BUDGETS).apply()
    }

    companion object {
        private const val BUDGET_PREF = "budget_preferences"
        private const val KEY_BUDGETS = "budgets"
    }
}