package com.yourname.plasticrex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.plasticrex.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    val currentIncome by viewModel.monthlyIncome.collectAsState()
    val currentThreshold by viewModel.safeThreshold.collectAsState()

    var incomeText by remember { mutableStateOf(if (currentIncome > 0) currentIncome.toInt().toString() else "") }
    var thresholdText by remember { mutableStateOf((currentThreshold * 100).toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = incomeText,
                onValueChange = { incomeText = it.filter { c -> c.isDigit() } },
                label = { Text("Monthly Income (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = thresholdText,
                onValueChange = { thresholdText = it.filter { c -> c.isDigit() } },
                label = { Text("Safe Threshold (%)") },
                supportingText = { Text("Example: 40 means 40% of income") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val income = incomeText.toDoubleOrNull() ?: 0.0
                    val threshold = thresholdText.toFloatOrNull() ?: 40f
                    viewModel.saveIncome(income)
                    viewModel.saveThreshold(threshold)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Text(
                text = "This threshold is used to warn you before taking a new loan.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}