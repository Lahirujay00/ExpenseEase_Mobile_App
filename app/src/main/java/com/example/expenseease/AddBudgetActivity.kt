package com.example.expenseease

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseease.databinding.ActivityAddBudgetBinding
import com.example.expenseease.Budget

class AddBudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBudgetBinding
    private lateinit var budgetManager: BudgetManager
    private var editingBudgetId: String? = null  // Changed from Long to String?

    // Category options
    private val categories = listOf(
        "Food", "Transportation", "Housing", "Entertainment", "Shopping",
        "Utilities", "Health", "Education", "Travel", "Personal", "Bills", "Others"
    )

    // Period options
    private val periods = listOf(
        "Monthly", "Weekly", "Yearly"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize budget manager
        budgetManager = BudgetManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Check if we're editing an existing budget
        editingBudgetId = intent.getStringExtra("budget_id")  // Changed to getStringExtra
        if (editingBudgetId != null) {  // Changed comparison to null check
            // We're editing an existing budget
            setupForEditing()
        }

        // Setup UI elements
        setupCategoryDropdown()
        setupPeriodDropdown()
        setupSaveButton()
    }

    private fun setupForEditing() {
        // Change title
        supportActionBar?.title = "Edit Budget"

        // Get the budget we're editing
        val budget = budgetManager.getAllBudgets().find { it.id == editingBudgetId }
        if (budget != null) {
            // Fill fields with budget data
            binding.etAmount.setText(budget.amount.toString())
            binding.actvCategory.setText(budget.category, false)
            binding.actvPeriod.setText(budget.period, false)
            binding.etNotes.setText(budget.notes ?: "")  // Added notes field handling
        } else {
            // Budget not found
            Toast.makeText(this, "Error: Budget not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupPeriodDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periods)
        binding.actvPeriod.setAdapter(adapter)

        // Set default to Monthly
        if (editingBudgetId == null) {  // Changed comparison to null check
            binding.actvPeriod.setText("Monthly", false)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveBudget.setOnClickListener {
            if (validateInputs()) {
                saveBudget()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate amount
        val amountStr = binding.etAmount.text.toString().trim()
        if (TextUtils.isEmpty(amountStr)) {
            binding.tilAmount.error = "Please enter an amount"
            isValid = false
        } else {
            try {
                val amount = amountStr.toDouble()
                if (amount <= 0) {
                    binding.tilAmount.error = "Amount must be greater than zero"
                    isValid = false
                } else {
                    binding.tilAmount.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilAmount.error = "Please enter a valid amount"
                isValid = false
            }
        }

        // Validate category
        val category = binding.actvCategory.text.toString().trim()
        if (TextUtils.isEmpty(category)) {
            binding.tilCategory.error = "Please select a category"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }

        // Validate period
        val period = binding.actvPeriod.text.toString().trim()
        if (TextUtils.isEmpty(period)) {
            binding.tilPeriod.error = "Please select a budget period"
            isValid = false
        } else {
            binding.tilPeriod.error = null
        }

        return isValid
    }

    private fun saveBudget() {
        try {
            val amount = binding.etAmount.text.toString().toDouble()
            val category = binding.actvCategory.text.toString().trim()
            val period = binding.actvPeriod.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()

            // Check if a budget for this category already exists
            if (editingBudgetId == null) {
                val existingBudget = budgetManager.getBudgetByCategory(category)
                if (existingBudget != null) {
                    // A budget for this category already exists
                    Toast.makeText(
                        this,
                        "A budget for $category already exists. Edit the existing budget instead.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }

            // Create budget object
            val budget = Budget(
                id = editingBudgetId ?: "",
                category = category,
                amount = amount,
                period = period.lowercase(),
                createdTimestamp = System.currentTimeMillis(),
                isActive = true,
                notes = notes.ifEmpty { null }  // Add notes to the constructor
            )

            // Save to database
            val success = if (editingBudgetId != null) {
                budgetManager.updateBudget(budget)
            } else {
                budgetManager.addBudget(budget)
            }

            if (success) {
                Toast.makeText(
                    this,
                    "${if (editingBudgetId != null) "Budget updated" else "Budget created"} successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // Return to previous screen
            } else {
                Toast.makeText(this, "Error saving budget", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}