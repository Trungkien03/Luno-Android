package com.luno.core.network.api

import retrofit2.http.GET
import retrofit2.http.POST

interface LunoApiService {

    // Placeholder for future endpoints
    @GET("v1/users/me")
    suspend fun getCurrentUser(): Any // Replace 'Any' with actual Response Model

    @POST("v1/auth/google")
    suspend fun verifyGoogleToken(): Any
}