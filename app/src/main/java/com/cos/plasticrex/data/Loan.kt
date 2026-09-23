package com.cos.plasticrex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lender: String,
    val emiAmount: Double,
    val dueDate: Long,          // epoch millis
    val remainingMonths: Int? = null,
    val rawMessage: String = "",
    val isCompleted: Boolean = false,
    val isManual: Boolean = false
)
