package com.jongheon.myreadle.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jongheon.myreadle.domain.model.Level
import com.jongheon.myreadle.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val keyLevel = stringPreferencesKey("default_level")
    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyFontScale = floatPreferencesKey("font_scale")
    private val keyLastIndexUpdate = longPreferencesKey("last_index_update")

    val selectedLevel: Flow<Level> = context.dataStore.data.map { prefs ->
        prefs[keyLevel]?.let { Level.fromKey(it) } ?: Level.Default
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[keyTheme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System
    }

    val fontScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[keyFontScale] ?: 1.0f
    }

    val lastIndexUpdate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[keyLastIndexUpdate] ?: 0L
    }

    suspend fun setSelectedLevel(level: Level) {
        context.dataStore.edit { it[keyLevel] = level.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[keyFontScale] = scale.coerceIn(0.8f, 1.6f) }
    }

    suspend fun setLastIndexUpdate(epochMillis: Long) {
        context.dataStore.edit { it[keyLastIndexUpdate] = epochMillis }
    }
}
