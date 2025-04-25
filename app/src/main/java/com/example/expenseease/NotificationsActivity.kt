package com.example.expenseease

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expenseease.databinding.ActivityNotificationsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var budgetManager: BudgetManager
    private lateinit var transactionManager: TransactionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        budgetManager = BudgetManager(this)
        transactionManager = TransactionManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvToolbarTitle.text = "Notifications"

        // Setup RecyclerView
        setupRecyclerView()

        // Load notifications
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(emptyList())
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }

    private fun loadNotifications() {
        val notifications = mutableListOf<NotificationItem>()

        // Check budget alerts
        val activeBudgets = budgetManager.getActiveBudgets()
        val totalSpent = transactionManager.getCurrentMonthExpense()
        val totalBudget = activeBudgets.sumOf { it.amount }
        
        if (totalBudget > 0) {
            val percentSpent = (totalSpent / totalBudget * 100).toInt()
            
            if (percentSpent >= 100) {
                notifications.add(
                    NotificationItem(
                        title = "Budget Exceeded!",
                        message = "You've spent $${String.format("%.2f", totalSpent)} of your $${String.format("%.2f", totalBudget)} monthly budget (${percentSpent}%)",
                        timestamp = System.currentTimeMillis(),
                        type = NotificationType.BUDGET_ALERT
                    )
                )
            } else if (percentSpent >= 90) {
                notifications.add(
                    NotificationItem(
                        title = "Budget Warning",
                        message = "You've used ${percentSpent}% of your monthly budget. $${String.format("%.2f", totalBudget - totalSpent)} remaining.",
                        timestamp = System.currentTimeMillis(),
                        type = NotificationType.BUDGET_WARNING
                    )
                )
            }
        }

        // Add daily reminder notification
        notifications.add(
            NotificationItem(
                title = "Daily Expense Reminder",
                message = "Don't forget to record your expenses for today!",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.DAILY_REMINDER
            )
        )

        // Update UI
        if (notifications.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
            notificationAdapter.updateNotifications(notifications)
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

data class NotificationItem(
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType
)

enum class NotificationType {
    BUDGET_ALERT,
    BUDGET_WARNING,
    DAILY_REMINDER
}

class NotificationAdapter(private var notifications: List<NotificationItem>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val tvTitle: android.widget.TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvMessage: android.widget.TextView = view.findViewById(R.id.tvNotificationMessage)
        val tvTime: android.widget.TextView = view.findViewById(R.id.tvNotificationTime)
        val iconType: android.widget.ImageView = view.findViewById(R.id.ivNotificationIcon)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        holder.tvTime.text = dateFormat.format(Date(notification.timestamp))

        // Set icon based on notification type
        val iconRes = when (notification.type) {
            NotificationType.BUDGET_ALERT -> R.drawable.ic_warning
            NotificationType.BUDGET_WARNING -> R.drawable.ic_warning
            NotificationType.DAILY_REMINDER -> R.drawable.ic_notification
        }
        holder.iconType.setImageResource(iconRes)

        // Set text color based on type
        val textColor = when (notification.type) {
            NotificationType.BUDGET_ALERT -> holder.itemView.context.getColor(R.color.expense_red)
            NotificationType.BUDGET_WARNING -> holder.itemView.context.getColor(R.color.warning_yellow)
            NotificationType.DAILY_REMINDER -> holder.itemView.context.getColor(R.color.primary)
        }
        holder.tvTitle.setTextColor(textColor)
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
} 