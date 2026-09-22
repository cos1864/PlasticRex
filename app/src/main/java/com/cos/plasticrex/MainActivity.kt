package com.cos.plasticrex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cos.plasticrex.ui.CheckLoanScreen
import com.cos.plasticrex.ui.HomeScreen
import com.cos.plasticrex.ui.SettingsScreen
import com.cos.plasticrex.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf("home") }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentScreen = "settings" },
                        onNavigateToCheckLoan = { currentScreen = "check" }
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                    "check" -> CheckLoanScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}
