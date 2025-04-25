package com.example.expenseease

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class BudgetCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BudgetCheckWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting budget check")
            
            val budgetManager = BudgetManager(applicationContext)
            val transactionManager = TransactionManager(applicationContext)
            val notificationManager = ExpenseNotificationManager(applicationContext)
            
            // Get all active budgets
            val budgets = budgetManager.getActiveBudgets()
            Log.d(TAG, "Found ${budgets.size} active budgets")
            
            if (budgets.isEmpty()) {
                Log.d(TAG, "No active budgets found")
                return@withContext Result.success()
            }
            
            // Get current month's transactions
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val transactions = transactionManager.getAllTransactions()
                .filter { it.timestamp >= startOfMonth }
            
            Log.d(TAG, "Found ${transactions.size} transactions for current month")
            
            // Calculate total budget and spending
            val totalBudget = budgets.sumOf { it.amount }
            val totalSpent = transactions.filter { !it.isIncome }.sumOf { it.amount }
            val percentSpent = ((totalSpent / totalBudget) * 100).toInt()
            
            Log.d(TAG, "Total budget: $totalBudget, Total spent: $totalSpent ($percentSpent%)")
            
            // Send notification if total budget is exceeded
            if (percentSpent >= 100) {
                Log.d(TAG, "Total budget exceeded, sending notification")
                notificationManager.sendMonthlyBudgetNotification(totalSpent, totalBudget, percentSpent)
            }
            // Send warning at 90%
            else if (percentSpent >= 90) {
                Log.d(TAG, "Total budget warning, sending notification")
                notificationManager.sendBudgetWarningNotification(totalSpent, totalBudget, percentSpent)
            }
            
            // Check individual budgets
            budgets.forEach { budget ->
                val spent = transactions
                    .filter { !it.isIncome && it.category == budget.category }
                    .sumOf { it.amount }
                
                val percentSpent = if (budget.amount > 0) ((spent / budget.amount) * 100).toInt() else 0
                Log.d(TAG, "Budget ${budget.category}: Spent $spent of ${budget.amount} ($percentSpent%)")
                
                // Send notification if budget is exceeded
                if (percentSpent >= 100) {
                    Log.d(TAG, "Budget exceeded for ${budget.category}, sending notification")
                    notificationManager.sendMonthlyBudgetNotification(spent, budget.amount, percentSpent)
                }
                // Send warning at 90%
                else if (percentSpent >= 90) {
                    Log.d(TAG, "Budget warning for ${budget.category}, sending notification")
                    notificationManager.sendBudgetWarningNotification(spent, budget.amount, percentSpent)
                }
            }
            
            Log.d(TAG, "Budget check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during budget check", e)
            Result.failure()
        }
    }
}