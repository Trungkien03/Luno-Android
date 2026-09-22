package com.luno.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataSource(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")

        private val USER_IMG = stringPreferencesKey("user_img")
    }

    val userEmail: Flow<String?> = dataStore.data
        .map { preferences -> preferences[USER_EMAIL_KEY] }

    val userId: Flow<String?> = dataStore.data
        .map { preferences -> preferences[USER_ID_KEY] }

    val userImg: Flow<String?> = dataStore.data.map { preferences -> preferences[USER_IMG] }

    suspend fun saveUserInfo(email: String, userId: String, userImg: String) {
        dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ID_KEY] = userId
            preferences[USER_IMG] = userImg
        }
    }

    suspend fun clearUserInfo() {
        dataStore.edit { preferences ->
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_IMG)
        }
    }
}
