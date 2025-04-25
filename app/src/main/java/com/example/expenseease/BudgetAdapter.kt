package com.example.expenseease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expenseease.Budget
import com.example.expenseease.BudgetManager
import com.example.expenseease.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class BudgetAdapter(
    private var budgets: List<Budget>,
    private val budgetManager: BudgetManager,
    private val onBudgetClickListener: (Budget) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBudgetCategory: TextView = view.findViewById(R.id.tvBudgetCategory)
        val tvBudgetAmount: TextView = view.findViewById(R.id.tvBudgetAmount)
        val tvBudgetPeriod: TextView = view.findViewById(R.id.tvBudgetPeriod)
        val tvBudgetUsage: TextView = view.findViewById(R.id.tvBudgetUsage)
        val tvBudgetRemaining: TextView = view.findViewById(R.id.tvBudgetRemaining)
        val progressBudget: LinearProgressIndicator = view.findViewById(R.id.progressBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val budget = budgets[position]
        val spent = budgetManager.calculateBudgetSpending(budget)
        val remaining = budgetManager.calculateBudgetRemaining(budget)
        val percentage = budgetManager.calculateBudgetUsagePercentage(budget)

        holder.tvBudgetCategory.text = budget.category
        holder.tvBudgetAmount.text = "$${String.format("%,.2f", budget.amount)}"

        // Format period nicely with capitalized first letter
        val formattedPeriod = budget.period.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        holder.tvBudgetPeriod.text = formattedPeriod

        holder.tvBudgetUsage.text = "$${String.format("%,.2f", spent)} used (${percentage}%)"

        holder.tvBudgetRemaining.text = "$${String.format("%,.2f", remaining)} left"

        // Set text color based on remaining amount
        holder.tvBudgetRemaining.setTextColor(
            holder.itemView.context.getColor(
                if (remaining < 0) R.color.expense_red else R.color.income_green
            )
        )

        // Update progress bar
        holder.progressBudget.setProgress(percentage, true)

        // Set progress bar color based on usage percentage
        val progressColor = when {
            percentage > 90 -> R.color.expense_red
            percentage > 75 -> R.color.warning_yellow
            else -> R.color.primary
        }
        holder.progressBudget.setIndicatorColor(holder.itemView.context.getColor(progressColor))

        // Set click listener
        holder.itemView.setOnClickListener {
            onBudgetClickListener(budget)
        }
    }

    override fun getItemCount() = budgets.size

    fun updateBudgets(newBudgets: List<Budget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }
}