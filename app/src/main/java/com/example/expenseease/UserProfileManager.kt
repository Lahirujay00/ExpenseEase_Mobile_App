package com.example.expenseease

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson

class UserProfileManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "user_profile_prefs"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_CURRENCY = "user_currency"
        private const val KEY_PROFILE_PICTURE = "user_profile_picture"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // User Name
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_NAME, "") ?: ""
    }

    fun setUserName(name: String) {
        sharedPreferences.edit().putString(KEY_NAME, name).apply()
    }

    // User Email
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_EMAIL, "") ?: ""
    }

    fun setUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }

    // Currency
    fun getUserCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
    }

    fun setUserCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    // Profile Picture
    fun getProfilePictureUri(): String? {
        return sharedPreferences.getString(KEY_PROFILE_PICTURE, null)
    }

    fun setProfilePictureUri(uri: String?) {
        sharedPreferences.edit().putString(KEY_PROFILE_PICTURE, uri).apply()
    }

    // Notifications
    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    // Get User's Currency Symbol
    fun getCurrencySymbol(): String {
        return when (getUserCurrency()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "INR" -> "₹"
            else -> "$"
        }
    }

    // Get all available currencies
    fun getAvailableCurrencies(): List<String> {
        return listOf(
            "USD - US Dollar",
            "EUR - Euro",
            "GBP - British Pound",
            "JPY - Japanese Yen",
            "INR - Indian Rupee",
            "CAD - Canadian Dollar",
            "AUD - Australian Dollar",
            "CNY - Chinese Yuan",
            "BRL - Brazilian Real"
        )
    }

    // Initialize default values if needed
    fun initializeDefaultProfileIfNeeded() {
        if (getUserName().isEmpty()) {
            setUserName("User")
            setUserEmail("")
            setUserCurrency("USD")
            setNotificationsEnabled(true)
        }
    }

    // Clear all user data
    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }
}