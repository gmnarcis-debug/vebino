package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.CalendarType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.webinoDataStore: DataStore<Preferences> by preferencesDataStore(name = "webino_user_prefs")

data class UserPreferences(
    val language: AppLanguage = AppLanguage.FA,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val fontChoice: AppFontChoice = AppFontChoice.VAZIRMATN,
    val calendarType: CalendarType = CalendarType.JALALI,
    val usePersianDigits: Boolean = true,
    val clearPreviewCacheOnExit: Boolean = false,
    val redactSensitiveLogs: Boolean = true,
    // Defaults to true so the tutorial never flashes before DataStore finishes loading.
    val onboardingCompleted: Boolean = true,
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val CALENDAR_TYPE = stringPreferencesKey("calendar_type")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
        val CLEAR_CACHE_EXIT = booleanPreferencesKey("clear_cache_exit")
        val REDACT_LOGS = booleanPreferencesKey("redact_logs")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val preferences: Flow<UserPreferences> = context.webinoDataStore.data.map { prefs ->
        UserPreferences(
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.FA,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.LIGHT,
            fontChoice = prefs[Keys.FONT_CHOICE]?.let { runCatching { AppFontChoice.valueOf(it) }.getOrNull() }
                ?: AppFontChoice.VAZIRMATN,
            calendarType = prefs[Keys.CALENDAR_TYPE]?.let { runCatching { CalendarType.valueOf(it) }.getOrNull() }
                ?: CalendarType.JALALI,
            usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: true,
            clearPreviewCacheOnExit = prefs[Keys.CLEAR_CACHE_EXIT] ?: false,
            redactSensitiveLogs = prefs[Keys.REDACT_LOGS] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
        )
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.webinoDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.webinoDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateFontChoice(font: AppFontChoice) {
        context.webinoDataStore.edit { it[Keys.FONT_CHOICE] = font.name }
    }

    suspend fun updateCalendarType(calendar: CalendarType) {
        context.webinoDataStore.edit { it[Keys.CALENDAR_TYPE] = calendar.name }
    }

    suspend fun updatePersianDigits(enabled: Boolean) {
        context.webinoDataStore.edit { it[Keys.PERSIAN_DIGITS] = enabled }
    }

    suspend fun updateClearCacheOnExit(enabled: Boolean) {
        context.webinoDataStore.edit { it[Keys.CLEAR_CACHE_EXIT] = enabled }
    }

    suspend fun updateRedactLogs(enabled: Boolean) {
        context.webinoDataStore.edit { it[Keys.REDACT_LOGS] = enabled }
    }

    suspend fun updateOnboardingCompleted(done: Boolean) {
        context.webinoDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }
}
