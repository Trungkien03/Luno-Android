package com.kane.luno.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.luno.core.data.datasource.ConversationRemoteDataSource
import com.luno.core.data.local.UserPreferencesDataSource
import com.luno.core.data.local.userDataStore
import com.luno.core.data.repository.AuthRepositoryImpl
import com.luno.core.data.repository.ConversationRepositoryImpl
import com.luno.core.data.repository.UserRepositoryImpl
import com.luno.core.domain.repository.AuthRepository
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.UserRepository
import com.luno.core.network.di.SupabaseModule

object AppModule {
    private lateinit var dataStore: DataStore<Preferences>

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        dataStore = appContext.userDataStore
        SupabaseModule.initialize(appContext)
    }

    val userPreferencesDataSource: UserPreferencesDataSource by lazy {
        UserPreferencesDataSource(dataStore)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(SupabaseModule.client)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            userPreferencesDataSource,
            SupabaseModule.client,
            userRepository
        )
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepositoryImpl(ConversationRemoteDataSource(SupabaseModule.client))
    }
}
