package com.example.expenseease

import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val amount: Double,
    val period: String = "monthly", // monthly, weekly, yearly
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String? = null  // Added notes field
)