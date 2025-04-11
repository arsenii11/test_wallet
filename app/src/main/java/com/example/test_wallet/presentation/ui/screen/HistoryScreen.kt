package com.example.test_wallet.presentation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.test_wallet.domain.model.TxDirection
import com.example.test_wallet.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp


@Composable
fun HistoryScreen(
    viewModel: MainViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val history by viewModel.history.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("Back to wallet")
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(history) { tx ->
                val icon = if (tx.direction == TxDirection.Sent) "➖" else "➕"
                val color = if (tx.direction == TxDirection.Sent) Color.Red else Color.Green
                val formatted = Instant.ofEpochSecond(tx.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("$icon ${tx.direction} ${formatSatsToBTC(tx.amountSat)} tBTC", color = color)
                    Text("ID: ${tx.txid.take(10)}...", fontSize = 12.sp)
                    Text("Time: $formatted", fontSize = 12.sp)
                    Divider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}


fun formatSatsToBTC(sats: Long): String {
    val btc = sats.toDouble() / 100_000_000
    return String.format("%.8f", btc).trimEnd('0').trimEnd('.')
}
