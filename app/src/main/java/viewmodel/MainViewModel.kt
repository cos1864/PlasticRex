package com.yourname.plasticrex.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.plasticrex.data.AppDatabase
import com.yourname.plasticrex.data.Loan
import com.yourname.plasticrex.data.LoanRepository
import com.yourname.plasticrex.data.UserPreferences
import com.yourname.plasticrex.parser.SmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LoanRepository(AppDatabase.getDatabase(application).loanDao())
    private val prefs = UserPreferences(application)

    val loans = repo.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome = prefs.monthlyIncome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val safeThreshold = prefs.safeThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.40f)

    val totalEmi = loans.map { list -> list.sumOf { it.emiAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentRatio = combine(totalEmi, monthlyIncome) { emi, income ->
        if (income <= 0) 0.0 else emi / income
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refreshFromSms() {
        viewModelScope.launch {
            val messages = readSms(getApplication<Application>().contentResolver)
            repo.clearAll()
            messages.forEach { (body, sender) ->
                SmsParser.parse(body, sender)?.let { repo.insert(it) }
            }
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
        val uri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = cr.query(
            uri,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS),
            null, null, "date DESC LIMIT 400"
        )
        cursor?.use {
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val sender = it.getString(addrIdx) ?: "Unknown"
                list.add(body to sender)
            }
        }
        return list
    }
}