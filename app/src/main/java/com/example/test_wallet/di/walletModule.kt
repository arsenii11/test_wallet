package com.example.test_wallet.di

import com.example.test_wallet.data.repository.WalletManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val walletModule = module {
    single { WalletManager(get(), androidContext()) }
}
