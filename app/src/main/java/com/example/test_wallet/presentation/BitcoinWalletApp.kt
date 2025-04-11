package com.example.test_wallet.presentation

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.test_wallet.presentation.ui.screen.HistoryScreen
import com.example.test_wallet.presentation.ui.screen.MainScreen

@Composable
fun BitcoinWalletApp(onTxClick: (String) -> Unit) {
    val navController = rememberNavController()

    MaterialTheme {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onTxClick = onTxClick,
                    onHistoryClick = {
                        navController.navigate("history")
                    }
                )
            }
            composable("history") {
                HistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
