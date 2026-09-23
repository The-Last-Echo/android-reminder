package com.thelastecho.reminder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        preferencesRepository.themeSettings.onEach { settings ->
            _uiState.update { current ->
                current.copy(
                    darkThemeConfig = settings.darkThemeConfig,
                    isAmoledMode = settings.isAmoledMode,
                    useDynamicColors = settings.useDynamicColors
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.SetDarkThemeConfig -> {
                    preferencesRepository.setDarkThemeConfig(intent.config)
                }
                is SettingsIntent.SetAmoledMode -> {
                    preferencesRepository.setAmoledMode(intent.enabled)
                }
                is SettingsIntent.SetDynamicColors -> {
                    preferencesRepository.setUseDynamicColors(intent.enabled)
                }
            }
        }
    }
}
