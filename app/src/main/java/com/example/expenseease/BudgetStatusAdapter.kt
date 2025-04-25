package com.example.expenseease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expenseease.Budget
import com.example.expenseease.BudgetManager
import com.example.expenseease.R
import java.util.Locale

class BudgetStatusAdapter(
    private var budgets: List<Budget>,
    private val budgetManager: BudgetManager
) : RecyclerView.Adapter<BudgetStatusAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBudgetCategory: TextView = view.findViewById(R.id.tvBudgetCategory)
        val tvBudgetStatus: TextView = view.findViewById(R.id.tvBudgetStatus)
        val progressBudget: ProgressBar = view.findViewById(R.id.progressBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_status, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val budget = budgets[position]
        val spent = budgetManager.calculateBudgetSpending(budget)
        val percentage = budgetManager.calculateBudgetUsagePercentage(budget)

        // Format category with period
        val formattedPeriod = budget.period.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        holder.tvBudgetCategory.text = "${budget.category} (${formattedPeriod})"

        // Format status text
        holder.tvBudgetStatus.text = "$${String.format("%,.2f", spent)} / $${String.format("%,.2f", budget.amount)}"

        // Set progress and color
        holder.progressBudget.progress = percentage

        // Set status text color based on usage
        val context = holder.itemView.context
        holder.tvBudgetStatus.setTextColor(
            when {
                percentage > 90 -> ContextCompat.getColor(context, R.color.expense_red)
                percentage > 75 -> ContextCompat.getColor(context, R.color.warning_yellow)
                else -> ContextCompat.getColor(context, R.color.primary)
            }
        )
    }

    override fun getItemCount() = budgets.size

    fun updateBudgets(newBudgets: List<Budget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }
}