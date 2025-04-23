package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.expenseease.utils.SharedPreferenceManager
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private val splashDelay: Long = 3000 // 3 seconds
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferenceManager to check login status
        sharedPreferenceManager = SharedPreferenceManager(this)

        // Debug: Print login status and user information
        Log.d(TAG, "Is user logged in: ${sharedPreferenceManager.isUserLoggedIn()}")
        Log.d(TAG, "Current user email: ${sharedPreferenceManager.getCurrentUserEmail() ?: "none"}")

        // Set immersive mode for full screen splash
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        // Find views
        val cardView = findViewById<CardView>(R.id.cardView)
        val logoImageView = findViewById<ImageView>(R.id.iv_logo)
        val appNameTextView = findViewById<TextView>(R.id.tv_app_name)
        val taglineTextView = findViewById<TextView>(R.id.tv_tagline)
        val progressIndicator = findViewById<LinearProgressIndicator>(R.id.progress_loading)
        val divider = findViewById<View>(R.id.view_divider)

        // Make sure all views are initially visible
        logoImageView.visibility = View.VISIBLE
        appNameTextView.visibility = View.VISIBLE
        taglineTextView.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE

        // Initially hide only the progress indicator
        progressIndicator.visibility = View.INVISIBLE

        // Load animations from resources (using the existing ones)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)

        // Apply animations (keeping all the original animations)
        cardView.startAnimation(fadeIn)
        logoImageView.startAnimation(zoomIn)
        appNameTextView.startAnimation(slideUp)
        divider.startAnimation(slideInRight)
        taglineTextView.startAnimation(fadeIn)

        // Add separate progress animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Show progress indicator with a smooth fade in
            progressIndicator.alpha = 0f
            progressIndicator.visibility = View.VISIBLE
            progressIndicator.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }, 1000)

        // Navigate after delay - check login status first
        Handler(Looper.getMainLooper()).postDelayed({
            // IMPORTANT: Check if user is logged in
            if (sharedPreferenceManager.isUserLoggedIn()) {
                // User is logged in, navigate directly to HomeActivity
                Log.d(TAG, "User is logged in, navigating to HomeActivity")
                navigateToHomeScreen()
            } else {
                // User is not logged in, continue with normal onboarding flow
                Log.d(TAG, "User is not logged in, navigating to OnboardingActivity")
                navigateToOnboardingScreen()
            }
        }, splashDelay)
    }

    private fun navigateToHomeScreen() {
        val intent = Intent(this, HomeActivity::class.java)
        // Clear back stack to prevent going back to splash or onboarding
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        // Use no animation for the transition
        overridePendingTransition(0, 0)
        finish()
    }

    private fun navigateToOnboardingScreen() {
        val intent = Intent(this, OnboardingActivity::class.java)
        // Add this flag to indicate coming from the splash screen
        intent.putExtra("FROM_SPLASH", true)

        // Change these flags - they might be causing the blank screen
        // Use a simpler flag configuration
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)

        // Make sure this finishes AFTER starting the next activity
        finish()

        // Use a different animation set that won't cause blanking
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}