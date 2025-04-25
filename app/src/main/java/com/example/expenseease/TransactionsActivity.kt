package com.example.expenseease

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.expenseease.TransactionAdapter  // Updated import path
import com.example.expenseease.databinding.ActivityTransactionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionManager: TransactionManager
    private lateinit var adapter: TransactionAdapter

    private val calendar = Calendar.getInstance()
    private var currentFilter = "all"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize transaction manager
        transactionManager = TransactionManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title

        // Setup RecyclerView
        setupTransactionsList()

        // Setup UI components
        setupDateRangePicker()
        setupSearchAndFilters()
        setupFilterChips()
        setupFloatingActionButton()

        // Load transactions
        loadTransactions()
    }

    private fun setupTransactionsList() {
        val transactions = transactionManager.getAllTransactions()

        // Show sample data if no transactions exist yet
        val displayTransactions = if (transactions.isEmpty()) {
            // Get the most recent 5 transactions
            transactions.sortedByDescending { it.timestamp }.take(5)
        } else {
            transactions
        }

        adapter = TransactionAdapter(
            transactions = displayTransactions,
            onTransactionClickListener = { transaction ->
                // Launch transaction details activity
                val intent = Intent(this, TransactionDetailsActivity::class.java)
                intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, transaction.id)
                startActivity(intent)
            }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = this@TransactionsActivity.adapter
            isNestedScrollingEnabled = false
        }

        // Update summary amounts
        updateSummaryAmounts(displayTransactions)
    }

    private fun setupDateRangePicker() {
        // Set initial date display
        updateDateDisplay()

        // Setup date range selection
        binding.tvDateRange.setOnClickListener {
            showDatePicker()
        }

        binding.btnExpandDateRange.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, _ ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                updateDateDisplay()
                loadTransactions()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            1
        ).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvDateRange.text = dateFormat.format(calendar.time)
    }

    private fun setupSearchAndFilters() {
        // Search functionality
        binding.etSearch.doAfterTextChanged { text ->
            searchQuery = text.toString().trim()
            loadTransactions()
        }

        // Filter button
        binding.btnFilter.setOnClickListener {
            // Toggle filters visibility
            if (binding.chipScrollView.visibility == View.VISIBLE) {
                binding.chipScrollView.visibility = View.GONE
            } else {
                binding.chipScrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentFilter = "all"
                loadTransactions()
            }
        }

        binding.chipIncome.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentFilter = "income"
                loadTransactions()
            }
        }

        binding.chipExpenses.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentFilter = "expenses"
                loadTransactions()
            }
        }

        // Category filters
        val categoryChips = listOf(binding.chipFood, binding.chipBills, binding.chipShopping)
        categoryChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currentFilter = chip.text.toString().lowercase()
                    loadTransactions()
                }
            }
        }

        // More filters button
        binding.chipMore.setOnClickListener {
            showCategoryFilterDialog()
        }
    }

    private fun showCategoryFilterDialog() {
        val bottomSheet = BottomSheetDialog(this)
        // Implement a custom layout for bottom sheet with more category options
        // This is simplified for now
        Toast.makeText(this, "More filters coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun setupFloatingActionButton() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }

        binding.btnAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTransactions() {
        binding.swipeRefresh.isRefreshing = true

        // Get current month's start and end dates
        val startDate = Calendar.getInstance().apply {
            time = calendar.time
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val endDate = Calendar.getInstance().apply {
            time = calendar.time
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        // Get transactions from manager
        val allTransactions = transactionManager.getAllTransactions()

        // Filter by date range
        var filteredTransactions = allTransactions.filter {
            it.timestamp in startDate..endDate
        }

        // Apply category/type filter
        filteredTransactions = when (currentFilter) {
            "income" -> filteredTransactions.filter { it.isIncome }
            "expenses" -> filteredTransactions.filter { !it.isIncome }
            "all" -> filteredTransactions
            else -> filteredTransactions.filter {
                it.category.lowercase() == currentFilter.lowercase()
            }
        }

        // Apply search query if not empty
        if (searchQuery.isNotEmpty()) {
            filteredTransactions = filteredTransactions.filter { transaction ->
                transaction.title.contains(searchQuery, ignoreCase = true) ||
                        transaction.category.contains(searchQuery, ignoreCase = true) ||
                        transaction.description.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort by most recent
        val sortedTransactions = filteredTransactions.sortedByDescending { it.timestamp }

        // Update adapter with filtered transactions
        adapter.updateTransactions(sortedTransactions)

        // Update summary amounts
        updateSummaryAmounts(filteredTransactions)

        // Show/hide empty state
        if (sortedTransactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE

            // Customize empty message based on filters
            val emptyMessage = when {
                searchQuery.isNotEmpty() -> "No results found for '$searchQuery'"
                currentFilter != "all" -> "No $currentFilter transactions found"
                else -> "No transactions for ${binding.tvDateRange.text}"
            }
            binding.tvEmptyMessage.text = emptyMessage
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }

        binding.swipeRefresh.isRefreshing = false
    }

    private fun updateSummaryAmounts(transactions: List<Transaction>) {
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expenses

        binding.tvIncomeAmount.text = "$${String.format("%.2f", income)}"
        binding.tvExpenseAmount.text = "$${String.format("%.2f", expenses)}"
        binding.tvBalanceAmount.text = "$${String.format("%.2f", balance)}"
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the transaction
                transactionManager.deleteTransaction(transaction.id)
                // Refresh the transactions list
                loadTransactions()
                // Show success message
                Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        // Reload transactions when returning to this screen
        loadTransactions()
    }
}