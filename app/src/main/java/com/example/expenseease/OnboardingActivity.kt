package com.example.expenseease

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var pageIndicator: TabLayout
    private lateinit var pagerAdapter: OnboardingPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences

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
        setContentView(R.layout.activity_onboarding)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("expense_ease_prefs", MODE_PRIVATE)

        // Check if onboarding has been completed before
        if (sharedPreferences.getBoolean("is_onboarding_completed", false)) {
            navigateToMainActivity()
            return
        }

        initViews()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        pageIndicator = findViewById(R.id.pageIndicator)
    }

    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(onboardingData)
        viewPager.adapter = pagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(pageIndicator, viewPager) { _, _ -> }.attach()

        // Update button text based on page position
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingData.size - 1) {
                    btnNext.text = getString(R.string.get_started)
                } else {
                    btnNext.text = getString(R.string.next)
                }

                // Show skip button only on first and second pages
                btnSkip.visibility = if (position < onboardingData.size - 1) View.VISIBLE else View.INVISIBLE
            }
        })
    }

    private fun setupListeners() {
        btnNext.setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition < onboardingData.size - 1) {
                // Go to next page
                viewPager.currentItem = currentPosition + 1
            } else {
                // Complete onboarding
                completeOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        // Mark onboarding as completed
        sharedPreferences.edit().putBoolean("is_onboarding_completed", true).apply()
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Data class for onboarding items
    data class OnboardingItem(
        val imageRes: Int,
        val title: String,
        val description: String
    )

    // ViewPager Adapter
    inner class OnboardingPagerAdapter(private val items: List<OnboardingItem>) :
        RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = layoutInflater.inflate(R.layout.onboarding_page, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val item = items[position]
            holder.imageView.setImageResource(item.imageRes)
            holder.titleView.text = item.title
            holder.descriptionView.text = item.description
        }

        override fun getItemCount() = items.size

        inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imgOnboarding)
            val titleView: TextView = itemView.findViewById(R.id.tvTitle)
            val descriptionView: TextView = itemView.findViewById(R.id.tvDescription)
        }
    }
}