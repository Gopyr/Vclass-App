package com.vclass.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object OnboardingKeys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
}

class OnboardingPrefs(private val context: Context) {

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[OnboardingKeys.ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[OnboardingKeys.ONBOARDING_DONE] = true
        }
    }
}
