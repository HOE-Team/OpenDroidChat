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
import com.hoeteam.opendroidchat.network.ChatResult
import com.hoeteam.opendroidchat.network.LlmApiService
import com.hoeteam.opendroidchat.network.StreamChunk
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiService: LlmApiService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ------------------- UI 状态 -------------------
    // 使用 Eagerly 策略让数据在 ViewModel 创建时立即开始加载，
    // 避免导航切换时因 WhileSubscribed 超时导致重新加载和解密

    val currentModel: StateFlow<LlmModel?> = settingsRepository.currentModelFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val allModels: StateFlow<List<LlmModel>> = settingsRepository.allModelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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

    // 文件选择状态
    private val _selectedFile = MutableStateFlow<SelectedFile?>(null)
    val selectedFile: StateFlow<SelectedFile?> = _selectedFile.asStateFlow()

    // ------------------- 聊天逻辑 -------------------

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setSelectedFile(file: SelectedFile?) {
        _selectedFile.value = file
    }

    fun clearSelectedFile() {
        _selectedFile.value = null
    }

    fun sendMessage() {
        val currentModel = currentModel.value
        val currentText = _inputText.value.trim()
        val currentFile = _selectedFile.value

        if (currentModel == null || currentModel.apiKey.isBlank() || currentModel.modelName.isBlank()) {
            viewModelScope.launch { _errorState.emit("请先配置有效的 LLM API 实例！") }
            return
        }

        if (currentText.isBlank() && currentFile == null || _isLoading.value) return
        if (currentText.isBlank() && currentFile == null) return

        // 用户消息只保存文本本身，不包含文件内容
        val userMessage = Message(text = currentText, sender = Sender.USER, selectedFile = currentFile)
        _messages.update { it + userMessage }
        _inputText.value = ""
        _selectedFile.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val apiMessages = _messages.value
                .filter { !it.text.contains("LLM API 响应出错") }
                .map { msg ->
                    // 如果消息有文件附件，将文件内容合并到 API 请求中
                    val apiContent = if (msg.selectedFile != null) {
                        buildUserMessageText(msg.text, msg.selectedFile)
                    } else {
                        msg.text
                    }
                    ApiMessage(
                        role = if (msg.sender == Sender.USER) "user" else "assistant",
                        content = apiContent
                    )
                }

            if (currentModel.useStream) {
                // 流式请求逻辑
                val llmMessageId = System.currentTimeMillis() + 1
                val initialLlmMessage = Message(
                    id = llmMessageId,
                    text = "",
                    sender = Sender.LLM,
                    isStreaming = true
                )
                _messages.update { it + initialLlmMessage }

                var fullResponseText = ""
                var fullReasoningText = ""
                var isThinkingStreaming = false
                // 禁用思考模式时忽略响应中的思考内容
                val shouldIgnoreThinking = !currentModel.enableThinking

                try {
                    apiService.sendChatStream(apiMessages, currentModel)
                        .catch { e ->
                            val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
                            updateMessageText(llmMessageId, errorMessage, isStreaming = false)
                            _errorState.emit("API 调用失败：${e.localizedMessage}")
                            _isLoading.value = false
                        }
                        .collect { chunk ->
                            if (fullResponseText.isEmpty() && chunk.reasoningText == null) {
                                _isLoading.value = false
                            }

                            if (shouldIgnoreThinking) {
                                // 禁用思考时，只取文本内容，忽略思考内容
                                if (chunk.content.isNotEmpty()) {
                                    fullResponseText += chunk.content
                                }
                                updateMessageWithReasoning(
                                    llmMessageId,
                                    fullResponseText,
                                    null,
                                    isStreaming = true,
                                    isThinkingStreaming = false
                                )
                            } else {
                                // 启用思考时，正常处理思考和文本
                                if (chunk.reasoningText != null) {
                                    fullReasoningText = chunk.reasoningText
                                    if (!isThinkingStreaming) {
                                        isThinkingStreaming = true
                                    }
                                }

                                if (chunk.content.isNotEmpty()) {
                                    fullResponseText += chunk.content
                                    isThinkingStreaming = false
                                }

                                updateMessageWithReasoning(
                                    llmMessageId,
                                    fullResponseText,
                                    fullReasoningText.ifEmpty { null },
                                    isStreaming = true,
                                    isThinkingStreaming = isThinkingStreaming
                                )
                            }
                        }
                    updateMessageWithReasoning(
                        llmMessageId,
                        fullResponseText,
                        if (shouldIgnoreThinking) null else fullReasoningText.ifEmpty { null },
                        isStreaming = false,
                        isThinkingStreaming = false
                    )
                } catch (e: Exception) {
                    _isLoading.value = false
                } finally {
                    _isLoading.value = false
                }
            } else {
                // 传统非流式请求逻辑
                try {
                    val result = apiService.sendChat(apiMessages, currentModel)
                    val llmMessage = Message(
                        text = result.content,
                        sender = Sender.LLM,
                        reasoningText = result.reasoningText
                    )
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
    }

    /**
     * 构建用户消息文本，将文件内容附加到消息中
     */
    private fun buildUserMessageText(text: String, file: SelectedFile?): String {
        if (file == null) return text
        val fileInfo = if (text.isBlank()) {
            "以下文件内容：\n文件名：${file.fileName}\n```\n${file.content}\n```"
        } else {
            "$text\n\n以下文件内容：\n文件名：${file.fileName}\n```\n${file.content}\n```"
        }
        return fileInfo
    }

    private fun updateMessageText(id: Long, newText: String, isStreaming: Boolean) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == id) {
                    msg.copy(text = newText, isStreaming = isStreaming)
                } else {
                    msg
                }
            }
        }
    }

    private fun updateMessageWithReasoning(
        id: Long,
        newText: String,
        reasoningText: String?,
        isStreaming: Boolean,
        isThinkingStreaming: Boolean = false
    ) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == id) {
                    msg.copy(
                        text = newText,
                        reasoningText = reasoningText,
                        isStreaming = isStreaming,
                        isThinkingStreaming = isThinkingStreaming
                    )
                } else {
                    msg
                }
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