package com.example.expenseease

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseease.databinding.ActivityNotificationSettingsBinding

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notification Settings"

        // Load current settings
        loadCurrentSettings()

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences("notification_preferences", MODE_PRIVATE)

        // Load daily reminder setting
        binding.switchDailyReminder.isChecked = prefs.getBoolean("daily_reminder_enabled", false)
        
        // Load budget alert setting
        binding.switchBudgetAlert.isChecked = prefs.getBoolean("budget_alert_enabled", true)
    }

    private fun setupClickListeners() {
        // Daily reminder switch
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("notification_preferences", MODE_PRIVATE)
                .edit()
                .putBoolean("daily_reminder_enabled", isChecked)
                .apply()
            
            if (isChecked) {
                Toast.makeText(this, "Daily reminders enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Daily reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Budget alert switch
        binding.switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("notification_preferences", MODE_PRIVATE)
                .edit()
                .putBoolean("budget_alert_enabled", isChecked)
                .apply()
            
            if (isChecked) {
                Toast.makeText(this, "Budget alerts enabled", Toast.LENGTH_SHORT).show()
                } else {
                Toast.makeText(this, "Budget alerts disabled", Toast.LENGTH_SHORT).show()
        }
    }

        // Back button in toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
            onBackPressed()
            return true
    }
}