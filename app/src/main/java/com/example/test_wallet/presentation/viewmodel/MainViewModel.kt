package com.example.test_wallet.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_wallet.data.repository.WalletManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val walletManager: WalletManager
) : ViewModel() {

    private val _balance = MutableStateFlow<Long?>(null)
    val balance: StateFlow<Long?> = _balance

    private val _transactionResult = MutableStateFlow<String?>(null)
    val transactionResult: StateFlow<String?> = _transactionResult

    val walletAddress = walletManager.address

    fun loadBalance() {
        viewModelScope.launch {
            runCatching {
                walletManager.getBalance()
            }.onSuccess {
                _balance.value = it
            }.onFailure {
                _balance.value = -1
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
}
