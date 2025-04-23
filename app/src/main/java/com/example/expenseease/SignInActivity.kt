package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.expenseease.databinding.ActivitySignInBinding
import com.example.expenseease.utils.SharedPreferenceManager
import com.google.android.material.snackbar.Snackbar

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private var isPasswordVisible = false
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences manager
        sharedPreferenceManager = SharedPreferenceManager(this)

        // Check if already logged in
        if (sharedPreferenceManager.isUserLoggedIn()) {
            navigateToHomeScreen()
            return
        }

        setupUI()
        applyAnimations()
        setupListeners()
        setupTextWatchers()
        setupPasswordToggle()
    }

    private fun setupUI() {
        // Set up immersive mode
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Adjust layout for smaller screens
        adjustLayoutForScreenSize()
    }

    private fun adjustLayoutForScreenSize() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels

        // If the screen is small, reduce text sizes
        if (screenHeight < 1600) {
            binding.tvNoAccount.textSize = 14f
            binding.tvSignUp.textSize = 14f
            binding.tvForgotPassword.textSize = 14f
        }
    }

    private fun setupPasswordToggle() {
        // Set up custom password toggle
        binding.tilPassword.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(
                isPasswordVisible,
                binding.etPassword,
                binding.tilPassword
            )
        }
    }

    // Helper function to toggle password visibility with proper icon switching
    private fun togglePasswordVisibility(
        isVisible: Boolean,
        editText: com.google.android.material.textfield.TextInputEditText,
        inputLayout: com.google.android.material.textfield.TextInputLayout
    ) {
        if (isVisible) {
            // Show password
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            inputLayout.setEndIconDrawable(R.drawable.ic_visibility)
        } else {
            // Hide password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            inputLayout.setEndIconDrawable(R.drawable.ic_visibility_off)
        }
        // Maintain cursor position
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun setupTextWatchers() {
        // Clear email error when typing starts
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear password error when typing starts
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyAnimations() {
        // Logo animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale_up)
        binding.ivAppLogo.startAnimation(fadeIn)

        // Title animation
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        binding.tvSignInTitle.startAnimation(slideDown)

        // Card animation
        val cardAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        cardAnim.startOffset = 300
        binding.cardSignIn.startAnimation(cardAnim)

        // Button animation
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_up)
        bounceAnim.startOffset = 600
        binding.btnSignIn.startAnimation(bounceAnim)

        // Text animations
        val fadeInSlow = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        fadeInSlow.startOffset = 800
        binding.tvForgotPassword.startAnimation(fadeInSlow)
        binding.btnBack.startAnimation(fadeInSlow)
        binding.tvNoAccount.startAnimation(fadeInSlow)
        binding.tvSignUp.startAnimation(fadeInSlow)

        // Label animations
        val labelAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim.startOffset = 400
        binding.tvEmailLabel.startAnimation(labelAnim)

        val labelAnim2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim2.startOffset = 500
        binding.tvPasswordLabel.startAnimation(labelAnim2)
    }

    private fun setupListeners() {
        // Sign In button click
        binding.btnSignIn.setOnClickListener {
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down_button)
            binding.btnSignIn.startAnimation(scaleDown)

            // Get input values
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validate inputs
            if (validateInputs(email, password)) {
                // Attempt sign in
                attemptSignIn(email, password)
            }
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            // Navigate to forgot password screen
            Snackbar.make(binding.root, "Forgot password feature coming soon", Snackbar.LENGTH_SHORT).show()
        }

        // Sign Up text click
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Back button click
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var isValid = true

        // Email validation
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun attemptSignIn(email: String, password: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.isEnabled = false

        // For demo purposes, simulate a network delay
        binding.root.postDelayed({
            // Hide loading state
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.isEnabled = true

            // Check if this is the demo account
            if (email == "demo@expenseease.com" && password == "password123") {
                // Successful sign in with demo account
                sharedPreferenceManager.setUserLoggedIn(email)
                navigateToHomeScreen()
                return@postDelayed
            }

            // Verify credentials using SharedPreferenceManager
            if (sharedPreferenceManager.verifyLogin(email, password)) {
                // Successful sign in
                sharedPreferenceManager.setUserLoggedIn(email)
                navigateToHomeScreen()
            } else {
                // Failed sign in
                Snackbar.make(binding.root, "Invalid email or password", Snackbar.LENGTH_SHORT).show()
            }
        }, 1500)
    }

    private fun navigateToHomeScreen() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}