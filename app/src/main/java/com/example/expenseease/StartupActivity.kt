package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.expenseease.databinding.ActivityStartupBinding

class StartupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set full screen mode
        setupFullScreen()

        // Apply animations
        animateElements()

        // Setup click listeners
        setupListeners()
    }

    private fun setupFullScreen() {
        // Hide system bars
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun animateElements() {
        // Logo animation
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale_up)
        binding.ivAppLogo.startAnimation(logoAnimation)

        // Card animation with delay
        val cardAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.cardContainer.startAnimation(cardAnimation)

        // Title animation
        val titleAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        titleAnimation.startOffset = 300
        binding.tvWelcomeTitle.startAnimation(titleAnimation)

        // Message animation
        val messageAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_rightt)
        messageAnimation.startOffset = 500
        binding.tvWelcomeMessage.startAnimation(messageAnimation)

        // Buttons animation
        val signInBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_up)
        signInBtnAnimation.startOffset = 700
        binding.btnSignIn.startAnimation(signInBtnAnimation)

        val signUpBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_up)
        signUpBtnAnimation.startOffset = 900
        binding.btnSignUp.startAnimation(signUpBtnAnimation)

        // Wave animation
       // val waveAnimation = AnimationUtils.loadAnimation(this, R.anim.slow_wave)
        //binding.ivWaveBg.startAnimation(waveAnimation)
    }

    private fun setupListeners() {
        binding.btnSignIn.setOnClickListener {
            // Apply click animation
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down_button)
            binding.btnSignIn.startAnimation(scaleDown)

            // Navigate to sign in screen
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnSignUp.setOnClickListener {
            // Apply click animation
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down_button)
            binding.btnSignUp.startAnimation(scaleDown)

            // Navigate to sign up screen
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}