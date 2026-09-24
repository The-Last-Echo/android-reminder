package com.thelastecho.reminder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        preferencesRepository.themeSettings.onEach { settings ->
            _uiState.update { current ->
                current.copy(
                    darkThemeConfig = settings.darkThemeConfig,
                    isAmoledMode = settings.isAmoledMode,
                    useDynamicColors = settings.useDynamicColors,
                    notificationStyle = settings.notificationStyle,
                    alarmSoundUri = settings.alarmSoundUri,
                    completedReminderRetentionDays = settings.completedReminderRetentionDays,
                    addButtonOnLeft = settings.addButtonOnLeft,
                    automaticUpdateChecks = settings.automaticUpdateChecks,
                    latestReleaseTag = settings.latestReleaseTag,
                    latestReleaseUrl = settings.latestReleaseUrl,
                    lastUpdateCheckMillis = settings.lastUpdateCheckMillis
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
                is SettingsIntent.SetNotificationStyle -> {
                    preferencesRepository.setNotificationStyle(intent.style)
                }
                is SettingsIntent.SetAlarmSound -> preferencesRepository.setAlarmSoundUri(intent.uri)
                is SettingsIntent.SetCompletedRetention -> preferencesRepository.setCompletedRetentionDays(intent.days)
                is SettingsIntent.SetAddButtonOnLeft -> preferencesRepository.setAddButtonOnLeft(intent.enabled)
                is SettingsIntent.SetAutomaticUpdateChecks -> preferencesRepository.setAutomaticUpdateChecks(intent.enabled)
                SettingsIntent.NavigateToCategories -> {
                    _effect.send(SettingsEffect.NavigateToCategories)
                }
                SettingsIntent.NavigateToTrash -> _effect.send(SettingsEffect.NavigateToTrash)
                SettingsIntent.NavigateToBackup -> _effect.send(SettingsEffect.NavigateToBackup)
                SettingsIntent.NavigateToPrivacy -> _effect.send(SettingsEffect.NavigateToPrivacy)
            }
        }
    }
}
