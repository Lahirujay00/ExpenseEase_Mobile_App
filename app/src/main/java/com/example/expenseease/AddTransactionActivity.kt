package com.example.expenseease

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseease.TransactionManager
import com.example.expenseease.ExpenseNotificationManager
import com.example.expenseease.databinding.ActivityAddTransactionBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionManager: TransactionManager
    private lateinit var notificationManager: ExpenseNotificationManager

    private val calendar = Calendar.getInstance()
    private var isIncome = false
    private var isEditing = false
    private var existingTransactionId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        transactionManager = TransactionManager(this)
        notificationManager = ExpenseNotificationManager(this)

        // Check if we're editing an existing transaction
        existingTransactionId = intent.getLongExtra("transaction_id", 0L)
        isEditing = existingTransactionId > 0

        // Check if income is pre-selected
        if (intent.hasExtra("is_income")) {
            isIncome = intent.getBooleanExtra("is_income", false)
            binding.rgTransactionType.check(if (isIncome) R.id.rbIncome else R.id.rbExpense)
        }

        setupToolbar()
        setupTransactionTypeSelection()
        setupDateTimePicker()
        setupCategoryDropdown()
        setupPaymentMethodDropdown()
        setupRecurringSwitch()
        setupSaveButton()

        // If editing, populate form with existing data
        if (isEditing) {
            loadExistingTransaction()
        } else {
            // Set current date and time for new transactions
            updateDateTimeField()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditing) "Edit Transaction" else "Add Transaction"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupTransactionTypeSelection() {
        binding.rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            isIncome = checkedId == R.id.rbIncome
            updateCategoryDropdown()
        }
    }

    private fun setupDateTimePicker() {
        binding.etDateTime.setOnClickListener {
            showDateTimePicker()
        }

        // Set initial value to current time
        updateDateTimeField()
    }

    private fun updateDateTimeField() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.etDateTime.setText(dateFormat.format(calendar.time))
    }

    private fun showDateTimePicker() {
        // Show Date Picker
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // After date is set, show Time Picker
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        // Show Time Picker after date is selected
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                // Update the text field with the new date and time
                updateDateTimeField()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun setupCategoryDropdown() {
        updateCategoryDropdown()
    }

    private fun updateCategoryDropdown() {
        val categories = if (isIncome) {
            Transaction.DEFAULT_INCOME_CATEGORIES
        } else {
            Transaction.DEFAULT_EXPENSE_CATEGORIES
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        (binding.actvCategory as? AutoCompleteTextView)?.setAdapter(adapter)

        // Clear current selection if changing between income/expense
        if (binding.actvCategory.text.isNotEmpty()) {
            binding.actvCategory.setText("")
        }
    }

    private fun setupPaymentMethodDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            Transaction.PAYMENT_METHODS
        )

        (binding.actvPaymentMethod as? AutoCompleteTextView)?.setAdapter(adapter)
        binding.actvPaymentMethod.setText(Transaction.PAYMENT_METHODS[0], false)
    }

    private fun setupRecurringSwitch() {
        val recurringTypes = listOf("Daily", "Weekly", "Monthly", "Yearly")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            recurringTypes
        )

        (binding.actvRecurringType as? AutoCompleteTextView)?.setAdapter(adapter)
        binding.actvRecurringType.setText(recurringTypes[2], false)

        // Show/hide recurring options based on switch state
        binding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            binding.tilRecurringType.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate amount
        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else {
            try {
                val amount = amountStr.toDouble()
                if (amount <= 0) {
                    binding.tilAmount.error = "Amount must be greater than 0"
                    isValid = false
                } else {
                    binding.tilAmount.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilAmount.error = "Invalid amount format"
                isValid = false
            }
        }

        // Validate title
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        // Validate category
        val category = binding.actvCategory.text.toString().trim()
        if (category.isEmpty()) {
            binding.tilCategory.error = "Category is required"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }

        return isValid
    }

    private fun saveTransaction() {
        // Extract values from form
        val amount = binding.etAmount.text.toString().toDouble()
        val title = binding.etTitle.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val paymentMethod = binding.actvPaymentMethod.text.toString().trim()
        val timestamp = calendar.timeInMillis

        // Handle recurring info
        val recurringType = if (binding.switchRecurring.isChecked) {
            binding.actvRecurringType.text.toString().trim()
        } else {
            "None"
        }

        // Create transaction object
        val transaction = Transaction(
            id = if (isEditing) existingTransactionId else 0,
            title = title,
            amount = amount,
            category = category,
            description = description,
            timestamp = timestamp,
            isIncome = isIncome,
            paymentMethod = paymentMethod,
            recurringType = recurringType
        )

        // Save transaction
        val success = if (isEditing) {
            transactionManager.updateTransaction(transaction)
        } else {
            transactionManager.addTransaction(transaction) > 0
        }

        if (success) {
            // If this is an expense, check budget thresholds
            if (!transaction.isIncome) {
                notificationManager.checkBudgetThresholds()
            }

            // Show success message and finish activity
            Snackbar.make(
                binding.root,
                if (isEditing) "Transaction updated successfully" else "Transaction added successfully",
                Snackbar.LENGTH_SHORT
            ).show()

            // Wait a moment before going back to give time for the snackbar to be visible
            binding.root.postDelayed({ finish() }, 800)
        } else {
            Snackbar.make(
                binding.root,
                "Failed to save transaction. Please try again.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun loadExistingTransaction() {
        val transaction = transactionManager.getTransactionById(existingTransactionId)
        if (transaction != null) {
            // Set transaction type
            isIncome = transaction.isIncome
            binding.rgTransactionType.check(if (isIncome) R.id.rbIncome else R.id.rbExpense)

            // Set amount
            binding.etAmount.setText(transaction.amount.toString())

            // Set title
            binding.etTitle.setText(transaction.title)

            // Set description
            binding.etDescription.setText(transaction.description)

            // Set date and time
            calendar.timeInMillis = transaction.timestamp
            updateDateTimeField()

            // Set category (need to update dropdown first)
            updateCategoryDropdown()
            binding.actvCategory.setText(transaction.category, false)

            // Set payment method
            binding.actvPaymentMethod.setText(transaction.paymentMethod, false)

            // Set recurring information
            val isRecurring = transaction.recurringType != "None"
            binding.switchRecurring.isChecked = isRecurring
            if (isRecurring) {
                binding.tilRecurringType.visibility = View.VISIBLE
                binding.actvRecurringType.setText(transaction.recurringType, false)
            }
        } else {
            // Transaction not found, show error and finish
            Snackbar.make(
                binding.root,
                "Error: Transaction not found",
                Snackbar.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}