package com.example.expenseease

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ExpenseNotificationManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        NOTIFICATION_PREF, Context.MODE_PRIVATE
    )

    private val budgetManager = BudgetManager(context)
    private val transactionManager = TransactionManager(context)
    private val gson = Gson()

    init {
        createNotificationChannels()
        scheduleBudgetCheck()
    }

    // Create notification channels for Android O and above
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels")
            
            // Budget alerts channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when you exceed your monthly budget"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Allow notifications to bypass Do Not Disturb
            }

            // Daily reminder channel
            val reminderChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record your expenses"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Register the channels with the system
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(budgetChannel, reminderChannel))
            
            Log.d(TAG, "Notification channels created successfully")
        } else {
            Log.d(TAG, "Android version below O, no need to create channels")
        }
    }

    // Schedule budget check to run every hour
    fun scheduleBudgetCheck() {
        // Cancel any existing budget check work
        WorkManager.getInstance(context).cancelUniqueWork("budget_check")
        
        val budgetCheckWork = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "budget_check",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            budgetCheckWork
        )
        
        Log.d(TAG, "Budget check rescheduled to run every hour")
        
        // Check budget thresholds immediately
        checkBudgetThresholds()
    }

    // Check monthly budget limit
    fun checkBudgetThresholds() {
        val activeBudgets = budgetManager.getActiveBudgets()
        Log.d(TAG, "Checking ${activeBudgets.size} active budgets")

        if (activeBudgets.isEmpty()) {
            Log.d(TAG, "No active budgets found")
            return
        }

        // Check if budget alerts are enabled
        val budgetAlertsEnabled = sharedPreferences.getBoolean("budget_alert_enabled", true)
        if (!budgetAlertsEnabled) {
            Log.d(TAG, "Budget alerts are disabled")
            return
        }

        // Calculate total monthly spending
        val totalSpent = transactionManager.getCurrentMonthExpense()
        val totalBudget = activeBudgets.sumOf { it.amount }

        if (totalBudget > 0) {
            val percentSpent = (totalSpent / totalBudget * 100).roundToInt()
            
            // Check if we should notify
            val lastNotified = getLastMonthlyNotification()
            val now = System.currentTimeMillis()
            
            // Check if this is a forced check (from budget update)
            val isForcedCheck = sharedPreferences.getBoolean("force_budget_check", false)
            Log.d(TAG, "Forced check: $isForcedCheck, Last notified: $lastNotified")

            // Only notify once per day unless it's a forced check
            if (isForcedCheck || lastNotified == 0L || now - lastNotified > TimeUnit.DAYS.toMillis(1)) {
                when {
                    percentSpent >= 100 -> {
                        // Budget exceeded
                        Log.d(TAG, "Budget exceeded: $percentSpent%")
                        sendMonthlyBudgetNotification(totalSpent, totalBudget, percentSpent)
                        saveLastMonthlyNotification(now)
                    }
                    percentSpent >= 90 -> {
                        // Budget warning (90% used)
                        Log.d(TAG, "Budget warning: $percentSpent%")
                        sendBudgetWarningNotification(totalSpent, totalBudget, percentSpent)
                        saveLastMonthlyNotification(now)
                    }
                    else -> {
                        Log.d(TAG, "Budget within limits: $percentSpent%")
                    }
                }
                
                // Reset the forced check flag
                if (isForcedCheck) {
                    sharedPreferences.edit().putBoolean("force_budget_check", false).apply()
                    Log.d(TAG, "Forced check flag reset")
                }
            } else {
                Log.d(TAG, "Skipping notification due to time constraint")
            }
        }
    }

    // Send a monthly budget notification
    fun sendMonthlyBudgetNotification(spent: Double, budget: Double, percentSpent: Int) {
        Log.d(TAG, "Attempting to send monthly budget notification")
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted")
                return
            }
        }

        // Create intent for notification tap action
        val intent = Intent(context, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Monthly Budget Exceeded!"
        val message = "You've spent $${String.format("%.2f", spent)} of your $${String.format("%.2f", budget)} monthly budget (${percentSpent}%)"

        // Show toast message
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        Log.d(TAG, "Toast message shown: $message")

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.expense_red))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(context.getColor(R.color.expense_red), 3000, 3000)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setTicker("Budget Alert: $message")
            .setFullScreenIntent(pendingIntent, true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_MONTHLY_BUDGET, builder.build())
                Log.d(TAG, "Monthly budget notification sent successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for sending notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }

    // Send a budget warning notification
    fun sendBudgetWarningNotification(spent: Double, budget: Double, percentSpent: Int) {
        Log.d(TAG, "Attempting to send budget warning notification")
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted")
                return
            }
        }

        // Create intent for notification tap action
        val intent = Intent(context, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Budget Warning"
        val message = "You've used ${percentSpent}% of your monthly budget. $${String.format("%.2f", budget - spent)} remaining."

        // Show toast message
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        Log.d(TAG, "Toast message shown: $message")

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.warning_yellow))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(context.getColor(R.color.warning_yellow), 3000, 3000)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setTicker("Budget Warning: $message")
            .setFullScreenIntent(pendingIntent, true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_BUDGET_WARNING, builder.build())
                Log.d(TAG, "Budget warning notification sent successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for sending notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }

    // Schedule daily expense reminder
    fun scheduleDailyReminder() {
        val reminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
        
        Log.d(TAG, "Daily reminder scheduled")
        }

    // Get the last time we sent a monthly notification
    private fun getLastMonthlyNotification(): Long {
        return sharedPreferences.getLong(KEY_LAST_MONTHLY_NOTIFICATION, 0)
    }

    // Save the time of the last monthly notification
    private fun saveLastMonthlyNotification(time: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_MONTHLY_NOTIFICATION, time).apply()
    }

    // Check if daily reminder is enabled
    fun isDailyReminderEnabled(): Boolean {
        return sharedPreferences.getBoolean("daily_reminder_enabled", false)
    }

    // Set daily reminder enabled state
    fun setDailyReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        if (enabled) {
            scheduleDailyReminder()
        } else {
            cancelDailyReminder()
        }
    }

    // Cancel daily reminder
    fun cancelDailyReminder() {
        WorkManager.getInstance(context).cancelUniqueWork("daily_reminder")
        Log.d(TAG, "Daily reminder cancelled")
    }

    // Check if budget alerts are enabled
    fun isBudgetAlertEnabled(): Boolean {
        return sharedPreferences.getBoolean("budget_alert_enabled", true)
    }

    // Set budget alerts enabled state
    fun setBudgetAlertEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("budget_alert_enabled", enabled).apply()
        if (enabled) {
            scheduleBudgetCheck()
        } else {
            cancelBudgetCheck()
        }
    }

    // Cancel budget check
    fun cancelBudgetCheck() {
        WorkManager.getInstance(context).cancelUniqueWork("budget_check")
        Log.d(TAG, "Budget check cancelled")
    }

    companion object {
        private const val TAG = "ExpNotificationManager"
        private const val NOTIFICATION_PREF = "notification_preferences"
        private const val KEY_LAST_MONTHLY_NOTIFICATION = "last_monthly_notification"
        const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
        const val CHANNEL_DAILY_REMINDER = "daily_reminder"
        const val NOTIFICATION_ID_MONTHLY_BUDGET = 1000
        const val NOTIFICATION_ID_BUDGET_WARNING = 1001
        const val NOTIFICATION_ID_DAILY_REMINDER = 1002
    }
}

