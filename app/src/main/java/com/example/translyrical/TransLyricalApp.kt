package com.example.translyrical

import android.app.Application
import com.example.translyrical.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TransLyricalApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TransLyricalApp)
            modules(appModule)
        }
    }
}