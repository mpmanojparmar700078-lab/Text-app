package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iti_exam_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        // Default Admin PIN: 8070
        const val DEFAULT_ADMIN_PIN = "8070"

        fun hashPin(pin: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val activeUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USER_ID]
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    val adminPinHashFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ADMIN_PIN_HASH] ?: hashPin(DEFAULT_ADMIN_PIN)
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "en"
    }

    suspend fun setActiveUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_USER_ID] = userId
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setAdminPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ADMIN_PIN_HASH] = hashPin(pin.trim())
        }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = lang
        }
    }

    suspend fun verifyAdminPin(enteredPin: String, storedHash: String): Boolean {
        return hashPin(enteredPin.trim()) == storedHash
    }
}
