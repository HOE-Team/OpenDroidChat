package com.hoeteam.opendroidchat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoeteam.opendroidchat.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    
    // 主题状态 Flow，初始值为 false
    val darkThemeEnabled: StateFlow<Boolean> = settingsRepository.darkThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * 切换主题
     * @param isDark 是否使用深色主题
     */
    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
    }

    /**
     * 切换主题状态（当前状态的反向）
     */
    fun toggleTheme() {
        viewModelScope.launch {
            val currentState = darkThemeEnabled.value
            settingsRepository.setDarkTheme(!currentState)
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
