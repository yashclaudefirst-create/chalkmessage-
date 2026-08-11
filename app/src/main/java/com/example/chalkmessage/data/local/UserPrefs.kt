package com.example.chalkmessage.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property: creates a singleton DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {
    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val INVITE_CODE = stringPreferencesKey("invite_code")
        val CONNECTED_TO = stringPreferencesKey("connected_to") // comma-separated IDs
        val HAS_SKIPPED_CONNECTION = booleanPreferencesKey("has_skipped_connection")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val inviteCode: Flow<String?> = context.dataStore.data.map { it[INVITE_CODE] }
    val connectedTo: Flow<String?> = context.dataStore.data.map { it[CONNECTED_TO] }
    val hasSkippedConnection: Flow<Boolean> = context.dataStore.data.map { it[HAS_SKIPPED_CONNECTION] ?: false }

    suspend fun saveUser(id: String, name: String, code: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USER_NAME] = name
            prefs[INVITE_CODE] = code
        }
    }

    suspend fun setHasSkippedConnection(skipped: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_SKIPPED_CONNECTION] = skipped
        }
    }

    suspend fun addConnection(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[CONNECTED_TO] ?: ""
            prefs[CONNECTED_TO] = if (current.isEmpty()) userId else "$current,$userId"
        }
    }

    suspend fun setConnectedPartner(partnerId: String) {
        context.dataStore.edit { prefs ->
            prefs[CONNECTED_TO] = partnerId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
