package com.example.expenseease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.expenseease.databinding.ActivityOnboardingBinding
import com.example.expenseease.databinding.OnboardingPageBinding
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pagerAdapter: OnboardingPagerAdapter

    // Define onboarding content
    private val onboardingData = listOf(
        OnboardingItem(
            R.drawable.onboarding_1,
            "Welcome to ExpenseEase",
            "Take control of your finances with our simple and intuitive expense tracking app."
        ),
        OnboardingItem(
            R.drawable.onboarding_2,
            "Track Your Expenses",
            "Easily record your income and expenses by category and keep your financial life organized."
        ),
        OnboardingItem(
            R.drawable.onboarding_3,
            "Set Budget Goals",
            "Create monthly budgets and get notified when you're close to your spending limits."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if coming from splash screen
        val fromSplash = intent.getBooleanExtra("FROM_SPLASH", false)

        // Set up UI before animations
        setupViewPager()
        setupListeners()

        // Apply animations only when not coming from splash
        if (!fromSplash) {
            applyEntranceAnimations()
        }
    }
    private fun applyEntranceAnimations() {
        // Apply any entrance animations here
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.viewPager.startAnimation(fadeIn)
        binding.pageIndicator.startAnimation(fadeIn)
        binding.btnNext.startAnimation(fadeIn)
        binding.btnSkip.startAnimation(fadeIn)
    }


    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(onboardingData)
        binding.viewPager.adapter = pagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.pageIndicator, binding.viewPager) { _, _ -> }.attach()

        // Update button text based on page position
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingData.size - 1) {
                    binding.btnNext.text = getString(R.string.get_started)
                } else {
                    binding.btnNext.text = getString(R.string.next)
                }

                // Show skip button only on first and second pages
                binding.btnSkip.visibility = if (position < onboardingData.size - 1) View.VISIBLE else View.INVISIBLE
            }
        })
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition < onboardingData.size - 1) {
                // Go to next page
                binding.viewPager.currentItem = currentPosition + 1
            } else {
                // Complete onboarding
                completeOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        // Simply navigate to the startup activity without using SharedPreferences
        navigateToStartupActivity()
    }

    private fun navigateToStartupActivity() {
        val intent = Intent(this, StartupActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Data class for onboarding items
    data class OnboardingItem(
        val imageRes: Int,
        val title: String,
        val description: String
    )

    // ViewPager Adapter (now uses ViewBinding)
    inner class OnboardingPagerAdapter(private val items: List<OnboardingItem>) :
        RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val binding = OnboardingPageBinding.inflate(layoutInflater, parent, false)
            return OnboardingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val item = items[position]
            holder.binding.imgOnboarding.setImageResource(item.imageRes)
            holder.binding.tvTitle.text = item.title
            holder.binding.tvDescription.text = item.description
        }

        override fun getItemCount() = items.size

        inner class OnboardingViewHolder(val binding: OnboardingPageBinding) : RecyclerView.ViewHolder(binding.root)
    }
}