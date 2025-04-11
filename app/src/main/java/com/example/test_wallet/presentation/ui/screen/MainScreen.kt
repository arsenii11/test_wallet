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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    var toAddress by rememberSaveable  { mutableStateOf("") }
    var amountToSend by rememberSaveable { mutableStateOf("") }

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
                    val clipboard =
                        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Wallet Address", viewModel.walletAddress)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT)
                        .show()
                }
        )

        var amountTextField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(""))
        }

        OutlinedTextField(
            value = amountTextField,
            onValueChange = { newValue ->
                val raw = newValue.text.replace(",", ".")
                val originalCursor = newValue.selection.start

                var fixed = raw

                if (raw.startsWith("0") && !raw.startsWith("0.")) {
                    val digitsOnly = raw.trimStart('0')
                    val afterZero = if (digitsOnly.startsWith(".")) digitsOnly else ".$digitsOnly"
                    fixed = "0$afterZero"
                }

                val dotCount = fixed.count { it == '.' }
                val isValidFormat = fixed.matches(Regex("^\\d*\\.?\\d{0,8}$")) && dotCount <= 1

                if (isValidFormat) {
                    val cursorOffset = fixed.length - raw.length + originalCursor
                    amountTextField = TextFieldValue(
                        text = fixed,
                        selection = TextRange(cursorOffset.coerceIn(0, fixed.length))
                    )
                }
            },
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

        val isFormValid = toAddress.isNotBlank() &&
                amountTextField.text.toDoubleOrNull()?.let { it > 0 } == true

        Button(
            onClick = {
                val trimmedAddress = toAddress.replace("\\s".toRegex(), "")
                if (!isFormValid) {
                    Toast.makeText(context, "Enter a valid amount and address", Toast.LENGTH_SHORT).show()
                } else {
                    val satoshis = (amountTextField.text.toDoubleOrNull() ?: 0.0) * 100_000_000L
                    viewModel.sendBitcoin(trimmedAddress, satoshis.toLong())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            enabled = isFormValid
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
            modifier = Modifier.align(Alignment.End),
            shape = MaterialTheme.shapes.small
        ) {
            Text("View transaction history")
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text(
                    "Success!",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text("Your funds have been sent successfully.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TX ID: ${txId.take(18)}...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    viewModel.resetTransactionState()
                    viewModel.loadBalance()
                }) {
                    Text("Close")
                }

            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onTxClick(txId) // откроем в браузере
                }) {
                    Text("View TX")
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
