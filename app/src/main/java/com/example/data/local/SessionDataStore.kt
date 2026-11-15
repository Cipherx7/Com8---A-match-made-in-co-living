package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode(val code: String, val label: String) {
    SYSTEM("SYSTEM", "System"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark");

    companion object {
        fun fromCode(code: String?): AppThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

private val Context.com8SessionStore: DataStore<Preferences> by preferencesDataStore(name = "com8_session")

class SessionDataStore(private val context: Context) {
    private val userIdKey = stringPreferencesKey("user_id")
    private val legacyPhoneKey = stringPreferencesKey("active_phone_number")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val permissionsPromptedKey = booleanPreferencesKey("permissions_prompted")

    val userIdFlow: Flow<String?> = context.com8SessionStore.data.map { prefs ->
        prefs[userIdKey] ?: prefs[legacyPhoneKey]
    }

    val activePhoneFlow: Flow<String?> = userIdFlow

    val themeModeFlow: Flow<AppThemeMode> = context.com8SessionStore.data.map { prefs ->
        AppThemeMode.fromCode(prefs[themeModeKey])
    }

    val permissionsPromptedFlow: Flow<Boolean> = context.com8SessionStore.data.map { prefs ->
        prefs[permissionsPromptedKey] ?: false
    }

    suspend fun saveUserId(userId: String) {
        context.com8SessionStore.edit { prefs ->
            prefs[userIdKey] = userId
            prefs.remove(legacyPhoneKey)
        }
    }

    suspend fun setActivePhone(phoneNumber: String) {
        saveUserId(phoneNumber)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.com8SessionStore.edit { prefs ->
            prefs[themeModeKey] = mode.code
        }
    }

    suspend fun setPermissionsPrompted(prompted: Boolean) {
        context.com8SessionStore.edit { prefs ->
            prefs[permissionsPromptedKey] = prompted
        }
    }

    suspend fun clearSession() {
        context.com8SessionStore.edit { prefs ->
            prefs.remove(userIdKey)
            prefs.remove(legacyPhoneKey)
            prefs.remove(permissionsPromptedKey)
        }
    }

    suspend fun clearAll() {
        context.com8SessionStore.edit { prefs ->
            prefs.clear()
        }
    }
}
