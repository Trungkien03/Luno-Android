package com.kane.luno.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.luno.core.data.repository.AuthRepositoryImpl
import com.luno.core.data.repository.UserRepositoryImpl
import com.luno.core.datastore.UserPreferencesDataSource
import com.luno.core.datastore.userDataStore
import com.luno.core.domain.repository.AuthRepository
import com.luno.core.network.di.SupabaseModule


object AppModule {
    private lateinit var dataStore: DataStore<Preferences>

    fun initialize(context: Context) {
        // Chỉ đọc applicationContext 1 lần lúc init
        dataStore = context.applicationContext.userDataStore
    }

    val userPreferencesDataSource: UserPreferencesDataSource by lazy {
        UserPreferencesDataSource(dataStore)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            userPreferencesDataSource,
            SupabaseModule.client,
            UserRepositoryImpl(SupabaseModule.client)
        )
    }
}
