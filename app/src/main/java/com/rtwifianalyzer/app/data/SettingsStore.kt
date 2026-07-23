package com.rtwifianalyzer.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rtwifianalyzer.app.domain.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit

private val Context.dataStore by preferencesDataStore("settings")
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    val theme: Flow<AppTheme> = context.dataStore.data.map { runCatching { AppTheme.valueOf(it[themeKey] ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM) }
    suspend fun setTheme(theme: AppTheme) { context.dataStore.edit { it[themeKey] = theme.name } }
}
