package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private val splashDelay: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Navigate to OnboardingActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)

            // Use no animation for the transition
            overridePendingTransition(0, 0)

            finish()
        }, splashDelay)
    }
}