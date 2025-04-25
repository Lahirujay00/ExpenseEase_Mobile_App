package com.example.expenseease

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expenseease.adapter.BudgetStatusAdapter
import com.example.expenseease.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var transactionManager: TransactionManager
    private lateinit var budgetManager: BudgetManager
    private lateinit var budgetStatusAdapter: BudgetStatusAdapter

    // Date range
    private var startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    private var endDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        transactionManager = TransactionManager(this)
        budgetManager = BudgetManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup UI elements
        setupDateRangeSelectors()
        setupBudgetStatusRecyclerView()

        // Load data
        loadData()
    }

    private fun setupDateRangeSelectors() {
        binding.chipGroupDateRange.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipWeek -> setDateRangeTo("week")
                R.id.chipMonth -> setDateRangeTo("month")
                R.id.chipQuarter -> setDateRangeTo("quarter")
                R.id.chipYear -> setDateRangeTo("year")
                R.id.chipCustom -> showCustomDateRangeDialog()
            }
        }
    }

    private fun showCustomDateRangeDialog() {
        val startDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Start Date")
            .setSelection(startDate.timeInMillis)
            .build()

        val endDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select End Date")
            .setSelection(endDate.timeInMillis)
            .build()

        startDatePicker.addOnPositiveButtonClickListener { selection ->
            startDate.timeInMillis = selection
            endDatePicker.show(supportFragmentManager, "end_date_picker")
        }

        endDatePicker.addOnPositiveButtonClickListener { selection ->
            endDate.timeInMillis = selection
            loadData()
        }

        startDatePicker.show(supportFragmentManager, "start_date_picker")
    }

    private fun updateSpendingTrendsChart(transactions: List<Transaction>) {
        setupSpendingTrendChart(transactions)
    }

    private fun updateCategoryDistribution(transactions: List<Transaction>) {
        setupCategoryPieChart(transactions)
    }

    private fun updateIncomeVsExpenses(transactions: List<Transaction>) {
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        setupIncomeExpenseChart(income, expense)
    }

    private fun updateMonthlyComparison(transactions: List<Transaction>) {
        // This method is already handled by setupSpendingTrendChart
        setupSpendingTrendChart(transactions)
    }

    private fun setDateRangeTo(range: String) {
        val now = Calendar.getInstance()

        startDate = Calendar.getInstance()
        endDate = Calendar.getInstance()

        when (range) {
            "week" -> {
                // Set to current week
                startDate.set(Calendar.DAY_OF_WEEK, startDate.firstDayOfWeek)
                endDate.set(Calendar.DAY_OF_WEEK, endDate.firstDayOfWeek)
                endDate.add(Calendar.DAY_OF_WEEK, 6)
            }
            "month" -> {
                // Set to current month
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "quarter" -> {
                // Set to last 3 months
                startDate.add(Calendar.MONTH, -2)
                startDate.set(Calendar.DAY_OF_MONTH, 1)
            }
            "year" -> {
                // Set to current year
                startDate.set(Calendar.DAY_OF_YEAR, 1)
                endDate.set(Calendar.MONTH, 11)
                endDate.set(Calendar.DAY_OF_MONTH, 31)
            }
        }

        // Set time components
        startDate.set(Calendar.HOUR_OF_DAY, 0)
        startDate.set(Calendar.MINUTE, 0)
        startDate.set(Calendar.SECOND, 0)

        endDate.set(Calendar.HOUR_OF_DAY, 23)
        endDate.set(Calendar.MINUTE, 59)
        endDate.set(Calendar.SECOND, 59)

        // Hide custom date layout for preset ranges
        binding.layoutCustomDateRange.visibility = View.GONE

        // Update UI and load data
        updateCustomDateButtons()
        loadData()
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                if (isStartDate) {
                    startDate.set(year, month, dayOfMonth, 0, 0, 0)
                    if (startDate.after(endDate)) {
                        // If start date is after end date, set end date to start date
                        endDate.time = startDate.time
                        endDate.add(Calendar.DAY_OF_MONTH, 1)
                        endDate.add(Calendar.SECOND, -1)
                    }
                } else {
                    endDate.set(year, month, dayOfMonth, 23, 59, 59)
                    if (endDate.before(startDate)) {
                        // If end date is before start date, set start date to end date
                        startDate.time = endDate.time
                        startDate.add(Calendar.DAY_OF_MONTH, -1)
                        startDate.set(Calendar.HOUR_OF_DAY, 0)
                        startDate.set(Calendar.MINUTE, 0)
                        startDate.set(Calendar.SECOND, 0)
                    }
                }

                updateCustomDateButtons()
                loadData()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateCustomDateButtons() {
        // Update button texts with selected dates
        binding.btnStartDate.text = dateFormat.format(startDate.time)
        binding.btnEndDate.text = dateFormat.format(endDate.time)
    }

    private fun setupBudgetStatusRecyclerView() {
        budgetStatusAdapter = BudgetStatusAdapter(emptyList(), budgetManager)
        binding.rvBudgetStatus.layoutManager = LinearLayoutManager(this)
        binding.rvBudgetStatus.adapter = budgetStatusAdapter

        binding.btnAddFirstBudgetFromAnalytics.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }
    }

    private fun loadData() {
        // Get transactions within the selected date range
        val transactions = transactionManager.getAllTransactions().filter {
            it.timestamp in startDate.timeInMillis..endDate.timeInMillis
        }

        // Calculate totals
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        // Update summary texts
        binding.tvIncomeAmount.text = "$${String.format("%.2f", income)}"
        binding.tvExpenseAmount.text = "$${String.format("%.2f", expense)}"
        binding.tvBalanceAmount.text = "$${String.format("%.2f", balance)}"

        // Load charts
        setupIncomeExpenseChart(income, expense)
        setupCategoryPieChart(transactions)
        setupSpendingTrendChart(transactions)

        // Load budget status
        loadBudgetStatus()
    }

    private fun setupIncomeExpenseChart(income: Double, expense: Double) {
        val barChart = binding.barChartIncomeExpense

        // Create entries
        val entries = listOf(
            BarEntry(0f, income.toFloat()),
            BarEntry(1f, expense.toFloat())
        )

        val barDataSet = BarDataSet(entries, "Income vs Expense")
        barDataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.income_green),
            ContextCompat.getColor(this, R.color.expense_red)
        )
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.valueTextSize = 12f

        val barData = BarData(barDataSet)
        barChart.data = barData

        // Customize the chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawBarShadow(false)
        barChart.setScaleEnabled(false)

        // Format X-axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Income", "Expense"))

        // Format Y-axis
        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f

        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false

        // Animate
        barChart.animateY(1000)

        // Refresh
        barChart.invalidate()
    }

    private fun setupCategoryPieChart(transactions: List<Transaction>) {
        val pieChart = binding.pieChartCategories

        // Filter only expenses
        val expenses = transactions.filter { !it.isIncome }

        // Group by category
        val categoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
            .filter { it.value > 0 }

        // Create entries
        val entries = categoryMap.entries.mapIndexed { index, entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }

        if (entries.isEmpty()) {
            // No data
            pieChart.setNoDataText("No expense data for this period")
            pieChart.invalidate()
            return
        }

        val pieDataSet = PieDataSet(entries, "Categories")

        // Set colors
        val colors = listOf(
            Color.rgb(240, 130, 130), // Red
            Color.rgb(130, 240, 130), // Green
            Color.rgb(130, 130, 240), // Blue
            Color.rgb(240, 240, 130), // Yellow
            Color.rgb(240, 130, 240), // Purple
            Color.rgb(130, 240, 240), // Cyan
            Color.rgb(200, 150, 120), // Brown
            Color.rgb(150, 150, 150)  // Grey
        )
        pieDataSet.colors = colors

        // Style the data set
        pieDataSet.valueTextSize = 12f
        pieDataSet.valueTextColor = Color.BLACK
        pieDataSet.valueFormatter = PercentFormatter(pieChart)

        // Create and style the pie data
        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        // Customize the chart
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false)
        pieChart.holeRadius = 50f
        pieChart.transparentCircleRadius = 55f
        pieChart.setDrawCenterText(false)

        // Configure legend
        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        // Animate
        pieChart.animateY(1000, Easing.EaseInOutQuad)

        // Refresh
        pieChart.invalidate()
    }

    private fun setupSpendingTrendChart(transactions: List<Transaction>) {
        val lineChart = binding.lineChartTrend

        // Group expenses by day
        val expenses = transactions.filter { !it.isIncome }

        if (expenses.isEmpty()) {
            lineChart.setNoDataText("No expense data for this period")
            lineChart.invalidate()
            return
        }

        // Determine the time interval based on date range
        val timeSpan = endDate.timeInMillis - startDate.timeInMillis
        val dayCount = (timeSpan / (24 * 60 * 60 * 1000)).toInt()

        val groupByFormat: SimpleDateFormat
        val intervalType: Int
        val intervalCount: Int

        when {
            dayCount <= 14 -> {
                // Group by day for short ranges
                groupByFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                intervalType = Calendar.DAY_OF_MONTH
                intervalCount = 1
            }
            dayCount <= 60 -> {
                // Group by week for medium ranges
                groupByFormat = SimpleDateFormat("'Week' w", Locale.getDefault())
                intervalType = Calendar.WEEK_OF_YEAR
                intervalCount = 1
            }
            else -> {
                // Group by month for long ranges
                groupByFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                intervalType = Calendar.MONTH
                intervalCount = 1
            }
        }

        // Create list of all dates in range
        val allDates = mutableListOf<Date>()
        val calendar = startDate.clone() as Calendar
        while (!calendar.after(endDate)) {
            allDates.add(calendar.time)
            calendar.add(intervalType, intervalCount)
        }

        // Map expenses to dates
        val expensesByInterval = expenses.groupBy { transaction ->
            val date = Date(transaction.timestamp)
            val cal = Calendar.getInstance()
            cal.time = date

            // Reset to beginning of interval
            when (intervalType) {
                Calendar.DAY_OF_MONTH -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                Calendar.WEEK_OF_YEAR -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                Calendar.MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
            }
            cal.time
        }

        // Create entries for each interval
        val entries = mutableListOf<Entry>()
        val xLabels = mutableListOf<String>()

        calendar.time = startDate.time
        var i = 0f

        while (!calendar.after(endDate)) {
            val intervalDate = calendar.time
            val amountInInterval = expensesByInterval[intervalDate]
                ?.sumOf { it.amount }
                ?.toFloat() ?: 0f

            entries.add(Entry(i, amountInInterval))
            xLabels.add(groupByFormat.format(intervalDate))

            calendar.add(intervalType, intervalCount)
            i++
        }

        // Create data set
        val lineDataSet = LineDataSet(entries, "Expenses")
        lineDataSet.color = ContextCompat.getColor(this, R.color.primary)
        lineDataSet.lineWidth = 2f
        lineDataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary))
        lineDataSet.circleRadius = 4f
        lineDataSet.valueTextSize = 10f

        // Add data to chart
        val lineData = LineData(lineDataSet)
        lineChart.data = lineData

        // Customize chart
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Format X-axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAxis.labelCount = xLabels.size
        xAxis.labelRotationAngle = if (xLabels.size > 7) 45f else 0f

        // Format Y-axis
        val leftAxis = lineChart.axisLeft
        leftAxis.axisMinimum = 0f

        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        // Animate
        lineChart.animateX(1000)

        // Refresh
        lineChart.invalidate()
    }

    private fun loadBudgetStatus() {
        val budgets = budgetManager.getAllBudgets().sortedByDescending {
            val spent = budgetManager.getCategorySpentAmount(it.category, it.period)
            spent / it.amount  // Sort by percentage spent
        }

        if (budgets.isEmpty()) {
            binding.rvBudgetStatus.visibility = View.GONE
            binding.emptyBudgetStatus.visibility = View.VISIBLE
        } else {
            binding.rvBudgetStatus.visibility = View.VISIBLE
            binding.emptyBudgetStatus.visibility = View.GONE
            budgetStatusAdapter.updateBudgets(budgets)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}