package com.example.expenseease

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expenseease.R
import com.example.expenseease.Transaction
import com.example.expenseease.TransactionDetailsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onTransactionClickListener: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTransactionTitle)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvTitle.text = transaction.title

        // Format amount with color based on transaction type
        val amountText = "$${String.format("%.2f", transaction.amount)}"
        holder.tvAmount.text = amountText
        holder.tvAmount.setTextColor(
            holder.itemView.context.getColor(
                if (transaction.isIncome) R.color.income_green else R.color.expense_red
            )
        )

        holder.tvCategory.text = transaction.category
        
        // Set category icon based on category
        val (iconResId, iconTint) = getCategoryIconAndTint(transaction.category, transaction.isIncome)
        holder.ivCategoryIcon.setImageResource(iconResId)
        holder.ivCategoryIcon.setColorFilter(
            ContextCompat.getColor(holder.itemView.context, iconTint),
            android.graphics.PorterDuff.Mode.SRC_IN
        )

        // Format date
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(transaction.timestamp))

        // Set click listener
        holder.itemView.setOnClickListener {
            onTransactionClickListener(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    private fun getCategoryIconAndTint(category: String, isIncome: Boolean): Pair<Int, Int> {
        return when {
            isIncome -> {
                when (category.lowercase()) {
                    "salary" -> Pair(R.drawable.ic_salary, R.color.income_green)
                    "investments" -> Pair(R.drawable.ic_investment, R.color.income_green)
                    "gifts" -> Pair(R.drawable.ic_gift, R.color.income_green)
                    "refunds" -> Pair(R.drawable.ic_refund, R.color.income_green)
                    else -> Pair(R.drawable.ic_income, R.color.income_green)
                }
            }
            else -> {
                when (category.lowercase()) {
                    "food" -> Pair(R.drawable.ic_food, R.color.expense_red)
                    "transport" -> Pair(R.drawable.ic_transport, R.color.expense_red)
                    "shopping" -> Pair(R.drawable.ic_shopping, R.color.expense_red)
                    "bills" -> Pair(R.drawable.ic_bills, R.color.expense_red)
                    "entertainment" -> Pair(R.drawable.ic_entertainment, R.color.expense_red)
                    "health" -> Pair(R.drawable.ic_health, R.color.expense_red)
                    "education" -> Pair(R.drawable.ic_education, R.color.expense_red)
                    else -> Pair(R.drawable.ic_expense, R.color.expense_red)
                }
            }
        }
    }
}