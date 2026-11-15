package com.example.data.firebase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storeDataStore: DataStore<Preferences> by preferencesDataStore(name = "best_shoping_prefs")

class StorePreferences(private val context: Context) {
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val languageKey = stringPreferencesKey("language_code")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val recentlyViewedKey = stringPreferencesKey("recently_viewed_ids")

    val hasCompletedOnboarding: Flow<Boolean> = context.storeDataStore.data.map { prefs ->
        prefs[onboardingDoneKey] ?: false
    }

    val languageCode: Flow<String> = context.storeDataStore.data.map { prefs ->
        prefs[languageKey] ?: "en"
    }

    val themeMode: Flow<String> = context.storeDataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "system"
    }

    val recentlyViewedIds: Flow<List<String>> = context.storeDataStore.data.map { prefs ->
        prefs[recentlyViewedKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.storeDataStore.edit { it[onboardingDoneKey] = done }
    }

    suspend fun setLanguage(lang: String) {
        context.storeDataStore.edit { it[languageKey] = if (lang == "bn") "bn" else "en" }
    }

    suspend fun setThemeMode(mode: String) {
        context.storeDataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun addRecentlyViewed(productId: String) {
        context.storeDataStore.edit { prefs ->
            val current = prefs[recentlyViewedKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(productId) + current.filter { it != productId }).take(10)
            prefs[recentlyViewedKey] = updated.joinToString(",")
        }
    }
}
