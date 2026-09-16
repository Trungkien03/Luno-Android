package com.kane.luno

import android.app.Application
import com.kane.luno.di.AppModule

class LunoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.initialize(this)
    }
}
