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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * 错误信息携带重试所需的数据
 */
data class ApiErrorState(
    val message: String,
    val lastApiMessages: List<ApiMessage> // 重试时需要的 API 消息
)

class ChatViewModel(
    private val apiService: LlmApiService,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
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

    val mcpServers: StateFlow<List<McpServer>> = settingsRepository.mcpServersFlow.stateIn(
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

    // 错误状态：携带重试所需数据
    private val _errorState = MutableSharedFlow<ApiErrorState>()
    val errorState: SharedFlow<ApiErrorState> = _errorState.asSharedFlow()

    // 文件选择状态
    private val _selectedFile = MutableStateFlow<SelectedFile?>(null)
    val selectedFile: StateFlow<SelectedFile?> = _selectedFile.asStateFlow()

    // 消息编辑状态
    private val _editingMessageId = MutableStateFlow<Long?>(null)
    val editingMessageId: StateFlow<Long?> = _editingMessageId.asStateFlow()

    private val _editingText = MutableStateFlow("")
    val editingText: StateFlow<String> = _editingText.asStateFlow()

    // 缓存最近一次发送的 API 消息列表，用于重试
    private var lastApiMessages: List<ApiMessage> = emptyList()

    // ------------------- 历史对话状态 -------------------

    // 当前会话的持久化实体（内存中的最新快照，用于更新标题/时间）
    private var activeConversation: Conversation? = null

    // 会话保存与「清除当前会话 ID」都串行执行，保证落盘顺序
    private val persistenceMutex = Mutex()

    // 已使用过的最大消息 ID：同一毫秒内创建多条消息时也不会撞 id
    // （LazyColumn 以 id 作为 key，重复会导致崩溃）
    private var lastMessageId: Long = 0L

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _conversationTitle = MutableStateFlow<String?>(null)
    val conversationTitle: StateFlow<String?> = _conversationTitle.asStateFlow()

    /** 历史对话列表（按最近更新时间倒序） */
    val conversations: StateFlow<List<ConversationSummary>> = conversationRepository.conversations

    init {
        restoreLastConversation()
    }

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

    // ------------------- 消息编辑 / 重新生成 -------------------

    /**
     * 开始编辑某条用户消息
     */
    fun startEditing(messageId: Long) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        if (_isLoading.value) return
        _editingMessageId.value = messageId
        _editingText.value = msg.text
    }

    fun updateEditingText(newText: String) {
        _editingText.value = newText
    }

    fun cancelEditing() {
        _editingMessageId.value = null
        _editingText.value = ""
    }

    /**
     * 保存用户消息的修改，并截断其后内容重新发送给大语言模型
     */
    fun saveEditedMessage(messageId: Long) {
        val currentModel = currentModel.value ?: return
        val newText = _editingText.value.trim()
        if (newText.isBlank() || _isLoading.value) return

        val idx = _messages.value.indexOfFirst { it.id == messageId }
        if (idx < 0) return

        // 更新该用户消息的文本，并截断其后所有消息（重新生成后续内容）
        _messages.update { list ->
            val updated = list.toMutableList()
            updated[idx] = updated[idx].copy(text = newText)
            updated.subList(0, idx + 1)
        }

        _editingMessageId.value = null
        _editingText.value = ""
        _isLoading.value = true
        // 截断后的对话已经是新的历史版本，先保存再请求
        persistActiveConversation()

        viewModelScope.launch {
            val apiMessages = buildApiMessages()
            if (apiMessages.isEmpty()) {
                _isLoading.value = false
                return@launch
            }
            lastApiMessages = apiMessages
            executeChatRequest(currentModel, apiMessages)
        }
    }

    /**
     * 让大语言模型重新生成指定 AI 消息（移除该消息及其后内容后重新请求）
     */
    fun regenerateMessage(messageId: Long) {
        val currentModel = currentModel.value ?: return
        if (_isLoading.value) return

        val idx = _messages.value.indexOfFirst { it.id == messageId }
        if (idx < 0) return

        // 移除该 AI 消息及其后所有消息，保留其前内容
        _messages.update { list -> list.subList(0, idx) }
        _isLoading.value = true
        persistActiveConversation()

        viewModelScope.launch {
            val apiMessages = buildApiMessages()
            if (apiMessages.isEmpty()) {
                _isLoading.value = false
                return@launch
            }
            lastApiMessages = apiMessages
            executeChatRequest(currentModel, apiMessages)
        }
    }

    /**
     * 重试最后一次 API 调用
     */
    fun retryLastRequest() {
        val currentModel = currentModel.value ?: return
        val cachedMessages = lastApiMessages
        if (cachedMessages.isEmpty()) return

        // 移除最后一条错误消息
        _messages.update { list ->
            if (list.isNotEmpty() && list.last().text.contains("LLM API 响应出错")) {
                list.dropLast(1)
            } else {
                list
            }
        }

        _isLoading.value = true
        viewModelScope.launch {
            executeChatRequest(currentModel, cachedMessages)
        }
    }

    fun sendMessage() {
        val currentModel = currentModel.value
        val currentText = _inputText.value.trim()
        val currentFile = _selectedFile.value

        if (currentModel == null || currentModel.apiKey.isBlank() || currentModel.modelName.isBlank()) {
            viewModelScope.launch {
                _errorState.emit(ApiErrorState("请先配置有效的 LLM API 实例！", emptyList()))
            }
            return
        }

        if (currentText.isBlank() && currentFile == null || _isLoading.value) return
        if (currentText.isBlank() && currentFile == null) return

        // 用户消息只保存文本本身，不包含文件内容
        val userMessage = Message(
            id = nextMessageId(),
            text = currentText,
            sender = Sender.USER,
            selectedFile = currentFile
        )
        _messages.update { it + userMessage }
        _inputText.value = ""
        _selectedFile.value = null
        _isLoading.value = true
        // 立即落盘：即使随后请求失败或进程被杀，用户的输入也不会丢
        persistActiveConversation()

        viewModelScope.launch {
            val apiMessages = buildApiMessages()
            lastApiMessages = apiMessages
            executeChatRequest(currentModel, apiMessages)
        }
    }

    /**
     * 构建发送给 API 的消息列表
     */
    private fun buildApiMessages(): List<ApiMessage> {
        return _messages.value
            .filter { !it.text.contains("LLM API 响应出错") }
            .map { msg ->
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
    }

    /**
     * 执行聊天请求（流式/非流式 / MCP 工具调用）
     */
    private suspend fun executeChatRequest(currentModel: LlmModel, apiMessages: List<ApiMessage>) {
        val enabledServers = mcpServers.value.filter { it.enabled }
        if (currentModel.enableMcp && enabledServers.isNotEmpty()) {
            executeMcpRequest(currentModel, apiMessages, enabledServers)
            return
        }
        if (currentModel.useStream) {
            executeStreamingRequest(currentModel, apiMessages)
        } else {
            executeNonStreamingRequest(currentModel, apiMessages)
        }
    }

    private suspend fun executeMcpRequest(
        currentModel: LlmModel,
        apiMessages: List<ApiMessage>,
        mcpServers: List<McpServer>
    ) {
        val useStream = currentModel.useStream
        val msgId = nextMessageId()

        // 先插入占位消息：流式模式用于打字机效果；同时用于展示 MCP 调用容器（含加载转圈）
        _messages.update { it + Message(id = msgId, text = "", sender = Sender.LLM, isStreaming = useStream) }

        try {
            val result = apiService.sendChatWithMcp(
                apiMessages, currentModel, mcpServers,
                stream = useStream,
                onChunk = { content: String, reasoningText: String? ->
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == msgId) {
                                m.copy(
                                    text = if (content.isEmpty()) m.text else m.text + content,
                                    reasoningText = reasoningText ?: m.reasoningText,
                                    isStreaming = true,
                                    isThinkingStreaming = !reasoningText.isNullOrBlank()
                                )
                            } else m
                        }
                    }
                },
                onToolStart = { serverName: String, toolName: String ->
                    // 工具调用开始：在 MCP 容器中追加一条"进行中"记录（显示加载转圈）
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == msgId) {
                                val segments = m.segments.toMutableList()
                                if (!m.reasoningText.isNullOrBlank()) {
                                    segments += McpSegment.Thinking(m.reasoningText)
                                }
                                val last = segments.lastOrNull()
                                if (last is McpSegment.Calls) {
                                    segments[segments.size - 1] = McpSegment.Calls(
                                        last.calls + McpCallInfo(serverName = serverName, toolName = toolName, durationMs = 0, pending = true)
                                    )
                                } else {
                                    segments += McpSegment.Calls(
                                        listOf(McpCallInfo(serverName = serverName, toolName = toolName, durationMs = 0, pending = true))
                                    )
                                }
                                m.copy(
                                    reasoningText = null,
                                    segments = segments
                                )
                            } else m
                        }
                    }
                },
                onToolComplete = { completed: McpCallInfo ->
                    // 工具调用完成：将"进行中"的调用替换为完成状态（立即显示）
                    _messages.update { list ->
                        list.map { m ->
                            if (m.id == msgId) {
                                var replaced = false
                                val newSegments = m.segments.map { seg ->
                                    if (replaced) seg
                                    else when (seg) {
                                        is McpSegment.Thinking -> seg
                                        is McpSegment.Calls -> McpSegment.Calls(seg.calls.map { call ->
                                            if (!replaced && call.pending) {
                                                replaced = true
                                                completed
                                            } else call
                                        })
                                    }
                                }
                                m.copy(segments = newSegments)
                            } else m
                        }
                    }
                },
            )
            // 最终化消息
            _messages.update { list ->
                list.map { m ->
                    if (m.id == msgId) {
                        // 用最终结果替换"进行中"的调用信息，保留分段顺序
                        val finalCalls = result.mcpCalls
                        var callIdx = 0
                        val finalizedSegments = m.segments.map { seg ->
                            when (seg) {
                                is McpSegment.Thinking -> seg
                                is McpSegment.Calls -> McpSegment.Calls(seg.calls.map { call ->
                                    if (call.pending && callIdx < finalCalls.size) {
                                        val f = finalCalls[callIdx]
                                        callIdx++
                                        f
                                    } else call
                                })
                            }
                        }
                        m.copy(
                            text = result.content,
                            reasoningText = result.reasoningText,
                            isStreaming = false,
                            isThinkingStreaming = false,
                            segments = finalizedSegments
                        )
                    } else m
                }
            }
        } catch (e: Exception) {
            val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
            _messages.update { list ->
                list.map { m ->
                    if (m.id == msgId) m.copy(text = errorMessage, isStreaming = false, isThinkingStreaming = false) else m
                }
            }
            _errorState.emit(ApiErrorState("API 调用失败：${e.localizedMessage}", apiMessages))
        } finally {
            _isLoading.value = false
            // 一次问答结束后保存会话（流式过程中不写盘，避免频繁 IO）
            persistActiveConversation()
        }
    }

    private suspend fun executeStreamingRequest(currentModel: LlmModel, apiMessages: List<ApiMessage>) {
        val llmMessageId = nextMessageId()
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
        val shouldIgnoreThinking = !currentModel.enableThinking
        var hasError = false

        try {
            apiService.sendChatStream(apiMessages, currentModel)
                .catch { e ->
                    hasError = true
                    val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
                    updateMessageText(llmMessageId, errorMessage, isStreaming = false)
                    _errorState.emit(ApiErrorState("API 调用失败：${e.localizedMessage}", apiMessages))
                    _isLoading.value = false
                }
                .collect { chunk ->
                    if (hasError) return@collect

                    if (fullResponseText.isEmpty() && chunk.reasoningText == null) {
                        _isLoading.value = false
                    }

                    if (shouldIgnoreThinking) {
                        if (chunk.content.isNotEmpty()) {
                            fullResponseText += chunk.content
                        }
                        updateMessageWithReasoning(
                            llmMessageId, fullResponseText, null,
                            isStreaming = true, isThinkingStreaming = false
                        )
                    } else {
                        if (chunk.reasoningText != null) {
                            fullReasoningText = chunk.reasoningText
                            if (!isThinkingStreaming) isThinkingStreaming = true
                        }
                        if (chunk.content.isNotEmpty()) {
                            fullResponseText += chunk.content
                            isThinkingStreaming = false
                        }
                        updateMessageWithReasoning(
                            llmMessageId, fullResponseText,
                            fullReasoningText.ifEmpty { null },
                            isStreaming = true, isThinkingStreaming = isThinkingStreaming
                        )
                    }
                }

            if (!hasError) {
                updateMessageWithReasoning(
                    llmMessageId, fullResponseText,
                    if (shouldIgnoreThinking) null else fullReasoningText.ifEmpty { null },
                    isStreaming = false, isThinkingStreaming = false
                )
            }
        } catch (e: Exception) {
            if (!hasError) {
                val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
                updateMessageText(llmMessageId, errorMessage, isStreaming = false)
                _errorState.emit(ApiErrorState("API 调用失败：${e.localizedMessage}", apiMessages))
            }
        } finally {
            _isLoading.value = false
            persistActiveConversation()
        }
    }

    private suspend fun executeNonStreamingRequest(currentModel: LlmModel, apiMessages: List<ApiMessage>) {
        try {
            val result = apiService.sendChat(apiMessages, currentModel)
            val llmMessage = Message(
                id = nextMessageId(),
                text = result.content,
                sender = Sender.LLM,
                reasoningText = result.reasoningText
            )
            _messages.update { it + llmMessage }
        } catch (e: Exception) {
            val errorMessage = "LLM API 响应出错: ${e.message ?: "未知错误"}"
            val errorMsgForUi = Message(id = nextMessageId(), text = errorMessage, sender = Sender.LLM)
            _messages.update { it + errorMsgForUi }
            _errorState.emit(ApiErrorState("API 调用失败：${e.localizedMessage}", apiMessages))
        } finally {
            _isLoading.value = false
            persistActiveConversation()
        }
    }

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
                if (msg.id == id) msg.copy(text = newText, isStreaming = isStreaming) else msg
            }
        }
    }

    private fun updateMessageWithReasoning(
        id: Long, newText: String, reasoningText: String?,
        isStreaming: Boolean, isThinkingStreaming: Boolean = false
    ) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == id) {
                    msg.copy(
                        text = newText, reasoningText = reasoningText,
                        isStreaming = isStreaming, isThinkingStreaming = isThinkingStreaming
                    )
                } else msg
            }
        }
    }

    /**
     * 新建对话：先把当前会话保存到历史记录，再清空界面开始新的对话
     */
    fun clearChat() {
        persistActiveConversation()
        resetConversationState()
    }

    // ------------------- 历史对话逻辑 -------------------

    /**
     * 冷启动时的初始会话。
     *
     * 默认从一段全新的对话开始；只有用户在设置里打开「启动时恢复上次对话」，
     * 才会续接上次打开的会话。
     */
    private fun restoreLastConversation() {
        viewModelScope.launch {
            // 先把历史列表读进内存，之后展开抽屉时无需再等待磁盘
            conversationRepository.refresh()

            if (!settingsRepository.resumeLastConversationFlow.first()) return@launch

            val lastId = settingsRepository.activeConversationIdFlow.first()
            if (lastId.isNullOrBlank() || _messages.value.isNotEmpty()) return@launch

            val conversation = conversationRepository.load(lastId)
            if (conversation == null) {
                // 会话文件已不存在（例如已被清空），忘掉这个 id
                settingsRepository.setActiveConversationId(null)
            } else if (_messages.value.isEmpty()) {
                // 恢复期间用户可能已经开始新对话，此时不要覆盖现有内容
                applyConversation(conversation)
            }
        }
    }

    /** 供历史列表页主动刷新（例如从其他页面返回时） */
    fun refreshConversations() {
        viewModelScope.launch { conversationRepository.refresh() }
    }

    /**
     * 打开一条历史对话：恢复其消息列表，之后可以直接继续发送消息。
     */
    fun openConversation(conversationId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            val conversation = conversationRepository.load(conversationId) ?: return@launch
            applyConversation(conversation)
            persistenceMutex.withLock {
                settingsRepository.setActiveConversationId(conversation.id)
            }
            // 如果该对话当初使用的模型仍然存在，就一并切回，保证继续对话时参数一致
            conversation.modelId?.let { modelId ->
                if (allModels.value.any { it.id == modelId }) {
                    settingsRepository.setCurrentModel(modelId)
                }
            }
        }
    }

    /** 删除单条历史对话；若删除的正是当前对话，则顺带清空界面 */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            persistenceMutex.withLock { conversationRepository.delete(conversationId) }
            if (activeConversation?.id == conversationId) {
                resetConversationState()
            }
        }
    }

    /** 清空全部历史对话 */
    fun deleteAllConversations() {
        viewModelScope.launch {
            persistenceMutex.withLock { conversationRepository.deleteAll() }
            resetConversationState()
        }
    }

    /**
     * 把当前消息列表保存为一个会话。
     *
     * 只保存「有实际内容」的消息（流式占位气泡不落盘），并在保存前清掉
     * isStreaming 标记，避免重新打开历史对话时一直显示打字机状态。
     */
    private fun persistActiveConversation() {
        val snapshot = _messages.value
            .filter {
                it.text.isNotBlank() ||
                    !it.reasoningText.isNullOrBlank() ||
                    it.segments.isNotEmpty()
            }
            .map { it.copy(isStreaming = false, isThinkingStreaming = false) }
        if (snapshot.isEmpty()) return

        val now = System.currentTimeMillis()
        val previous = activeConversation
        val current = currentModel.value
        val conversation = Conversation(
            id = previous?.id ?: UUID.randomUUID().toString(),
            title = previous?.title?.takeIf { it.isNotBlank() } ?: deriveTitle(snapshot),
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
            modelId = current?.id ?: previous?.modelId,
            modelName = current?.name ?: previous?.modelName,
            messages = snapshot
        )

        activeConversation = conversation
        _activeConversationId.value = conversation.id
        _conversationTitle.value = conversation.title.ifBlank { null }

        viewModelScope.launch {
            persistenceMutex.withLock {
                conversationRepository.save(conversation)
                settingsRepository.setActiveConversationId(conversation.id)
            }
        }
    }

    /** 用一条历史会话填充界面状态 */
    private fun applyConversation(conversation: Conversation) {
        val restoredMessages = ensureUniqueMessageIds(
            conversation.messages.map { it.copy(isStreaming = false, isThinkingStreaming = false) }
        )
        activeConversation = conversation.copy(messages = restoredMessages)
        // 先恢复消息再广播会话 id，界面收到 id 变化时消息已经就位（可直接滚动到底部）
        _messages.value = restoredMessages
        _activeConversationId.value = conversation.id
        _conversationTitle.value = conversation.title.ifBlank { null }
        lastMessageId = maxOf(lastMessageId, restoredMessages.maxOfOrNull { it.id } ?: 0L)
        lastApiMessages = buildApiMessages()
        _inputText.value = ""
        _selectedFile.value = null
        cancelEditing()
    }

    /** 清空界面状态并解除与当前会话的绑定（不删除已保存的历史文件） */
    private fun resetConversationState() {
        activeConversation = null
        _activeConversationId.value = null
        _conversationTitle.value = null
        _messages.value = emptyList()
        lastApiMessages = emptyList()
        cancelEditing()
        _inputText.value = ""
        _selectedFile.value = null
        viewModelScope.launch {
            persistenceMutex.withLock { settingsRepository.setActiveConversationId(null) }
        }
    }

    /** 生成历史列表标题：取第一条用户消息的第一行 */
    private fun deriveTitle(messages: List<Message>): String {
        val firstUser = messages.firstOrNull { it.sender == Sender.USER } ?: messages.first()
        val firstLine = firstUser.text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: firstUser.selectedFile?.fileName
            ?: ""
        val compact = firstLine.replace(WHITESPACE_REGEX, " ").trim()
        return if (compact.length > TITLE_MAX_LENGTH) {
            compact.take(TITLE_MAX_LENGTH).trimEnd() + "…"
        } else {
            compact
        }
    }

    /** 单调递增的消息 id，避免同一毫秒内创建的多个消息 id 冲突 */
    private fun nextMessageId(): Long {
        val now = System.currentTimeMillis()
        lastMessageId = if (now > lastMessageId) now else lastMessageId + 1
        return lastMessageId
    }

    /** 旧数据里可能存在重复 id，打开会话时补齐，避免 LazyColumn 的 key 冲突 */
    private fun ensureUniqueMessageIds(messages: List<Message>): List<Message> {
        val seen = HashSet<Long>(messages.size)
        var fallback = messages.maxOfOrNull { it.id } ?: 0L
        return messages.map { message ->
            if (seen.add(message.id)) {
                message
            } else {
                var candidate = fallback + 1
                while (!seen.add(candidate)) candidate++
                fallback = candidate
                message.copy(id = candidate)
            }
        }
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

    // ------------------- MCP 服务器配置逻辑 -------------------

    fun addOrUpdateMcpServer(server: McpServer) {
        viewModelScope.launch {
            settingsRepository.addOrUpdateMcpServer(server)
        }
    }

    fun deleteMcpServer(server: McpServer) {
        viewModelScope.launch {
            settingsRepository.deleteMcpServer(server)
        }
    }

    fun setMcpServerEnabled(serverId: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMcpServerEnabled(serverId, enabled)
        }
    }

    private companion object {
        /** 历史列表标题的最大长度 */
        const val TITLE_MAX_LENGTH = 40
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}

class ChatViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            val conversationRepository = ConversationRepository(context)
            val apiService = LlmApiService()
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(apiService, settingsRepository, conversationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
