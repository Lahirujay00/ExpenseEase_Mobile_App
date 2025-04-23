package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.expenseease.databinding.ActivitySignUpBinding
import com.example.expenseease.utils.SharedPreferenceManager
import com.google.android.material.snackbar.Snackbar

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize shared preference manager
        sharedPreferenceManager = SharedPreferenceManager(this)

        // Check if user is already logged in
        if (sharedPreferenceManager.isUserLoggedIn()) {
            navigateToHome()
            return
        }

        // Make sure the "Already have account" text is visible from the start
        ensureSignInOptionsVisible()

        // Adjust layout based on screen size
        adjustLayoutForScreenSize()

        setupUI()
        applyAnimations()
        setupListeners()
        setupTextWatchers()
        setupPasswordToggles()
    }

    // Adjust layout based on screen size
    private fun adjustLayoutForScreenSize() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels

        // If the screen is small, make adjustments to fit everything
        if (screenHeight < 1600) {
            // Reduce card padding
            val cardLayout = binding.clCardContent
            cardLayout.setPadding(cardLayout.paddingLeft, 16, cardLayout.paddingRight, 16)

            // Reduce spacing between elements
            val params = binding.tvEmailLabel.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = 8
            binding.tvEmailLabel.layoutParams = params

            val params2 = binding.tvPasswordLabel.layoutParams as ViewGroup.MarginLayoutParams
            params2.topMargin = 8
            binding.tvPasswordLabel.layoutParams = params2

            val params3 = binding.tvConfirmPasswordLabel.layoutParams as ViewGroup.MarginLayoutParams
            params3.topMargin = 8
            binding.tvConfirmPasswordLabel.layoutParams = params3

            // Reduce sign up button margin
            val btnParams = binding.btnSignUp.layoutParams as ViewGroup.MarginLayoutParams
            btnParams.topMargin = 12
            binding.btnSignUp.layoutParams = btnParams

            // Make sign-in text smaller on very small screens
            if (screenHeight < 1300) {
                binding.tvAlreadyHaveAccount.textSize = 12f
                binding.tvSignIn.textSize = 12f
                binding.tvSignIn.setPadding(4, 2, 4, 2)
            }
        }
    }

    // Helper method to ensure sign-in options remain visible
    private fun ensureSignInOptionsVisible() {
        binding.tvAlreadyHaveAccount.visibility = View.VISIBLE
        binding.tvAlreadyHaveAccount.alpha = 1.0f
        binding.tvSignIn.visibility = View.VISIBLE
        binding.tvSignIn.alpha = 1.0f
        binding.llSignInContainer.visibility = View.VISIBLE
        binding.llSignInContainer.bringToFront()
    }

    private fun setupUI() {
        // Set up immersive mode
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Set up custom password toggles to ensure proper icon switching
    private fun setupPasswordToggles() {
        // Password toggle
        binding.tilPassword.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(
                isPasswordVisible,
                binding.etPassword,
                binding.tilPassword
            )
        }

        // Confirm password toggle
        binding.tilConfirmPassword.setEndIconOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(
                isConfirmPasswordVisible,
                binding.etConfirmPassword,
                binding.tilConfirmPassword
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

    // Set up text watchers that clear errors on typing
    private fun setupTextWatchers() {
        // Clear full name error when typing starts
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilFullName.error = null
                ensureSignInOptionsVisible() // Ensure visibility
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear email error when typing starts
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
                ensureSignInOptionsVisible() // Ensure visibility
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear password error when typing starts
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                // Also clear confirm password error if they now match
                if (binding.etConfirmPassword.text.toString() == s.toString()) {
                    binding.tilConfirmPassword.error = null
                }
                ensureSignInOptionsVisible() // Ensure visibility
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear confirm password error when typing starts
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilConfirmPassword.error = null
                ensureSignInOptionsVisible() // Ensure visibility
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
        binding.tvSignUpTitle.startAnimation(slideDown)

        // Card animation
        val cardAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        cardAnim.startOffset = 300
        binding.cardSignUp.startAnimation(cardAnim)

        // Button animation
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_up)
        bounceAnim.startOffset = 600
        binding.btnSignUp.startAnimation(bounceAnim)

        // Text animations
        val fadeInSlow = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        fadeInSlow.startOffset = 800
        binding.llSignInContainer.startAnimation(fadeInSlow)

        // Label animations
        val labelAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim.startOffset = 400
        binding.tvFullNameLabel.startAnimation(labelAnim)

        val labelAnim2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim2.startOffset = 450
        binding.tvEmailLabel.startAnimation(labelAnim2)

        val labelAnim3 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim3.startOffset = 500
        binding.tvPasswordLabel.startAnimation(labelAnim3)

        val labelAnim4 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        labelAnim4.startOffset = 550
        binding.tvConfirmPasswordLabel.startAnimation(labelAnim4)
    }

    private fun setupListeners() {
        // Sign Up button click
        binding.btnSignUp.setOnClickListener {
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down_button)
            binding.btnSignUp.startAnimation(scaleDown)

            // Get input values
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // CRITICAL: Keep Sign-in options visible during validation
            ensureSignInOptionsVisible()

            // Validate inputs
            if (validateInputs(fullName, email, password, confirmPassword)) {
                // Attempt sign up
                attemptSignUp(fullName, email, password)
            } else {
                // Post a delayed check to ensure visibility after validation errors appear
                binding.root.post {
                    ensureSignInOptionsVisible()
                }

                // Second check with longer delay to handle any animations
                binding.root.postDelayed({
                    ensureSignInOptionsVisible()
                }, 100)
            }
        }

        // Sign In text click
        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        // Back button click
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(fullName: String, email: String, password: String, confirmPassword: String): Boolean {
        // Clear previous errors
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        // Full name validation
        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        }

        // Email validation
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        } else if (sharedPreferenceManager.isUserRegistered(email)) {
            binding.tilEmail.error = "Email is already registered"
            isValid = false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords don't match"
            isValid = false
        }

        // Ensure sign-in options remain visible after setting errors
        ensureSignInOptionsVisible()

        return isValid
    }

    private fun attemptSignUp(fullName: String, email: String, password: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignUp.isEnabled = false

        // Ensure sign-in options remain visible during loading
        ensureSignInOptionsVisible()

        // For demo purposes, simulate a network delay
        binding.root.postDelayed({
            // Save user to SharedPreferences
            val isSuccessful = sharedPreferenceManager.saveUserData(fullName, email, password)

            if (isSuccessful) {
                // Set user as logged in
                sharedPreferenceManager.setUserLoggedIn(email)

                // Hide loading state
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true

                // Successful sign up
                showSignUpSuccess()
            } else {
                // Hide loading state
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true

                // Show error
                Snackbar.make(binding.root, "Failed to create account. Please try again.", Snackbar.LENGTH_LONG).show()
            }
        }, 1000)
    }

    private fun showSignUpSuccess() {
        Snackbar.make(binding.root, "Account created successfully!", Snackbar.LENGTH_LONG).show()

        // Navigate to home screen after a short delay
        binding.root.postDelayed({
            navigateToHome()
        }, 1000)
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Make sure "Already have account" text is always visible
        ensureSignInOptionsVisible()
    }

    // Make sure view visibility is restored after any system events
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            ensureSignInOptionsVisible()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}