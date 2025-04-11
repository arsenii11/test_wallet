package com.example.test_wallet.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_wallet.data.repository.WalletManager
import com.example.test_wallet.domain.model.HistoryItem
import com.example.test_wallet.domain.model.WalletBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val walletManager: WalletManager
) : ViewModel() {

    private val pollingJob = Job()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(pollingJob + Dispatchers.IO) {
            while (true) {
                delay(10_000)
                loadBalance()
                loadHistory()
            }
        }
    }


    private val _balance = MutableStateFlow<WalletBalance?>(null)
    val balance: StateFlow<WalletBalance?> = _balance

    private val _transactionResult = MutableStateFlow<String?>(null)
    val transactionResult: StateFlow<String?> = _transactionResult

    val walletAddress = walletManager.address

    fun loadBalance() {
        viewModelScope.launch {
            runCatching {
                walletManager.getBalanceFull()
            }.onSuccess {
                _balance.value = it
            }.onFailure {
                _balance.value = WalletBalance(-1, 0)
            }
        }
    }

    fun sendBitcoin(toAddress: String, amountSat: Long) {
        viewModelScope.launch {
            val supported = toAddress.startsWith("m") ||
                    toAddress.startsWith("n") ||
                    toAddress.startsWith("tb1q")

            if (!supported) {
                _transactionResult.value = "error:Unsupported address format (use tb1q... or m...)"
                return@launch
            }

            Log.d("SendTx", "⏳ Creating transaction to $toAddress for $amountSat satoshis")

            runCatching {
                val hex = walletManager.createTransaction(toAddress, amountSat)
                Log.d("SendTx", "✅ TX Hex: $hex")
                walletManager.broadcastTransaction(hex)
            }.onSuccess {
                Log.d("SendTx", "🚀 Broadcast success! TX ID: $it")
                _transactionResult.value = it
            }.onFailure { e ->
                Log.e("SendTx", "❌ Error: ${e.message}", e)
                _transactionResult.value = "error:${e.message}"
            }
        }
    }

    fun resetTransactionState() {
        _transactionResult.value = null
    }


    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history

    fun loadHistory() {
        viewModelScope.launch {
            runCatching {
                walletManager.getTransactionHistory()
            }.onSuccess {
                _history.value = it
            }
        }
    }
}
