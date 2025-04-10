package com.example.test_wallet.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.test_wallet.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel(), onTxClick: (String) -> Unit) {
    val balance by viewModel.balance.collectAsState()
    val transactionResult by viewModel.transactionResult.collectAsState()

    var toAddress by remember { mutableStateOf("") }
    var amountToSend by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var txId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadBalance()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {

        Text(text = "Bitcoin Wallet", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Balance: ${balanceText(balance)}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Your address: ${viewModel.walletAddress}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

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
            label = { Text("Address to send") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val satoshis = (amountToSend.toDoubleOrNull() ?: 0.0) * 100_000_000L
                viewModel.sendBitcoin(toAddress, satoshis.toLong())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        if (transactionResult?.startsWith("error:") == true) {
            Text(
                text = "Error: ${transactionResult?.removePrefix("error:")}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (transactionResult?.startsWith("error:") == false && transactionResult != null) {
            txId = transactionResult!!
            showSuccessDialog = true
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
                    Text("Your transaction ID is $txId")
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
