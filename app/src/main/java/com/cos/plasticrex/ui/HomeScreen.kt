package com.cos.plasticrex.ui

import android.Manifest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cos.plasticrex.data.Loan
import com.cos.plasticrex.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToCheckLoan: () -> Unit,
    onNavigateToAddLoan: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    val currentRatio by viewModel.currentRatio.collectAsState()
    val threshold by viewModel.safeThreshold.collectAsState()

    val income = viewModel.monthlyIncome
        .collectAsState(initial = 0.0)
        .value

    val totalEmi = viewModel.totalEmi
        .collectAsState(initial = 0.0)
        .value

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshFromSms()
        }
    }

    val ratioColor = when {
        currentRatio > threshold -> MaterialTheme.colorScheme.error
        currentRatio > threshold * 0.8f -> Color(0xFFFFA000)
        else -> Color(0xFF2E7D32)
    }

    val riskLevel = when {
        income <= 0 -> "Unknown"
        currentRatio > threshold -> "High Risk"
        currentRatio > threshold * 0.8f -> "Moderate Risk"
        else -> "Low Risk"
    }

    val riskBackground = when {
        income <= 0 -> MaterialTheme.colorScheme.surfaceVariant
        currentRatio > threshold -> MaterialTheme.colorScheme.errorContainer
        currentRatio > threshold * 0.8f -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val remainingSafeEmi = (income * threshold) - totalEmi

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plastic Rex") },
                actions = {
                    TextButton(onClick = onNavigateToSettings) { Text("Settings") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "All data stays on your phone. No internet required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))


// ---------- Risk + Capacity Card ----------
            // ===== Progress + Risk Card =====
            val progress = if (threshold > 0f && income > 0) {
                (currentRatio / threshold).toFloat().coerceIn(0f, 1.5f)
            } else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = riskBackground)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Risk Level: $riskLevel", style = MaterialTheme.typography.titleMedium, color = ratioColor)
                    Spacer(Modifier.height(4.dp))
                    Text("Current ratio: ${(currentRatio * 100).toInt()}%  •  Limit: ${(threshold * 100).toInt()}%")

                    Spacer(Modifier.height(10.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = ratioColor,
                        trackColor = Color.LightGray.copy(alpha = 0.4f),
                    )

                    Spacer(Modifier.height(8.dp))

                    if (income <= 0) {
                        Text("Set your monthly income in Settings to see capacity.", color = MaterialTheme.colorScheme.error)
                    } else if (remainingSafeEmi > 0) {
                        Text("You can still take up to ₹${"%.0f".format(remainingSafeEmi)} more in EMI.")
                    } else {
                        Text(
                            "You are already ₹${"%.0f".format(-remainingSafeEmi)} over your safe limit.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

// ===== Buttons =====
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                viewModel.refreshFromSms()
                            }

                            else -> {
                                permissionLauncher.launch(
                                    Manifest.permission.READ_SMS
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh SMS")
                }

                Button(
                    onClick = onNavigateToAddLoan,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add EMI")
                }
            }

            Spacer(Modifier.height(16.dp))

// ===== Active EMIs =====
            Text("Active EMIs", style = MaterialTheme.typography.titleMedium)
            val active = loans.filter { !it.isCompleted }
            if (active.isEmpty()) {
                Text("No active EMIs", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(active, key = { it.id }) { loan ->
                        LoanItem(
                            loan = loan,
                            onMarkCompleted = { viewModel.markAsCompleted(loan.id) },
                            onDelete = { viewModel.deleteLoan(loan.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

// ===== Completed EMIs =====
            Text("Completed EMIs", style = MaterialTheme.typography.titleMedium)
            val completed = loans.filter { it.isCompleted }
            if (completed.isEmpty()) {
                Text("No completed EMIs yet", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(completed, key = { it.id }) { loan ->
                        LoanItem(
                            loan = loan,
                            isCompleted = true,
                            onDelete = { viewModel.deleteLoan(loan.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onNavigateToCheckLoan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check New Loan Safety")
            }

            Spacer(Modifier.height(16.dp))

            Text("Upcoming EMIs (${loans.size})", style = MaterialTheme.typography.titleMedium)

            if (loans.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No EMIs found yet.\nGrant SMS permission and tap Refresh.")
                }
            } else {
                LazyColumn {
                    items(loans) { loan ->
                        LoanItem(loan)
                    }
                }
            }
        }
    }
}
@Composable
fun LoanItem(
    loan: Loan,
    isCompleted: Boolean = false,
    onMarkCompleted: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(loan.lender, style = MaterialTheme.typography.titleSmall)
                Text("₹${"%.0f".format(loan.emiAmount)}")
                Text(
                    "Due: ${dateFormat.format(Date(loan.dueDate))}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (loan.isManual) {
                    Text("Manual entry", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!isCompleted && onMarkCompleted != null) {
                TextButton(onClick = onMarkCompleted) {
                    Text("Done")
                }
            }

            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var lender by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add EMI Manually") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = lender,
                onValueChange = { lender = it },
                label = { Text("Lender name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("EMI Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Due date (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = day, onValueChange = { day = it.filter { c -> c.isDigit() } },
                    label = { Text("DD") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = month, onValueChange = { month = it.filter { c -> c.isDigit() } },
                    label = { Text("MM") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = year, onValueChange = { year = it.filter { c -> c.isDigit() } },
                    label = { Text("YYYY") }, modifier = Modifier.weight(1.4f))
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (lender.isBlank() || amt <= 0) return@Button

                    val cal = Calendar.getInstance()
                    val d = day.toIntOrNull()
                    val m = month.toIntOrNull()
                    val y = year.toIntOrNull()
                    if (d != null && m != null && y != null) {
                        cal.set(y, m - 1, d)
                    }

                    viewModel.addManualLoan(lender.trim(), amt, cal.timeInMillis)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save EMI")
            }
        }
    }
}
