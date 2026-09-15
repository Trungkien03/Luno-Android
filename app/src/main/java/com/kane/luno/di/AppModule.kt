package com.kane.luno.di

import com.luno.core.data.repository.AuthRepositoryImpl
import com.luno.core.domain.repository.AuthRepository

object AppModule {
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }
}
