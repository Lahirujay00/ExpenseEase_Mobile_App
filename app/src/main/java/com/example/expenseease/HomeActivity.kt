package com.example.expenseease

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log  // Add this import
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expenseease.databinding.ActivityHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionManager: TransactionManager
    private lateinit var notificationManager: ExpenseNotificationManager
    private lateinit var budgetManager: BudgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize shared preference manager and transaction manager
        sharedPreferenceManager = SharedPreferenceManager(this)
        transactionManager = TransactionManager(this)
        notificationManager = ExpenseNotificationManager(this)
        budgetManager = BudgetManager(this)

        // Setup UI components
        setupWelcomeMessage()
        setupDate()
        setupTransactionsList()
        setupBottomNavigation()
        setupClickListeners()

        // Initialize notification system
        setupNotifications()

        // Apply animations
        applyAnimations()
    }

    // Add notification setup method
    private fun setupNotifications() {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_NOTIFICATIONS
                )
            }
        }

        // Schedule daily budget check
        val budgetCheckWork = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "budget_check",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheckWork
        )

        // Schedule daily reminder
        notificationManager.scheduleDailyReminder()

        // Check budget thresholds immediately
        notificationManager.checkBudgetThresholds()
    }

    private fun setupWelcomeMessage() {
        val userName = sharedPreferenceManager.getCurrentUserName() ?: "User"
        binding.tvWelcome.text = "Welcome, $userName!"
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
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

        transactionAdapter = TransactionAdapter(
            transactions = displayTransactions,
            onTransactionClickListener = { transaction ->
                // Launch transaction details activity
                val intent = Intent(this, TransactionDetailsActivity::class.java)
                intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, transaction.id)
                startActivity(intent)
            }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }

        // Update balance, income and expense amounts
        updateBalanceCard()
    }

    private fun updateBalanceCard() {
        val income = transactionManager.getCurrentMonthIncome()
        val expense = transactionManager.getCurrentMonthExpense()
        val balance = income - expense

        binding.tvBalanceAmount.text = "$${String.format("%.2f", balance)}"
        binding.tvIncomeAmount.text = "$${String.format("%.2f", income)}"
        binding.tvExpenseAmount.text = "$${String.format("%.2f", expense)}"
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.navigation_transactions -> {
                    navigateToTransactions()
                    true
                }
                R.id.navigation_budget -> {
                    navigateToBudget()
                    true
                }
                R.id.navigation_analysis -> {
                    navigateToAnalytics()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToTransactions() {
        val intent = Intent(this, TransactionsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToBudget() {
        val intent = Intent(this, BudgetActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToAnalytics() {
        val intent = Intent(this, AnalyticsActivity::class.java)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        // FAB for adding transactions
        binding.fabAddTransaction.setOnClickListener {
            // Show a nice scale animation when clicking
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down_button)
            binding.fabAddTransaction.startAnimation(scaleDown)

            // Launch Add Transaction activity
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }

        // Quick action buttons
        binding.cardAddIncome.setOnClickListener {
            // Launch Add Transaction activity with income pre-selected
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("is_income", true)
            startActivity(intent)
        }

        binding.cardAddExpense.setOnClickListener {
            // Launch Add Transaction activity with expense pre-selected
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("is_income", false)
            startActivity(intent)
        }

        // View all transactions
        binding.tvViewAll.setOnClickListener {
            navigateToTransactions()
        }

        // Top bar actions
        binding.cardSearch.setOnClickListener {
            Toast.makeText(this, "Search Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Notification button - show notifications
        binding.btnNotification.setOnClickListener {
            // Show notifications
            showNotifications()
        }

        // Menu button - includes notification settings
        binding.btnMenu.setOnClickListener {
            showMenu()
        }
        
        // Test notification button (long press on notification icon)
        binding.btnNotification.setOnLongClickListener {
            // Test budget notification
            val totalSpent = transactionManager.getCurrentMonthExpense()
            val totalBudget = 2000.0 // Example budget
            val percentSpent = (totalSpent / totalBudget * 100).roundToInt()
            
            if (percentSpent >= 100) {
                notificationManager.sendMonthlyBudgetNotification(totalSpent, totalBudget, percentSpent)
            } else if (percentSpent >= 90) {
                notificationManager.sendBudgetWarningNotification(totalSpent, totalBudget, percentSpent)
            } else {
                // Force a test notification
                notificationManager.sendBudgetWarningNotification(totalSpent, totalBudget, 95)
            }
            
            Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showNotifications() {
        // Navigate to notifications activity
        val intent = Intent(this, NotificationsActivity::class.java)
        startActivity(intent)
    }

    private fun showMenu() {
        // Navigate directly to ProfileActivity
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    // Handle permission result for notifications
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, schedule notifications
                setupNotifications()
            } else {
                // Permission denied - show a message that functionality will be limited
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive budget alerts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun applyAnimations() {
        val balanceCardAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.cardBalance.startAnimation(balanceCardAnim)

        // Animate quick actions with a delay
        val quickActionsAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        quickActionsAnim.startOffset = 200
        binding.tvQuickActions.startAnimation(quickActionsAnim)
        binding.layoutQuickActions.startAnimation(quickActionsAnim)

        // Animate transactions section with a delay
        val transactionsAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        transactionsAnim.startOffset = 400
        binding.tvRecentTransactions.startAnimation(transactionsAnim)
        binding.tvViewAll.startAnimation(transactionsAnim)
        binding.rvTransactions.startAnimation(transactionsAnim)

        // Animate budget card with a delay
        val budgetAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        budgetAnim.startOffset = 600
        binding.cardBudget.startAnimation(budgetAnim)

        // Animate bottom navigation
        val bottomNavAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.bottomNavigation.startAnimation(bottomNavAnim)

        // Animate FAB with bounce
        val fabAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_up)
        fabAnim.startOffset = 300
        binding.fabAddTransaction.startAnimation(fabAnim)
    }

    // Refresh data when returning to this activity
    override fun onResume() {
        super.onResume()
        // Refresh transaction list and balance in case data changed
        setupTransactionsList()
        // Refresh welcome message
        setupWelcomeMessage()
        // Update budget progress
        updateBudgetProgress()
    }

    // Update budget progress if you have budget functionality
    private fun updateBudgetProgress() {
        // Get the current month's expense
        val currentExpense = transactionManager.getCurrentMonthExpense()

        // Get active budgets
        val activeBudgets = budgetManager.getActiveBudgets()
        
        // Calculate total budget
        val monthlyBudget = activeBudgets.sumOf { it.amount }

        // Calculate percentages
        val percentage = if (monthlyBudget > 0) {
            (currentExpense / monthlyBudget * 100).toInt()
        } else {
            0
        }
        val remaining = monthlyBudget - currentExpense

        // Update UI
        binding.tvBudgetProgress.text = "$${String.format("%.0f", currentExpense)} of $${String.format("%.0f", monthlyBudget)}"
        binding.tvBudgetRemaining.text = "$${String.format("%.0f", remaining)} remaining"
        binding.budgetProgressBar.progress = percentage.coerceIn(0, 100)
        
        // Update progress bar color based on percentage
        val progressColor = when {
            percentage > 90 -> R.color.expense_red
            percentage > 75 -> R.color.warning_yellow
            else -> R.color.primary
        }
        binding.budgetProgressBar.setIndicatorColor(getColor(progressColor))
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the transaction
                transactionManager.deleteTransaction(transaction.id)
                // Refresh the transactions list
                setupTransactionsList()
                // Show success message
                Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val PERMISSION_REQUEST_NOTIFICATIONS = 100
    }
}