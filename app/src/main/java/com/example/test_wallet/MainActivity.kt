package com.example.test_wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.test_wallet.presentation.ui.screen.MainScreen
import com.example.test_wallet.presentation.ui.theme.TestWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestWalletTheme {
                val context = LocalContext.current

                var txIdToOpen by remember { mutableStateOf<String?>(null) }

                MainScreen(
                    onTxClick = { txId ->
                        txIdToOpen = txId
                    }
                )

                txIdToOpen?.let { txId ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://mempool.space/signet/tx/$txId")
                    }
                    context.startActivity(intent)
                    txIdToOpen = null
                }
            }
        }
    }
}
