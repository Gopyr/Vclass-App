package com.vclass.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vclass_settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
    val SAVED_USERNAME = stringPreferencesKey("saved_username")
    val SAVED_PASSWORD = stringPreferencesKey("saved_password")
    val MOODLE_TOKEN = stringPreferencesKey("moodle_token")
    val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
    val SAVED_ACCOUNTS = stringPreferencesKey("saved_accounts")
}

data class SavedLogin(
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val rememberLogin: Boolean = false
)

data class SavedAccount(
    val username: String = "",
    val password: String = "",
    val token: String = ""
)

class SettingsRepository(private val context: Context) {

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.DARK_MODE] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val refreshInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.REFRESH_INTERVAL_MINUTES] ?: 15
    }

    val savedLogin: Flow<SavedLogin> = context.dataStore.data.map { prefs ->
        SavedLogin(
            username = prefs[SettingsKeys.SAVED_USERNAME].orEmpty(),
            password = prefs[SettingsKeys.SAVED_PASSWORD].orEmpty(),
            token = prefs[SettingsKeys.MOODLE_TOKEN].orEmpty(),
            rememberLogin = prefs[SettingsKeys.REMEMBER_LOGIN] ?: false
        )
    }

    val savedAccounts: Flow<List<SavedAccount>> = context.dataStore.data.map { prefs ->
        decodeAccounts(prefs[SettingsKeys.SAVED_ACCOUNTS].orEmpty())
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setRefreshInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.REFRESH_INTERVAL_MINUTES] = minutes
        }
    }

    suspend fun saveLogin(username: String, password: String, token: String, rememberLogin: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.REMEMBER_LOGIN] = rememberLogin
            prefs[SettingsKeys.MOODLE_TOKEN] = token
            if (rememberLogin) {
                prefs[SettingsKeys.SAVED_USERNAME] = username
                prefs[SettingsKeys.SAVED_PASSWORD] = password
                val accounts = decodeAccounts(prefs[SettingsKeys.SAVED_ACCOUNTS].orEmpty())
                    .filterNot { it.username.equals(username, ignoreCase = true) }
                    .toMutableList()
                accounts.add(0, SavedAccount(username = username, password = password, token = token))
                prefs[SettingsKeys.SAVED_ACCOUNTS] = encodeAccounts(accounts)
            } else {
                prefs.remove(SettingsKeys.SAVED_USERNAME)
                prefs.remove(SettingsKeys.SAVED_PASSWORD)
            }
        }
    }

    suspend fun switchAccount(account: SavedAccount) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.SAVED_USERNAME] = account.username
            prefs[SettingsKeys.SAVED_PASSWORD] = account.password
            prefs[SettingsKeys.MOODLE_TOKEN] = account.token
            prefs[SettingsKeys.REMEMBER_LOGIN] = true
        }
    }

    suspend fun clearLogin(clearAccounts: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs.remove(SettingsKeys.SAVED_USERNAME)
            prefs.remove(SettingsKeys.SAVED_PASSWORD)
            prefs.remove(SettingsKeys.MOODLE_TOKEN)
            prefs[SettingsKeys.REMEMBER_LOGIN] = false
            if (clearAccounts) {
                prefs.remove(SettingsKeys.SAVED_ACCOUNTS)
            }
        }
    }

    private fun decodeAccounts(raw: String): List<SavedAccount> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val username = item.optString("username")
                    val password = item.optString("password")
                    val token = item.optString("token")
                    if (username.isNotBlank() && token.isNotBlank()) {
                        add(SavedAccount(username = username, password = password, token = token))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeAccounts(accounts: List<SavedAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("username", account.username)
                    .put("password", account.password)
                    .put("token", account.token)
            )
        }
        return array.toString()
    }
}
