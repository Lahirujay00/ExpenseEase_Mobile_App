package com.example.expenseease

import android.app.Dialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expenseease.BudgetAdapter
import com.example.expenseease.databinding.ActivityBudgetBinding
import com.example.expenseease.databinding.DialogBudgetBinding
import com.example.expenseease.CategoryManager
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var budgetManager: BudgetManager
    private lateinit var categoryManager: CategoryManager
    private lateinit var budgetAdapter: BudgetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        budgetManager = BudgetManager(this)
        categoryManager = CategoryManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvToolbarTitle.visibility = View.VISIBLE

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        binding.btnAddBudget.setOnClickListener {
            showBudgetDialog()
        }

        binding.btnAddFirstBudget.setOnClickListener {
            showBudgetDialog()
        }

        // Load data
        loadBudgets()
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(emptyList(), budgetManager) { budget ->
            showBudgetDialog(budget)
        }

        binding.rvBudgets.apply {
            layoutManager = LinearLayoutManager(this@BudgetActivity)
            adapter = budgetAdapter
        }
    }

    private fun loadBudgets() {
        val activeBudgets = budgetManager.getActiveBudgets().sortedBy { it.category }

        // Update adapter
        budgetAdapter.updateBudgets(activeBudgets)

        // Calculate overall budget summary
        updateBudgetSummary()

        // Show empty state if no budgets
        if (activeBudgets.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.rvBudgets.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvBudgets.visibility = View.VISIBLE
        }
    }

    private fun updateBudgetSummary() {
        val activeBudgets = budgetManager.getActiveBudgets()

        if (activeBudgets.isEmpty()) {
            binding.tvBudgetOverview.text = "No active budgets"
            binding.progressOverall.progress = 0
            binding.tvTotalBudget.text = "Total Budget: $0"
            binding.tvRemainingBudget.text = "Remaining: $0"
            return
        }

        val totalBudget = activeBudgets.sumOf { it.amount }
        val totalSpent = activeBudgets.sumOf { budgetManager.calculateBudgetSpending(it) }
        val totalRemaining = totalBudget - totalSpent
        val overallPercentage = if (totalBudget > 0) {
            ((totalSpent / totalBudget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Format values
        val formattedTotalBudget = String.format("%,.2f", totalBudget)
        val formattedTotalSpent = String.format("%,.2f", totalSpent)
        val formattedRemaining = String.format("%,.2f", totalRemaining)

        // Update UI
        binding.tvBudgetOverview.text = "You've spent $$formattedTotalSpent out of $$formattedTotalBudget"
        binding.progressOverall.progress = overallPercentage
        binding.tvTotalBudget.text = "Total Budget: $$formattedTotalBudget"

        binding.tvRemainingBudget.text = "Remaining: $$formattedRemaining"
        binding.tvRemainingBudget.setTextColor(
            getColor(if (totalRemaining < 0) R.color.expense_red else R.color.primary)
        )
    }

    private fun showBudgetDialog(budget: Budget? = null) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_budget)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etAmount = dialog.findViewById<TextInputEditText>(R.id.etAmount)
        val actvCategory = dialog.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val actvPeriod = dialog.findViewById<AutoCompleteTextView>(R.id.actvPeriod)
        val etNotes = dialog.findViewById<TextInputEditText>(R.id.etNotes)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        // Set dialog title based on whether we're editing or creating
        tvDialogTitle.text = if (budget == null) "Create New Budget" else "Edit Budget"

        // Show/hide delete button based on whether we're editing or creating
        btnDelete.visibility = if (budget == null) View.GONE else View.VISIBLE

        // If editing, populate fields with existing budget data
        if (budget != null) {
            etAmount.setText(budget.amount.toString())
            actvCategory.setText(budget.category, false)
            actvPeriod.setText(budget.period, false)
            etNotes.setText(budget.notes)
        }

        // Setup category dropdown
        val categories = listOf(
            "Food", "Transportation", "Housing", "Entertainment", "Shopping",
            "Utilities", "Health", "Education", "Travel", "Personal", "Bills", "Others"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        actvCategory.setAdapter(categoryAdapter)

        // Setup period dropdown
        val periods = arrayOf("Monthly", "Weekly", "Daily")
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periods)
        actvPeriod.setAdapter(periodAdapter)

        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(budget) {
                dialog.dismiss()
        }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = actvCategory.text.toString()
            val period = actvPeriod.text.toString()
            val notes = etNotes.text.toString()

            if (amount == null || category.isEmpty() || period.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (budget == null) {
                // Create new budget
                val newBudget = Budget(
                    amount = amount,
                    category = category,
                    period = period,
                    notes = notes
                )
                if (budgetManager.addBudget(newBudget)) {
                    Toast.makeText(this, "Budget created successfully", Toast.LENGTH_SHORT).show()
                    loadBudgets() // Refresh the UI
                } else {
                    Toast.makeText(this, "Failed to create budget", Toast.LENGTH_SHORT).show()
                }
            } else {
                    // Update existing budget
                val updatedBudget = budget.copy(
                    amount = amount,
                        category = category,
                        period = period,
                    notes = notes
                    )
                    if (budgetManager.updateBudget(updatedBudget)) {
                        Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                    loadBudgets() // Refresh the UI
                } else {
                    Toast.makeText(this, "Failed to update budget", Toast.LENGTH_SHORT).show()
                    }
                }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(budget: Budget?, onConfirmed: () -> Unit) {
        if (budget == null) return
        
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete this budget?")
            .setPositiveButton("Delete") { _, _ ->
                if (budgetManager.deleteBudget(budget.id)) {
                    Toast.makeText(this, "Budget deleted successfully", Toast.LENGTH_SHORT).show()
                    loadBudgets() // Refresh the UI
                    onConfirmed()
                } else {
                    Toast.makeText(this, "Failed to delete budget", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Refresh data when returning to this activity
    override fun onResume() {
        super.onResume()
        loadBudgets() // Refresh the budget list
    }

    // Define a warning color
    companion object {
        const val WARNING_YELLOW = "#FFC107"
    }
}