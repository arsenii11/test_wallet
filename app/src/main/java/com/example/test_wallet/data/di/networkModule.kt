package com.example.test_wallet.data.di

import com.example.test_wallet.data.api.EsploraApiService
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single {
        Retrofit.Builder()
            .baseUrl("https://mempool.space/signet/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EsploraApiService::class.java)
    }
}
