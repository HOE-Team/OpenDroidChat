/*
OpenDroidChat Theme View Model
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoeteam.opendroidchat.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    
    // 主题状态 Flow
    val darkThemeEnabled: StateFlow<Boolean> = settingsRepository.darkThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // 更新渠道状态 Flow
    val allowOtherChannelsUpdate: StateFlow<Boolean> = settingsRepository.allowOtherChannelsUpdateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * 切换主题
     */
    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
    }

    /**
     * 设置是否允许其他渠道更新
     */
    fun setAllowOtherChannelsUpdate(allow: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAllowOtherChannelsUpdate(allow)
        }
    }
}

class ThemeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
