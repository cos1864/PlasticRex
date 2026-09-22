package com.yourname.plasticrex.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val INCOME_KEY = doublePreferencesKey("monthly_income")
        private val THRESHOLD_KEY = floatPreferencesKey("safe_threshold") // 0.40f = 40%
    }

    val monthlyIncome: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[INCOME_KEY] ?: 0.0
    }

    val safeThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[THRESHOLD_KEY] ?: 0.40f
    }

    suspend fun saveIncome(income: Double) {
        context.dataStore.edit { it[INCOME_KEY] = income }
    }

    suspend fun saveThreshold(threshold: Float) {
        context.dataStore.edit { it[THRESHOLD_KEY] = threshold }
    }
}