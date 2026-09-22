package com.kane.luno

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kane.luno.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LunoApplication : Application(), DefaultLifecycleObserver {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super<Application>.onCreate()
        AppModule.initialize(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        applicationScope.launch {
            AppModule.conversationRepository.updateOnlineStatus(true)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch {
            AppModule.conversationRepository.updateOnlineStatus(false)
        }
    }
}
