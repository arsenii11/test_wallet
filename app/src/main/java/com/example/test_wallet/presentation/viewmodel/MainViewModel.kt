package com.example.test_wallet.presentation.viewmodel

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
            runCatching {
                val hex = walletManager.createTransaction(toAddress, amountSat)
                walletManager.broadcastTransaction(hex)
            }.onSuccess {
                _transactionResult.value = it
            }.onFailure {
                _transactionResult.value = "error:${it.message}"
            }
        }
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
