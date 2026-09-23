package com.cos.plasticrex.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cos.plasticrex.data.AppDatabase
import com.cos.plasticrex.data.Loan
import com.cos.plasticrex.data.LoanRepository
import com.cos.plasticrex.data.UserPreferences
import com.cos.plasticrex.parser.SmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LoanRepository(AppDatabase.getDatabase(application).loanDao())
    private val prefs = UserPreferences(application)

    val loans = repo.allLoans
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val monthlyIncome = prefs.monthlyIncome
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

    val safeThreshold = prefs.safeThreshold
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.40f
        )

    // Keep only ACTIVE loans in calculations
    val activeLoans = loans
        .map { list -> list.filter { !it.isCompleted } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Total EMI from active loans only
    val totalEmi = activeLoans
        .map { list -> list.sumOf { it.emiAmount } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

    val currentRatio = combine(totalEmi, monthlyIncome) { emi, income ->
        if (income <= 0) 0.0 else emi / income
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.0
    )
    fun markAsCompleted(id: Long) {
        viewModelScope.launch { repo.markCompleted(id) }
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun addManualLoan(lender: String, amount: Double, dueDateMillis: Long) {
        viewModelScope.launch {
            repo.insert(
                Loan(
                    lender = lender,
                    emiAmount = amount,
                    dueDate = dueDateMillis,
                    isCompleted = false,
                    isManual = true
                )
            )
        }
    }

    fun refreshFromSms() {
        viewModelScope.launch {

            val messages = readSms(
                getApplication<Application>().contentResolver
            )

            println("PLASTIC_REX_SMS: Messages found = ${messages.size}")

            val parsed = messages.mapNotNull { (body, sender) ->

                println(
                    "PLASTIC_REX_SMS: Checking message from $sender:\n$body"
                )

                val result = SmsParser.parse(body, sender)

                if (result != null) {
                    println(
                        "PLASTIC_REX_SMS: EMI DETECTED -> " +
                                "Lender=${result.lender}, " +
                                "Amount=${result.emiAmount}"
                    )
                }

                result
            }

            println("PLASTIC_REX_SMS: Parsed EMI count = ${parsed.size}")

            val uniqueLoans = mutableListOf<Loan>()

            for (candidate in parsed) {
                val alreadyExists = uniqueLoans.any { existing ->
                    SmsParser.isSameLoan(existing, candidate)
                }

                if (!alreadyExists) {
                    uniqueLoans.add(candidate)
                }
            }

            repo.clearAll()

            uniqueLoans.forEach {
                repo.insert(it)
            }

            println(
                "PLASTIC_REX_SMS: Saved ${uniqueLoans.size} unique loans"
            )
        }
    }

    fun saveIncome(income: Double) {
        viewModelScope.launch { prefs.saveIncome(income) }
    }

    fun saveThreshold(thresholdPercent: Float) {
        viewModelScope.launch { prefs.saveThreshold(thresholdPercent / 100f) }
    }

    fun projectedRatio(newEmi: Double): Double {
        val income = monthlyIncome.value
        if (income <= 0) return 0.0
        return (totalEmi.value + newEmi) / income
    }

    private fun readSms(cr: ContentResolver): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        try {
            val uri = Telephony.Sms.Inbox.CONTENT_URI

            val projection = arrayOf(
                Telephony.Sms.BODY,
                Telephony.Sms.ADDRESS
            )

            val cursor = cr.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIdx) ?: continue
                    val sender = it.getString(addressIdx) ?: "Unknown"

                    list.add(body to sender)
                }
            }

        } catch (e: SecurityException) {
            println("PLASTIC_REX_SMS: READ_SMS permission denied: ${e.message}")
        } catch (e: Exception) {
            println("PLASTIC_REX_SMS: Error reading SMS: ${e.message}")
        }

        println("PLASTIC_REX_SMS: Total SMS read = ${list.size}")

        return list
    }
    // Maximum EMI allowed under the user's safe threshold
    val maxSafeEmi = combine(monthlyIncome, safeThreshold) { income, threshold ->
        income * threshold
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // How much more EMI the user can still take
    val remainingSafeEmi = combine(maxSafeEmi, totalEmi) { max, current ->
        max - current
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Risk level based on how close they are to the limit
    val riskLevel = combine(currentRatio, safeThreshold) { ratio, threshold ->
        when {
            threshold <= 0f -> "Unknown"
            ratio >= threshold -> "Critical"
            ratio >= threshold * 0.85f -> "High"
            ratio >= threshold * 0.60f -> "Medium"
            else -> "Low"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Low")
}