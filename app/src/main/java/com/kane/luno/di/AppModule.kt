package com.kane.luno.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.luno.core.data.datasource.ConversationRemoteDataSource
import com.luno.core.data.datasource.MediaRemoteDataSource
import com.luno.core.data.local.LunoDatabase
import com.luno.core.data.local.UserPreferencesDataSource
import com.luno.core.data.local.userDataStore
import com.luno.core.data.repository.AuthRepositoryImpl
import com.luno.core.data.repository.ConversationRepositoryImpl
import com.luno.core.data.repository.MediaRepositoryImpl
import com.luno.core.data.repository.PostRepositoryImpl
import com.luno.core.data.repository.UserRepositoryImpl
import com.luno.core.domain.repository.AuthRepository
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.MediaRepository
import com.luno.core.domain.repository.PostRepository
import com.luno.core.domain.repository.UserRepository
import com.luno.core.network.di.SupabaseModule

object AppModule {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var database: LunoDatabase

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        dataStore = appContext.userDataStore
        database = LunoDatabase.getInstance(appContext)
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

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(MediaRemoteDataSource(SupabaseModule.client))
    }

    val postRepository: PostRepository by lazy {
        PostRepositoryImpl(SupabaseModule.client)
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepositoryImpl(
            remoteDataSource = ConversationRemoteDataSource(SupabaseModule.client),
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao()
        )
    }
}
