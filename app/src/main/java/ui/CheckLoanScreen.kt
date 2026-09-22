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
fun CheckLoanScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    var newEmiText by remember { mutableStateOf("") }
    val currentRatio by viewModel.currentRatio.collectAsState()
    val threshold by viewModel.safeThreshold.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()

    val newEmi = newEmiText.toDoubleOrNull() ?: 0.0
    val projected = viewModel.projectedRatio(newEmi)
    val isUnsafe = projected > threshold && income > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check New Loan") },
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
            Text(
                text = "Current ratio: ${(currentRatio * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = newEmiText,
                onValueChange = { newEmiText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Proposed EMI Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (newEmi > 0 && income > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnsafe)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (isUnsafe) "WARNING – Unsafe" else "Looks Safe",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Projected ratio: ${(projected * 100).toInt()}%")
                        Text("Your limit: ${(threshold * 100).toInt()}%")
                        if (isUnsafe) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This loan would push you over your safe limit.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (income <= 0) {
                Text(
                    "Please set your monthly income in Settings first.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}