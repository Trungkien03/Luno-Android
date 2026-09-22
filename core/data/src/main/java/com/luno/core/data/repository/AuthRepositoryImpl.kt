package com.luno.core.data.repository

import com.luno.core.data.local.UserPreferencesDataSource
import com.luno.core.domain.repository.AuthRepository
import com.luno.core.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AuthRepositoryImpl(
    private val userPreferencesDataSource: UserPreferencesDataSource? = null,
    private val supabaseClient: SupabaseClient,
    private val userRepository: UserRepository = UserRepositoryImpl(supabaseClient)
) : AuthRepository {
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            refreshUserInfo()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val nowStr = dateFormat.format(Date())
                    supabaseClient.postgrest["users"].update(
                        JsonObject(
                            mapOf(
                                "is_online" to JsonPrimitive(false),
                                "last_seen_at" to JsonPrimitive(nowStr)
                            )
                        )
                    ) {
                        filter { eq("id", currentUserId) }
                    }
                } catch (_: Exception) {
                }
            }
            supabaseClient.auth.signOut()
            userPreferencesDataSource?.clearUserInfo()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshUserInfo(): Result<Unit> {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                val email = user.email ?: ""
                val id = user.id
                val authImg = user.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
                    ?: user.userMetadata?.get("picture")?.jsonPrimitive?.content
                    ?: ""
                val dbUser = userRepository.findUserById(id).getOrNull()
                val finalImg = dbUser?.avatarUrl?.takeIf { it.isNotBlank() } ?: authImg

                userPreferencesDataSource?.saveUserInfo(email, id, finalImg)

                // Update online status in Supabase users table
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val nowStr = dateFormat.format(Date())
                    supabaseClient.postgrest["users"].update(
                        JsonObject(
                            mapOf(
                                "is_online" to JsonPrimitive(true),
                                "last_seen_at" to JsonPrimitive(nowStr)
                            )
                        )
                    ) {
                        filter { eq("id", id) }
                    }
                } catch (_: Exception) {
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun currentUserEmail(): String? {
        return supabaseClient.auth.currentUserOrNull()?.email
    }

    override fun currentUserImg(): String? {
        val user = supabaseClient.auth.currentUserOrNull()
        return user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
            ?: user?.userMetadata?.get("picture")?.jsonPrimitive?.content
    }

    override val userEmailFlow: Flow<String?> = userPreferencesDataSource?.userEmail ?: flowOf(null)
    override val userImgFlow: Flow<String?> = userPreferencesDataSource?.userImg ?: flowOf(null)
}
