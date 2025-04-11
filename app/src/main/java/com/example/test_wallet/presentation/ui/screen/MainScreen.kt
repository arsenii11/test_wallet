package com.example.test_wallet.presentation.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test_wallet.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    onTxClick: (String) -> Unit,
    onHistoryClick: () -> Unit = {}
) {
    val balance by viewModel.balance.collectAsState()
    val transactionResult by viewModel.transactionResult.collectAsState()

    val context = LocalContext.current
    var toAddress by remember { mutableStateOf("") }
    var amountToSend by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var txId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadBalance()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bitcoin Wallet",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Balance: ${balanceText(balance?.confirmed)}",
            style = MaterialTheme.typography.titleMedium
        )
        if ((balance?.pending ?: 0L) > 0) {
            Text(
                text = "Pending: +${balanceText(balance?.pending)}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        Text(
            text = "Your address:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = viewModel.walletAddress,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Wallet Address", viewModel.walletAddress)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                }
        )

        OutlinedTextField(
            value = amountToSend,
            onValueChange = { amountToSend = it },
            label = { Text("Amount to send (tBTC)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = toAddress,
            onValueChange = { toAddress = it },
            label = { Text("Recipient address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val satoshis = (amountToSend.toDoubleOrNull() ?: 0.0) * 100_000_000L
                viewModel.sendBitcoin(toAddress.trim(), satoshis.toLong())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        if (transactionResult?.startsWith("error:") == true) {
            Text(
                text = "Error: ${transactionResult?.removePrefix("error:")}",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (transactionResult?.startsWith("error:") == false && transactionResult != null) {
            txId = transactionResult!!
            showSuccessDialog = true
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = onHistoryClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("View transaction history")
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Your funds have been sent!") },
            text = {
                TextButton(onClick = {
                    onTxClick(txId)
                }) {
                    Text("Your transaction ID is ${txId.take(18)}...", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    viewModel.loadBalance()
                }) {
                    Text("Send more")
                }
            }
        )
    }
}

private fun balanceText(balance: Long?): String {
    return when {
        balance == null -> "Loading..."
        balance < 0 -> "Error"
        else -> "%.8f tBTC".format(balance / 100_000_000.0)
    }
}
