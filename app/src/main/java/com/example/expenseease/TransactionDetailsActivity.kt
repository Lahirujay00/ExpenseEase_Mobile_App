package com.example.expenseease

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseease.databinding.ActivityTransactionDetailsBinding
import com.example.expenseease.CategoryManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }

    private lateinit var binding: ActivityTransactionDetailsBinding
    private lateinit var transactionManager: TransactionManager
    private lateinit var categoryManager: CategoryManager

    private var transactionId: Long = -1
    private var currentTransaction: Transaction? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var isIncome: Boolean = false

    // Date formatter for display
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        transactionManager = TransactionManager(this)
        categoryManager = CategoryManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get transaction ID from intent
        transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1)

        // Load transaction if editing existing one
        if (transactionId != -1L) {
            loadTransaction()
        }

        // Setup UI components
        setupTypeToggle()
        setupCategoryDropdown()
        setupDatePicker()
        setupClickListeners()
    }

    private fun loadTransaction() {
        currentTransaction = transactionManager.getTransactionById(transactionId)
        currentTransaction?.let {
            // Set UI based on transaction data
            binding.etTitle.setText(it.title)
            binding.etAmount.setText(String.format("%.2f", it.amount))
            binding.actvCategory.setText(it.category)
            binding.etDescription.setText(it.description)

            // Set date
            selectedDate.timeInMillis = it.timestamp
            binding.etDate.setText(dateFormatter.format(Date(it.timestamp)))

            // Set type
            isIncome = it.isIncome
            updateTypeToggle()
        }
    }

    private fun setupTypeToggle() {
        binding.rbExpense.setOnClickListener {
            isIncome = false
            updateTypeToggle()
            updateCategoryDropdown()
        }

        binding.rbIncome.setOnClickListener {
            isIncome = true
            updateTypeToggle()
            updateCategoryDropdown()
        }

        // Set initial state
        updateTypeToggle()
    }

    private fun updateTypeToggle() {
        if (isIncome) {
            binding.rbIncome.isChecked = true
            binding.rbExpense.isChecked = false
        } else {
            binding.rbExpense.isChecked = true
            binding.rbIncome.isChecked = false
        }
    }

    private fun setupCategoryDropdown() {
        updateCategoryDropdown()

        // Set listener to handle selection
        binding.actvCategory.setOnItemClickListener { _, _, _, _ ->
            // Nothing special needed here, the selection is automatic
        }
    }

    private fun updateCategoryDropdown() {
        // Get appropriate category list based on transaction type
        val categories = if (isIncome) {
            categoryManager.getIncomeCategories()
        } else {
            categoryManager.getExpenseCategories()
        }

        // Create and set adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        // Show date picker when clicking on the date field
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Set initial date
        binding.etDate.setText(dateFormatter.format(selectedDate.time))
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.etDate.setText(dateFormatter.format(selectedDate.time))
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )

        // Don't allow future dates
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun setupClickListeners() {
        // Save button
        binding.btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }

        // Delete button
        binding.btnDeleteTransaction.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        if (transactionId != -1L) {
            val success = transactionManager.deleteTransaction(transactionId)
            if (success) {
                Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTransaction() {
        // Validate inputs
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            return
        } else {
            binding.tilTitle.error = null
        }

        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "Amount cannot be empty"
            return
        } else {
            binding.tilAmount.error = null
        }

        if (category.isEmpty()) {
            binding.tilCategory.error = "Please select a category"
            return
        } else {
            binding.tilCategory.error = null
        }

        try {
            val amount = amountStr.toDouble()
            if (amount <= 0) {
                binding.tilAmount.error = "Amount must be greater than zero"
                return
            }

            // Create or update transaction
            if (currentTransaction != null) {
                // Update existing transaction
                val updatedTransaction = currentTransaction!!.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = selectedDate.timeInMillis,
                    isIncome = isIncome
                )

                val success = transactionManager.updateTransaction(updatedTransaction)
                if (success) {
                    Toast.makeText(this, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update transaction", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Create new transaction
                val newTransaction = Transaction(
                    id = 0, // Will be assigned by TransactionManager
                    title = title,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = selectedDate.timeInMillis,
                    isIncome = isIncome
                )

                val id = transactionManager.addTransaction(newTransaction)
                if (id > 0) {
                    Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to add transaction", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: NumberFormatException) {
            binding.tilAmount.error = "Please enter a valid amount"
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