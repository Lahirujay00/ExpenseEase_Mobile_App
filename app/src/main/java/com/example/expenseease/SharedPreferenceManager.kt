package com.example.expenseease.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SharedPreferenceManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "ExpenseEasePrefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "SharedPrefManager"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_PREFIX_NAME = "name_for_"
        private const val KEY_PREFIX_PASSWORD = "password_for_"
    }

    // Store user signup data
    fun saveUserData(name: String, email: String, password: String): Boolean {
        return try {
            val editor = sharedPreferences.edit()
            editor.putString(KEY_PREFIX_NAME + email, name)
            editor.putString(KEY_PREFIX_PASSWORD + email, password)
            // Use commit() instead of apply() for immediate write
            val result = editor.commit()
            Log.d(TAG, "User saved: $email, Success: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data", e)
            false
        }
    }

    // Check if email exists (user is registered)
    fun isUserRegistered(email: String): Boolean {
        val exists = sharedPreferences.contains(KEY_PREFIX_PASSWORD + email)
        Log.d(TAG, "Is user registered check for $email: $exists")
        return exists
    }

    // Verify login credentials
    fun verifyLogin(email: String, password: String): Boolean {
        val savedPassword = sharedPreferences.getString(KEY_PREFIX_PASSWORD + email, null)
        val isValid = savedPassword != null && savedPassword == password
        Log.d(TAG, "Login verification for $email: $isValid")
        return isValid
    }

    // Set user as logged in
    fun setUserLoggedIn(email: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_USER_LOGGED_IN, true)
        editor.putString(KEY_CURRENT_USER_EMAIL, email)
        // Use commit() instead of apply() for critical authentication changes
        val result = editor.commit()
        Log.d(TAG, "User set as logged in: $email, Success: $result")
    }

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
        val email = getCurrentUserEmail()
        Log.d(TAG, "User logged in check: $isLoggedIn, Email: $email")
        return isLoggedIn
    }

    // Get current logged-in user's email
    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, null)
    }

    // Get current user's name
    fun getCurrentUserName(): String? {
        val email = getCurrentUserEmail() ?: return null
        return sharedPreferences.getString(KEY_PREFIX_NAME + email, null)
    }

    // Log out user
    fun logoutUser() {
        Log.d(TAG, "Logging out user: ${getCurrentUserEmail()}")
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_USER_LOGGED_IN, false)
        editor.remove(KEY_CURRENT_USER_EMAIL)
        // Use commit() for critical changes
        val result = editor.commit()
        Log.d(TAG, "User logged out, Success: $result")
    }

    // Debug method - print all stored data
    fun printAllData() {
        Log.d(TAG, "=== All SharedPreferences Data ===")
        Log.d(TAG, "Is logged in: ${sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)}")
        Log.d(TAG, "Current email: ${sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, "none")}")

        val allEntries = sharedPreferences.all
        for (entry in allEntries.entries) {
            // Don't log actual passwords in production
            if (!entry.key.startsWith(KEY_PREFIX_PASSWORD)) {
                Log.d(TAG, "${entry.key}: ${entry.value}")
            } else {
                Log.d(TAG, "${entry.key}: [PASSWORD HIDDEN]")
            }
        }
        Log.d(TAG, "=== End of SharedPreferences Data ===")
    }
}