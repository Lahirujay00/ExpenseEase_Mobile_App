package com.example.expenseease

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {
    private val TAG = "BackupManager"
    private val transactionManager = TransactionManager(context)
    private val budgetManager = BudgetManager(context)
    private val sharedPreferenceManager = SharedPreferenceManager(context)


    fun createBackup(): String? {
        try {
            Log.d(TAG, "Starting backup creation...")
            
            // Create a JSON object to hold all data
            val backupData = JSONObject()
            
            // Add transactions
            val transactions = transactionManager.getAllTransactions()
            Log.d(TAG, "Found ${transactions.size} transactions to backup")
            val transactionsArray = JSONArray()
            for (transaction in transactions) {
                val transactionObj = JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("description", transaction.description)
                    put("timestamp", transaction.timestamp)
                    put("isIncome", transaction.isIncome)
                }
                transactionsArray.put(transactionObj)
            }
            backupData.put("transactions", transactionsArray)
            
            // Add budgets
            val budgets = budgetManager.getAllBudgets()
            Log.d(TAG, "Found ${budgets.size} budgets to backup")
            val budgetsArray = JSONArray()
            for (budget in budgets) {
                val budgetObj = JSONObject().apply {
                    put("id", budget.id)
                    put("category", budget.category)
                    put("amount", budget.amount)
                    put("period", budget.period)
                    put("createdTimestamp", budget.createdTimestamp)
                    put("isActive", budget.isActive)
                    put("notes", budget.notes)
                }
                budgetsArray.put(budgetObj)
            }
            backupData.put("budgets", budgetsArray)
            
            // Add user preferences
            val preferences = JSONObject().apply {
                put("userName", sharedPreferenceManager.getCurrentUserName() ?: "")
                put("notificationEnabled", sharedPreferenceManager.isNotificationEnabled())
                //put("darkModeEnabled", sharedPreferenceManager.isDarkModeEnabled())
            }
            backupData.put("preferences", preferences)
            
            // Create backup directory if it doesn't exist
            val backupDir = File(context.filesDir, "backups")
            Log.d(TAG, "Backup directory path: ${backupDir.absolutePath}")
            if (!backupDir.exists()) {
                val created = backupDir.mkdirs()
                Log.d(TAG, "Created backup directory: $created")
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "expenseease_backup_$timestamp.json")
            Log.d(TAG, "Backup file path: ${backupFile.absolutePath}")
            
            // Write backup data to file
            FileOutputStream(backupFile).use { 
                it.write(backupData.toString(2).toByteArray()) 
            }
            
            Log.d(TAG, "Backup created successfully at ${backupFile.absolutePath}")
            return backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            return null
        }
    }

    fun restoreBackup(backupFilePath: String): Boolean {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: $backupFilePath")
                return false
            }
            
            // Read backup data from file
            val backupData = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val jsonData = JSONObject(backupData)
            
            // Clear existing data
            transactionManager.clearAllTransactions()
            budgetManager.clearAllBudgets()
            
            // Restore transactions
            val transactionsArray = jsonData.getJSONArray("transactions")
            for (i in 0 until transactionsArray.length()) {
                val transactionObj = transactionsArray.getJSONObject(i)
                val transaction = Transaction(
                    id = transactionObj.getLong("id"),
                    title = transactionObj.getString("title"),
                    amount = transactionObj.getDouble("amount"),
                    category = transactionObj.getString("category"),
                    description = transactionObj.getString("description"),
                    timestamp = transactionObj.getLong("timestamp"),
                    isIncome = transactionObj.getBoolean("isIncome")
                )
                transactionManager.addTransaction(transaction)
            }
            
            // Restore budgets
            val budgetsArray = jsonData.getJSONArray("budgets")
            for (i in 0 until budgetsArray.length()) {
                val budgetObj = budgetsArray.getJSONObject(i)
                val budget = Budget(
                    id = budgetObj.getLong("id").toString(),
                    category = budgetObj.getString("category"),
                    amount = budgetObj.getDouble("amount"),
                    period = budgetObj.getString("period"),
                    createdTimestamp = budgetObj.getLong("createdTimestamp"),
                    isActive = budgetObj.getBoolean("isActive"),
                    notes = budgetObj.getString("notes")
                )
                budgetManager.addBudget(budget)
            }
            
            // Restore preferences
            val preferences = jsonData.getJSONObject("preferences")
            sharedPreferenceManager.setCurrentUserName(preferences.getString("userName"))
            sharedPreferenceManager.setNotificationEnabled(preferences.getBoolean("notificationEnabled"))
           // sharedPreferenceManager.setDarkModeEnabled(preferences.getBoolean("darkModeEnabled"))
            
            Log.d(TAG, "Backup restored successfully from $backupFilePath")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            return false
        }
    }

    fun getAvailableBackups(): List<String> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
            return emptyList()
        }
        
        return backupDir.listFiles { file -> 
            file.isFile && file.name.startsWith("expenseease_backup_") && file.name.endsWith(".json")
        }?.map { it.absolutePath }?.sortedByDescending { it } ?: emptyList()
    }
    

    fun deleteBackup(backupFilePath: String): Boolean {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: $backupFilePath")
                return false
            }
            
            val result = backupFile.delete()
            Log.d(TAG, "Backup file deleted: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            return false
        }
    }
} 