// Worker for daily expense reminder
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("DailyReminderWorker", "Starting daily reminder check")

        // Check if we should send the reminder (e.g., if user has enabled it)
        val sharedPreferences = applicationContext.getSharedPreferences("notification_preferences", Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferences.getBoolean("daily_reminder_enabled", false)

        if (!dailyReminderEnabled) {
            Log.d("DailyReminderWorker", "Daily reminder is disabled")
            return Result.success()
        }

        // Create intent for notification tap action
        val intent = Intent(applicationContext, AddTransactionActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Daily Expense Reminder"
        val message = "Don't forget to record your expenses for today!"

        // Show toast message
        android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_LONG).show()
        Log.d("DailyReminderWorker", "Toast message shown: $message")

        val builder = NotificationCompat.Builder(applicationContext, ExpenseNotificationManager.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(applicationContext.getColor(R.color.primary))
            .setVibrate(longArrayOf(0, 250, 250, 250)) // Add vibration pattern
            .setLights(applicationContext.getColor(R.color.primary), 3000, 3000) // Add LED light

        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(ExpenseNotificationManager.NOTIFICATION_ID_DAILY_REMINDER, builder.build())
                Log.d("DailyReminderWorker", "Daily reminder notification sent successfully")
            }
        } catch (e: SecurityException) {
            Log.e("DailyReminderWorker", "Permission denied for sending notification", e)
        } catch (e: Exception) {
            Log.e("DailyReminderWorker", "Error sending notification", e)
        }

        return Result.success()
    }
}