package com.example.test_wallet

import android.app.Application
import com.example.test_wallet.data.di.networkModule
import com.example.test_wallet.di.viewModelModule
import com.example.test_wallet.di.walletModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                networkModule,
                walletModule,
                viewModelModule
            )
        }
    }
}

