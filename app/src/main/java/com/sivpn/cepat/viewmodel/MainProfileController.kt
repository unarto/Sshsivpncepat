package com.sivpn.cepat.viewmodel

import com.sivpn.cepat.config.DefaultValues
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.repository.LogRepository
import com.sivpn.cepat.config.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainProfileController(
    private val settingsManager: SettingsManager,
    private val logRepository: LogRepository,
    private val _uiState: MutableStateFlow<MainUiState>,
    private val refreshUiState: () -> Unit
) {

    private companion object {
        const val DEFAULT_PROFILE = DefaultValues.DEFAULT_PROFILE
    }

    private fun updateState(transform: (MainUiState) -> MainUiState) {
        _uiState.update(transform)
    }

    fun loadSettings() {
        val currentProfile = settingsManager.getCurrentProfile()
        val profileList = settingsManager.getProfiles().toList()

        updateState { state ->
            state.copy(
                currentProfile = currentProfile,
                profileList = profileList
            )
        }
    }

    fun selectProfile(profile: String) {
        settingsManager.setCurrentProfile(profile)
        refreshUiState()
        logRepository.addLog("Switched profile to: $profile")
    }

    fun addProfile(name: String) {
        if (name.isNotBlank()) {
            settingsManager.addProfile(name)
            selectProfile(name)
        }
    }

    fun deleteProfile(profile: String): Boolean {
        val list = settingsManager.getProfiles()
        if (list.size > 1 && profile != DEFAULT_PROFILE) {
            settingsManager.removeProfile(profile)
            loadSettings()
            return true
        }
        return false
    }

    fun deleteCurrentProfile(): Boolean {
        val list = settingsManager.getProfiles()
        val current = settingsManager.getCurrentProfile()
        if (list.size > 1) {
            settingsManager.removeProfile(current)
            val newProfile = settingsManager.getProfiles().firstOrNull() ?: DEFAULT_PROFILE
            selectProfile(newProfile)
            return true
        }
        return false
    }
}
