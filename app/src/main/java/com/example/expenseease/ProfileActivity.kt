package com.example.expenseease

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.expenseease.databinding.ActivityProfileBinding
import com.example.expenseease.SharedPreferenceManager
import com.example.expenseease.UserProfileManager
import com.example.expenseease.ExpenseNotificationManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var userProfileManager: UserProfileManager
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var expenseNotificationManager: ExpenseNotificationManager

    // Image picker result
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                // Save the URI to user profile
                userProfileManager.setProfilePictureUri(imageUri.toString())
                // Update the UI
                binding.ivProfilePic.setImageURI(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Preferences"

        // Initialize managers
        sharedPreferenceManager = SharedPreferenceManager(this)
        expenseNotificationManager = ExpenseNotificationManager(this)

        // Initialize profile manager
        userProfileManager = UserProfileManager(this)
        userProfileManager.initializeDefaultProfileIfNeeded()

        // Load user data
        loadUserData()

        // Set up UI listeners
        setupClickListeners()

        // Set up currency dropdown
        setupCurrencyDropdown()

        // Set up app version
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        binding.tvAppVersion.text = packageInfo.versionName

        // Setup UI
        setupProfileSection()
        setupNotificationSettings()
        setupDarkMode()
    }

    private fun loadUserData() {
        // Load name and email
        binding.etName.setText(userProfileManager.getUserName())
        binding.etEmail.setText(userProfileManager.getUserEmail())
        
        // Load profile picture if exists
        val profilePictureUri = userProfileManager.getProfilePictureUri()
        if (!profilePictureUri.isNullOrEmpty()) {
            try {
                binding.ivProfilePic.setImageURI(Uri.parse(profilePictureUri))
            } catch (e: Exception) {
                // If there's an error loading the image, use default
                userProfileManager.setProfilePictureUri(null)
            }
        }

        // Load currency
        binding.actvCurrency.setText(
            userProfileManager.getAvailableCurrencies().find {
                it.startsWith(userProfileManager.getUserCurrency())
            }
        )

        // Load dark mode setting
        //binding.switchDarkMode.isChecked = userProfileManager.isDarkModeEnabled()
    }

    private fun setupClickListeners() {
        // Change profile picture
        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Save profile changes
        binding.btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        // Toggle dark mode
       // binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
        //    userProfileManager.setDarkModeEnabled(isChecked)
            // Apply dark mode immediately
          //  if (isChecked) {
          //      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
         //   } else {
        //        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        //    }
      //  }

        // Backup & Restore
        binding.layoutBackup.setOnClickListener {
            val intent = Intent(this, BackupActivity::class.java)
            startActivity(intent)
        }

        // Help & Support
        binding.layoutHelpSupport.setOnClickListener {
            Toast.makeText(this, "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        // Privacy Policy
        binding.layoutPrivacyPolicy.setOnClickListener {
            Toast.makeText(this, "Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
        }

        // Terms of Service
        binding.layoutTerms.setOnClickListener {
            Toast.makeText(this, "Terms of Service coming soon", Toast.LENGTH_SHORT).show()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun setupCurrencyDropdown() {
        val currencies = userProfileManager.getAvailableCurrencies()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        binding.actvCurrency.setAdapter(adapter)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveUserProfile() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name cannot be empty"
            return
        } else {
            binding.tilName.error = null
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            return
        } else {
            binding.tilEmail.error = null
        }

        // Save currency selection
        val currencySelection = binding.actvCurrency.text.toString()
        val currencyCode = currencySelection.split(" - ").firstOrNull() ?: "USD"

        // Update profile
        userProfileManager.setUserName(name)
        userProfileManager.setUserEmail(email)
        userProfileManager.setUserCurrency(currencyCode)

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupNotificationSettings() {
        // Set initial states
        binding.switchDailyReminder.isChecked = expenseNotificationManager.isDailyReminderEnabled()
        binding.switchBudgetAlert.isChecked = expenseNotificationManager.isBudgetAlertEnabled()

        // Setup listeners
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            expenseNotificationManager.setDailyReminderEnabled(isChecked)
        }

        binding.switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            expenseNotificationManager.setBudgetAlertEnabled(isChecked)
        }
    }

    private fun setupDarkMode() {
        // Dark mode option removed
    }

    private fun setupProfileSection() {
        // Load user data
        loadUserData()
    }

    // UPDATED: Complete logout implementation that redirects to SplashActivity
    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Perform logout
                userProfileManager.clearUserData()
                sharedPreferenceManager.logoutUser()

                // Clear activities and redirect to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
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
}