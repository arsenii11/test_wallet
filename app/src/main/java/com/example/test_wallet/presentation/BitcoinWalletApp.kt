package com.example.test_wallet.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.test_wallet.presentation.ui.screen.MainScreen

@Composable
fun BitcoinWalletApp() {
    val navController = rememberNavController()

    MaterialTheme {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onTxClick = { txId ->
                        // обработка клика на txId — опционально
                    },
                  /*  onHistoryClick = {
                        navController.navigate("history")
                    }*/
                )
            }
            composable("history") {
                //HistoryScreen()
            }
        }
    }
}
