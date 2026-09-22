package com.yourname.plasticrex.ui

import android.Manifest
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
import com.yourname.plasticrex.data.Loan
import com.yourname.plasticrex.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToCheckLoan: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    val currentRatio by viewModel.currentRatio.collectAsState()
    val threshold by viewModel.safeThreshold.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.refreshFromSms()
    }

    val ratioColor = when {
        currentRatio > threshold -> MaterialTheme.colorScheme.error
        currentRatio > threshold * 0.8f -> Color(0xFFFFA000)
        else -> Color(0xFF2E7D32)
    }

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

            // Ratio card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Repayment-to-Income Ratio", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${(currentRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ratioColor
                    )
                    Text("Your safe limit: ${(threshold * 100).toInt()}%")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                                == PackageManager.PERMISSION_GRANTED -> viewModel.refreshFromSms()
                        else -> permissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh from SMS")
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
fun LoanItem(loan: Loan) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(loan.lender, style = MaterialTheme.typography.titleSmall)
            Text("₹${"%.0f".format(loan.emiAmount)}")
            Text("Due: ${dateFormat.format(Date(loan.dueDate))}", style = MaterialTheme.typography.bodySmall)
        }
    }
}