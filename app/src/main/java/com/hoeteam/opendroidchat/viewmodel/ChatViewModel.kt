/*
OpenDroidChat Chat View Model
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoeteam.opendroidchat.data.*
import com.hoeteam.opendroidchat.network.LlmApiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiService: LlmApiService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ------------------- UI 状态 -------------------

    val currentModel: StateFlow<LlmModel?> = settingsRepository.currentModelFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allModels: StateFlow<List<LlmModel>> = settingsRepository.allModelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()

    // ------------------- 聊天逻辑 -------------------

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun sendMessage() {
        val currentModel = currentModel.value
        val currentText = _inputText.value.trim()

        if (currentModel == null || currentModel.apiKey.isBlank() || currentModel.modelName.isBlank()) {
            viewModelScope.launch { _errorState.emit("请先配置有效的 LLM API 实例！") }
            return
        }

        if (currentText.isBlank() || _isLoading.value) return

        val userMessage = Message(text = currentText, sender = Sender.USER)
        _messages.update { it + userMessage }
        _inputText.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            val apiMessages = _messages.value
                .filter { !it.text.contains("LLM API 响应出错") }
                .map {
                    ApiMessage(
                        role = if (it.sender == Sender.USER) "user" else "assistant",
                        content = it.text
                    )
                }

            try {
                val llmResponseText = apiService.sendChat(apiMessages, currentModel)

                val llmMessage = Message(text = llmResponseText, sender = Sender.LLM)
                _messages.update { it + llmMessage }

            } catch (e: Exception) {
                val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
                val errorMsgForUi = Message(text = errorMessage, sender = Sender.LLM)
                _messages.update { it + errorMsgForUi }
                _errorState.emit("API 调用失败：${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    // ------------------- 模型配置逻辑 -------------------

    fun addOrUpdateModel(model: LlmModel) {
        viewModelScope.launch {
            settingsRepository.addOrUpdateModel(model)
            if (currentModel.value == null || currentModel.value?.id == model.id) {
                settingsRepository.setCurrentModel(model.id)
            }
        }
    }

    fun deleteModel(model: LlmModel) {
        viewModelScope.launch { settingsRepository.deleteModel(model) }
    }

    fun setCurrentModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.setCurrentModel(modelId)
            clearChat()
        }
    }
}
// ------------------- ViewModel Factory -------------------
// 必须创建 Factory 来将 Repository 和 Service 传递给 ViewModel
class ChatViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            val apiService = LlmApiService()
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(apiService, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}