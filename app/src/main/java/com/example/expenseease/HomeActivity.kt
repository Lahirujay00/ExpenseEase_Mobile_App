package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseease.databinding.ActivityHomeBinding
import com.example.expenseease.utils.SharedPreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Home"

        // Initialize shared preference manager
        sharedPreferenceManager = SharedPreferenceManager(this)

        // Check if user is logged in, if not redirect to login
        if (!sharedPreferenceManager.isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // Display welcome message with user name
        val userName = sharedPreferenceManager.getCurrentUserName() ?: "User"
        binding.tvWelcome.text = "Welcome, $userName!"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> logoutUser() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutUser() {
        // Clear login state
        sharedPreferenceManager.logoutUser()

        // Navigate to login screen
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}