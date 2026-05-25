package com.jongheon.myreadle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jongheon.myreadle.core.ServiceLocator
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.domain.model.Level
import com.jongheon.myreadle.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val level: Level = Level.Default,
    val fontScale: Float = 1.0f,
    val themeMode: ThemeMode = ThemeMode.System,
)

class SettingsViewModel(
    private val settings: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.selectedLevel,
        settings.fontScale,
        settings.themeMode,
    ) { level, scale, theme ->
        SettingsUiState(level = level, fontScale = scale, themeMode = theme)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setLevel(level: Level) {
        viewModelScope.launch { settings.setSelectedLevel(level) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setFontScale(scale) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(settings = ServiceLocator.settingsDataStore)
            }
        }
    }
}
