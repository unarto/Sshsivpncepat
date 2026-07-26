package com.sivpn.cepat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sivpn.cepat.config.ProfileManager
import com.sivpn.cepat.repository.LogRepository
import com.sivpn.cepat.config.SettingsManager

class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val settingsManager = com.sivpn.cepat.config.SettingsManager(context.applicationContext)
            val logRepo = LogRepository()
            val profileManager = com.sivpn.cepat.config.ProfileManager(context.applicationContext, settingsManager)
            return MainViewModel(settingsManager, logRepo, profileManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
