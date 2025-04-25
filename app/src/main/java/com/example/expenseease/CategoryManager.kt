package com.example.expenseease

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoryManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_EXPENSE_CATEGORIES = "expense_categories"
        private const val KEY_INCOME_CATEGORIES = "income_categories"

        // Default categories
        private val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Food", "Housing", "Transportation", "Entertainment",
            "Shopping", "Utilities", "Health", "Education",
            "Personal Care", "Travel", "Bills", "Other"
        )

        private val DEFAULT_INCOME_CATEGORIES = listOf(
            "Salary", "Freelance", "Investments", "Gifts",
            "Refunds", "Rental Income", "Bonus", "Other"
        )
    }

    // Initialize categories if needed
    init {
        if (!sharedPreferences.contains(KEY_EXPENSE_CATEGORIES)) {
            saveExpenseCategories(DEFAULT_EXPENSE_CATEGORIES)
        }

        if (!sharedPreferences.contains(KEY_INCOME_CATEGORIES)) {
            saveIncomeCategories(DEFAULT_INCOME_CATEGORIES)
        }
    }

    // Get expense categories
    fun getExpenseCategories(): List<String> {
        val categoriesJson = sharedPreferences.getString(KEY_EXPENSE_CATEGORIES, null)
        val type = object : TypeToken<List<String>>() {}.type

        return if (categoriesJson != null) {
            gson.fromJson(categoriesJson, type)
        } else {
            DEFAULT_EXPENSE_CATEGORIES
        }
    }

    // Get income categories
    fun getIncomeCategories(): List<String> {
        val categoriesJson = sharedPreferences.getString(KEY_INCOME_CATEGORIES, null)
        val type = object : TypeToken<List<String>>() {}.type

        return if (categoriesJson != null) {
            gson.fromJson(categoriesJson, type)
        } else {
            DEFAULT_INCOME_CATEGORIES
        }
    }

    // Save expense categories
    fun saveExpenseCategories(categories: List<String>) {
        val editor = sharedPreferences.edit()
        val categoriesJson = gson.toJson(categories)
        editor.putString(KEY_EXPENSE_CATEGORIES, categoriesJson)
        editor.apply()
    }

    // Save income categories
    fun saveIncomeCategories(categories: List<String>) {
        val editor = sharedPreferences.edit()
        val categoriesJson = gson.toJson(categories)
        editor.putString(KEY_INCOME_CATEGORIES, categoriesJson)
        editor.apply()
    }

    // Add a new expense category
    fun addExpenseCategory(category: String): Boolean {
        if (category.isBlank()) return false

        val categories = getExpenseCategories().toMutableList()
        // Check if category already exists (case insensitive)
        if (categories.any { it.equals(category, ignoreCase = true) }) {
            return false
        }

        categories.add(category)
        saveExpenseCategories(categories)
        return true
    }

    // Add a new income category
    fun addIncomeCategory(category: String): Boolean {
        if (category.isBlank()) return false

        val categories = getIncomeCategories().toMutableList()
        // Check if category already exists (case insensitive)
        if (categories.any { it.equals(category, ignoreCase = true) }) {
            return false
        }

        categories.add(category)
        saveIncomeCategories(categories)
        return true
    }

    // Delete an expense category
    fun deleteExpenseCategory(category: String): Boolean {
        val categories = getExpenseCategories().toMutableList()
        val removed = categories.removeIf { it.equals(category, ignoreCase = true) }

        if (removed) {
            saveExpenseCategories(categories)
        }

        return removed
    }

    // Delete an income category
    fun deleteIncomeCategory(category: String): Boolean {
        val categories = getIncomeCategories().toMutableList()
        val removed = categories.removeIf { it.equals(category, ignoreCase = true) }

        if (removed) {
            saveIncomeCategories(categories)
        }

        return removed
    }
}