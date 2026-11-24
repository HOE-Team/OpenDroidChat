/*
OpenDroidChat LLM API Service Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import com.hoeteam.opendroidchat.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.net.UnknownHostException

class LlmApiService {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
        encodeDefaults = false
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300000 // 5分钟超时
            connectTimeoutMillis = 60000  // 1分钟连接超时
            socketTimeoutMillis = 300000  // 5分钟套接字超时
        }
        expectSuccess = true
    }

    private fun getApiUrl(provider: LlmProvider, isStream: Boolean): String {
        return when (provider) {
            LlmProvider.OpenAI -> "https://api.openai.com/v1/chat/completions"
            LlmProvider.Gemini -> {
                val action = if (isStream) "streamGenerateContent" else "generateContent"
                "https://generativelanguage.googleapis.com/v1beta/models/MODEL_PLACEHOLDER:$action"
            }
            LlmProvider.DeepSeek -> "https://api.deepseek.com/v1/chat/completions"
            LlmProvider.Dashscope -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
            LlmProvider.Claude -> "https://api.anthropic.com/v1/messages"
            LlmProvider.Custom -> ""
        }
    }

    private val modelNamePlaceholder = "MODEL_PLACEHOLDER"

    /**
     * 判断是否是 Claude 最新版本（4.7+），强制使用自适应模式
     */
    private fun isClaude47Plus(modelName: String): Boolean {
        val lowerName = modelName.lowercase()
        return lowerName.contains("4-7") || lowerName.contains("4.7") ||
                lowerName.contains("opus-4-7") || lowerName.contains("sonnet-4-7")
    }

    /**
     * 判断是否是 Claude 4.6
     */
    private fun isClaude46(modelName: String): Boolean {
        val lowerName = modelName.lowercase()
        return (lowerName.contains("sonnet-4") || lowerName.contains("opus-4")) &&
                !isClaude47Plus(modelName) && !lowerName.contains("haiku")
    }

    /**
     * 构建 Claude thinking 配置
     */
    private fun buildClaudeThinkingConfig(config: LlmModel): ClaudeThinkingConfig? {
        if (!config.enableThinking) {
            return ClaudeThinkingConfig(type = "disabled") // 显式告知模型不要思考
        }
        val modelName = config.modelName

        return when {
            isClaude47Plus(modelName) -> {
                // Claude 4.7+ 必须使用自适应模式
                ClaudeThinkingConfig(
                    type = "adaptive",
                    display = "summarized" // 返回思考内容摘要
                )
            }
            isClaude46(modelName) -> {
                // Claude 4.6 推荐使用自适应模式
                ClaudeThinkingConfig(
                    type = "adaptive"
                )
            }
            else -> {
                // Claude 4/4.1/4.5/Haiku 4.5 使用固定预算模式
                ClaudeThinkingConfig(
                    type = "enabled",
                    budgetTokens = when {
                        modelName.contains("haiku", ignoreCase = true) -> 2048
                        modelName.contains("sonnet", ignoreCase = true) -> 4096
                        else -> 8192 // Claude 4/4.1 等
                    }
                )
            }
        }
    }

    // 流式传输
    fun sendChatStream(
        messages: List<ApiMessage>,
        config: LlmModel,
        extraSystemPrompt: String = ""
    ): Flow<StreamChunk> = flow {
        var apiUrl = config.customApiUrl.takeIf { it?.isNotBlank() == true } ?: getApiUrl(config.provider, true)
        val apiKey = config.apiKey

        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        if (config.provider == LlmProvider.Gemini) {
            apiUrl = apiUrl.replace(modelNamePlaceholder, config.modelName)
        }

        // 构建系统提示（包含基础提示词、系统当前时间、附加提示如 MCP 工具说明）
        val systemText = buildSystemPrompt(config.systemPrompt, extraSystemPrompt)
        val systemMessage = ApiMessage(role = "system", content = systemText)
        val fullMessages = listOf(systemMessage) + messages

        if (config.provider == LlmProvider.Claude) {
            // ===== Claude 流式请求 =====
            val claudeMessages = fullMessages.map { ClaudeApiMessage(role = it.role, content = it.content) }
            val claudeSystemText = systemText.ifBlank { null }
            val claudeThinking = buildClaudeThinkingConfig(config)

            val requestBody = ClaudeChatRequest(
                model = config.modelName,
                messages = claudeMessages.filterNot { it.role == "system" },
                stream = true,
                system = claudeSystemText,
                thinking = claudeThinking,
                outputConfig = if (claudeThinking?.type == "adaptive" && config.reasoningEffort != null) {
                    ClaudeOutputConfig(effort = config.reasoningEffort)
                } else null
            )

            try {
                client.preparePost(apiUrl) {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    // 工具调用场景：如需 Interleaved Thinking，添加 Beta 头
                    if (config.enableThinking && (isClaude46(config.modelName) || isClaude47Plus(config.modelName))) {
                        header("anthropic-beta", "interleaved-thinking-2025-05-14")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    var currentThinking = ""
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: continue
                        if (line.isEmpty()) continue

                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()
                            if (data == "[DONE]") break
                            try {
                                val event = json.decodeFromString<ClaudeStreamEvent>(data)
                                when (event.type) {
                                    "content_block_start" -> {
                                        val block = event.content_block
                                        if (block?.type == "thinking") {
                                            currentThinking = block.thinking ?: ""
                                            emit(StreamChunk(reasoningText = currentThinking))
                                        }
                                    }
                                    "content_block_delta" -> {
                                        val delta = event.delta
                                        when (delta?.type) {
                                            "text_delta" -> {
                                                emit(StreamChunk(content = delta.text ?: ""))
                                            }
                                            "thinking_delta" -> {
                                                currentThinking += (delta.thinking ?: "")
                                                emit(StreamChunk(reasoningText = currentThinking))
                                            }
                                        }
                                    }
                                    "message_delta" -> {
                                        // 可以处理 usage 等信息
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip lines that aren't valid JSON
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        } else if (config.provider == LlmProvider.Gemini) {
            // ===== Gemini 流式请求 =====
            val geminiContents = fullMessages.map { msg ->
                buildJsonObject {
                    put("role", JsonPrimitive(msg.role))
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", JsonPrimitive(msg.content))
                        })
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("contents", JsonArray(geminiContents))
                // Gemini 必须显式传递 thinkingConfig，关闭时也需传 budget: 0 禁用
                put("thinkingConfig", buildJsonObject {
                    if (config.enableThinking) {
                        put("thinkingBudget", JsonPrimitive(-1)) // -1 表示无限预算
                    } else {
                        put("thinkingBudget", JsonPrimitive(0))  // 0 表示禁用思考
                    }
                })
            }

            try {
                client.preparePost(apiUrl) {
                    parameter("key", apiKey)
                    headers { remove(HttpHeaders.Authorization) }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: continue
                        if (line.isEmpty()) continue

                        try {
                            val cleanLine = line.trim().removePrefix("[").removeSuffix(",").removeSuffix("]")
                            if (cleanLine.isNotEmpty()) {
                                val geminiResp = json.decodeFromString<GeminiResponse>(cleanLine)
                                geminiResp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.let {
                                    emit(StreamChunk(content = it))
                                }
                            }
                        } catch (e: Exception) {
                            // Skip
                        }
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        } else {
            // ===== OpenAI 格式流式请求 (OpenAI/DeepSeek/Dashscope/Custom) =====
            val requestBody = buildOpenAiChatRequest(config, fullMessages, stream = true)

            try {
                val httpRequestBuilder: HttpRequestBuilder.() -> Unit = {
                    header("Authorization", "Bearer $apiKey")
                    when (config.provider) {
                        LlmProvider.Dashscope -> {
                            config.appId?.takeIf { it.isNotBlank() }?.let {
                                header("X-DashScope-Appid", it)
                            }
                        }
                        else -> {}
                    }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                client.preparePost(apiUrl) {
                    httpRequestBuilder()
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    var reasoningBuffer = ""
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: continue
                        if (line.isEmpty()) continue

                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()
                            if (data == "[DONE]") break
                            try {
                                val chatResp = json.decodeFromString<ChatResponse>(data)
                                val delta = chatResp.choices.firstOrNull()?.delta

                                // 提取思考内容 (DeepSeek/Qwen 使用 reasoning_content)
                                if (delta?.reasoning_content != null) {
                                    reasoningBuffer += delta.reasoning_content
                                    emit(StreamChunk(reasoningText = reasoningBuffer))
                                }

                                // 提取正常内容
                                delta?.content?.let {
                                    emit(StreamChunk(content = it))
                                }
                            } catch (e: Exception) {
                                // Skip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * 返回系统当前时间（含时区），用于让模型感知实时时间。
     */
    private fun currentTimeString(): String {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(now) + "（" + java.util.TimeZone.getDefault().id + "）"
    }

    /**
     * 构建系统提示：基础提示词 + 当前时间 + 附加提示（如 MCP 工具说明）。
     */
    private fun buildSystemPrompt(basePrompt: String, extraPrompt: String): String {
        val timeInfo = "系统当前时间：" + currentTimeString()
        return listOfNotNull(
            basePrompt.ifBlank { null },
            timeInfo,
            extraPrompt.ifBlank { null }
        ).joinToString("\n\n")
    }

    // 非流式传输
    suspend fun sendChat(
        messages: List<ApiMessage>,
        config: LlmModel,
        extraSystemPrompt: String = ""
    ): ChatResult {
        var apiUrl = config.customApiUrl.takeIf { it?.isNotBlank() == true } ?: getApiUrl(config.provider, false)
        val apiKey = config.apiKey

        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        if (config.provider == LlmProvider.Gemini) {
            apiUrl = apiUrl.replace(modelNamePlaceholder, config.modelName)
        }

        // 构建系统提示（包含基础提示词、系统当前时间、附加提示如 MCP 工具说明）
        val systemText = buildSystemPrompt(config.systemPrompt, extraSystemPrompt)
        val systemMessage = ApiMessage(role = "system", content = systemText)
        val fullMessages = listOf(systemMessage) + messages

        if (config.provider == LlmProvider.Claude) {
            return sendClaudeChat(apiUrl, apiKey, config, fullMessages, systemText)
        } else if (config.provider == LlmProvider.Gemini) {
            return sendGeminiChat(apiUrl, apiKey, config, fullMessages)
        } else {
            return sendOpenAiChat(apiUrl, apiKey, config, fullMessages)
        }
    }

    /**
     * DSML 只给出工具名，有时会带上服务器前缀（如 AnySearch__extract）。
     * 依次尝试完全匹配、后缀匹配，找到第一个暴露该工具的服务器。
     */
    private fun findServerForTool(
        usableServers: List<Pair<McpServer, List<McpTool>>>,
        toolName: String
    ): McpServer? {
        val bareName = toolName
            .substringAfterLast("__")
            .substringAfterLast("::")
            .substringAfterLast(':')
            .substringAfterLast('/')
            .substringAfterLast('.')
        return usableServers.firstOrNull { (_, tools) ->
            tools.any { it.name == toolName || it.name == bareName }
        }?.first
    }

    private data class ResolvedMcpCall(
        val server: McpServer?,
        val serverName: String,
        val tool: String,
        val arguments: Map<String, JsonElement>
    )

    /**
     * 支持 MCP 工具调用的聊天。
     *
     * 流程：
     *  1. 拉取所有已启用服务器的工具列表并注入系统提示。
     *  2. 解析模型输出中的 [MCP_CALL] 或 DeepSeek DSML 工具调用。
     *  3. 执行工具后，把「模型本轮的可见文本」作为 assistant、「工具结果」作为 user 回填。
     *  4. 继续下一轮，直到模型不再请求工具调用（或达到最大迭代次数）。
     */
    suspend fun sendChatWithMcp(
        messages: List<ApiMessage>,
        config: LlmModel,
        mcpServers: List<McpServer>,
        stream: Boolean = false,
        onChunk: (suspend (content: String, reasoningText: String?) -> Unit)? = null,
        onToolStart: (suspend (serverName: String, toolName: String) -> Unit)? = null,
        onToolComplete: (suspend (completed: McpCallInfo) -> Unit)? = null
    ): ChatResult {
        val mcpClient = McpClient()
        val toolPrompt = StringBuilder()
        val usableServers = mutableListOf<Pair<McpServer, List<McpTool>>>()

        for (server in mcpServers) {
            val tools = mcpClient.listTools(server)
            if (tools.isEmpty()) continue
            usableServers += server to tools
            toolPrompt.append("MCP 服务器 \"${server.name}\" 提供以下工具：\n")
            tools.forEach { tool ->
                toolPrompt.append("  - 工具名: ${tool.name}\n")
                if (tool.description.isNotBlank()) {
                    toolPrompt.append("    描述: ${tool.description}\n")
                }
                if (!tool.inputSchema.isNullOrBlank()) {
                    toolPrompt.append("    参数 (JSON Schema): ${tool.inputSchema}\n")
                }
            }
        }

        if (usableServers.isEmpty()) {
            // 没有可用工具，退化为普通聊天
            return sendChat(messages, config)
        }

        toolPrompt.append("\n当需要调用上述工具时，必须输出工具调用标记后停止，等待工具结果：\n")
        toolPrompt.append("[MCP_CALL]{\"server\":\"<服务器名>\",\"tool\":\"<工具名>\",\"arguments\":{...}}\n")
        toolPrompt.append("收到工具结果后，再基于结果继续回答用户的问题。\n")

        val collectedCalls = mutableListOf<McpCallInfo>()
        val workingMessages = messages.toMutableList()
        val maxIterations = 6
        var nudgedAfterEmptyReply = false

        repeat(maxIterations) {
            var roundText: String
            var reasoning: String? = null

            if (stream && onChunk != null) {
                // === 真·流式：边生成边推送，工具调用标记会被实时剔除 ===
                val roundContent = StringBuilder()
                var pending = StringBuilder()
                sendChatStream(workingMessages, config, extraSystemPrompt = toolPrompt.toString())
                    .collect { chunk ->
                        if (!chunk.reasoningText.isNullOrBlank()) {
                            val cleanedReasoning = ToolCallMarkup.stripMarkup(chunk.reasoningText)
                            reasoning = cleanedReasoning
                            onChunk("", cleanedReasoning)
                        }
                        roundContent.append(chunk.content)
                        pending.append(chunk.content)

                        val (visible, rest) = ToolCallMarkup.splitStreaming(pending.toString())
                        if (visible.isNotEmpty()) onChunk(visible, null)
                        pending = StringBuilder(rest)
                    }
                // 冲刷尾部（非标记文本）
                if (pending.isNotBlank()) {
                    val leftover = ToolCallMarkup.stripMarkup(pending.toString())
                    if (leftover.isNotBlank()) onChunk(leftover, null)
                }
                roundText = roundContent.toString()
            } else {
                // === 非流式：单次调用获取本轮文本 ===
                val result = sendChat(workingMessages, config, extraSystemPrompt = toolPrompt.toString())
                roundText = result.content
                reasoning = result.reasoningText?.let { ToolCallMarkup.stripMarkup(it) }
            }

            // 同时兼容本应用协议与 DeepSeek 的 DSML 标记
            val calls = mutableListOf<ResolvedMcpCall>()
            for (call in ToolCallMarkup.parseMcpCalls(roundText)) {
                val server = usableServers.firstOrNull { it.first.name == call.server }?.first
                calls += ResolvedMcpCall(
                    server = server,
                    serverName = server?.name ?: call.server,
                    tool = call.tool,
                    arguments = call.arguments
                )
            }
            for (call in ToolCallMarkup.parseDsmlCalls(roundText)) {
                val server = findServerForTool(usableServers, call.tool)
                calls += ResolvedMcpCall(
                    server = server,
                    serverName = server?.name ?: "未知服务器",
                    tool = call.tool,
                    arguments = call.arguments
                )
            }

            if (calls.isEmpty()) {
                // 没有工具调用，本轮文本就是最终答案
                val visible = ToolCallMarkup.stripMarkup(roundText)
                // 模型只复述了工具结果或标记、正文为空时，再提示一次，
                // 避免“工具调用完成后回复停住”的情况
                if (visible.isBlank() && collectedCalls.isNotEmpty() && !nudgedAfterEmptyReply) {
                    nudgedAfterEmptyReply = true
                    workingMessages += ApiMessage(
                        role = "user",
                        content = "请直接回答用户的问题，不要复述工具返回的原始数据。"
                    )
                    return@repeat
                }
                return ChatResult(
                    content = visible,
                    reasoningText = reasoning,
                    mcpCalls = collectedCalls
                )
            }

            // 把模型本轮的可见文本作为 assistant 消息（不伪造“等待结果”之类的话术），
            // 工具结果放进 user 消息，避免模型把占位文案当成自己的最终回答。
            val assistantText = ToolCallMarkup.stripMarkup(roundText)
            workingMessages += ApiMessage(
                role = "assistant",
                // 只输出工具标记时用空格占位：保证 user/assistant 交替，
                // 又不会像合成文案那样被模型复读
                content = assistantText.ifBlank { " " }
            )

            val resultText = StringBuilder()
            for (call in calls) {
                onToolStart?.invoke(call.serverName, call.tool)
                val server = call.server
                if (server == null) {
                    val error = "未找到提供工具 \"${call.tool}\" 的 MCP 服务器"
                    val completed = McpCallInfo(
                        serverName = call.serverName,
                        toolName = call.tool,
                        durationMs = 0,
                        error = error
                    )
                    collectedCalls += completed
                    onToolComplete?.invoke(completed)
                    resultText.append("工具 \"${call.tool}\" 执行失败：${error}\n\n")
                    continue
                }

                val startTime = System.currentTimeMillis()
                val result = mcpClient.callTool(server, call.tool, call.arguments)
                val duration = System.currentTimeMillis() - startTime
                val completed = McpCallInfo(
                    serverName = server.name,
                    toolName = call.tool,
                    durationMs = duration,
                    error = if (result.isError) result.text else null
                )
                collectedCalls += completed
                onToolComplete?.invoke(completed)
                resultText.append("工具 \"${call.tool}\"（服务器 ${server.name}）返回结果：\n${result.text}\n\n")
            }

            // DeepSeek 的 DSML 协议用 <tool_result> 承载结果；按这个格式回填，
            // 模型更不容易自己伪造一份结果再停住
            val resultPayload = if (config.provider == LlmProvider.DeepSeek) {
                "<tool_result>\n${resultText}</tool_result>"
            } else {
                resultText.toString()
            }
            workingMessages += ApiMessage(
                role = "user",
                content = "你刚才请求调用 MCP 工具：${calls.joinToString("、") { it.tool }}。\n\n" +
                    "工具执行结果如下：\n\n${resultPayload}" +
                    "请基于以上结果继续回答用户的问题，不要重复工具调用说明，除非还需要调用其他工具。"
            )
        }

        // 达到最大迭代次数仍无法收敛，返回最后一次结果
        val last = sendChat(workingMessages, config, extraSystemPrompt = toolPrompt.toString())
        return ChatResult(
            content = ToolCallMarkup.stripMarkup(last.content),
            reasoningText = last.reasoningText?.let { ToolCallMarkup.stripMarkup(it) },
            mcpCalls = collectedCalls
        )
    }

    // OpenAI 格式的非流式请求
    private suspend fun sendOpenAiChat(
        apiUrl: String,
        apiKey: String,
        config: LlmModel,
        fullMessages: List<ApiMessage>
    ): ChatResult {
        val requestBody = buildOpenAiChatRequest(config, fullMessages, stream = false)

        try {
            val response: ChatResponse = client.post(apiUrl) {
                header("Authorization", "Bearer $apiKey")
                when (config.provider) {
                    LlmProvider.Dashscope -> {
                        config.appId?.takeIf { it.isNotBlank() }?.let {
                            header("X-DashScope-Appid", it)
                        }
                    }
                    else -> {}
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val message = response.choices.firstOrNull()?.message
            val content = message?.content ?: "未能获取到 LLM API 响应。"
            val reasoningContent = message?.reasoning_content

            return ChatResult(
                content = content,
                reasoningText = reasoningContent
            )
        } catch (e: Exception) {
            throw e
        }
    }

    // Gemini 非流式请求
    private suspend fun sendGeminiChat(
        apiUrl: String,
        apiKey: String,
        config: LlmModel,
        fullMessages: List<ApiMessage>
    ): ChatResult {
        val geminiContents = fullMessages.map { msg ->
            buildJsonObject {
                put("role", JsonPrimitive(msg.role))
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("text", JsonPrimitive(msg.content))
                    })
                })
            }
        }

        val requestBody = buildJsonObject {
            put("contents", JsonArray(geminiContents))
            // Gemini 必须显式传递 thinkingConfig，关闭时也需传 budget: 0 禁用
            put("thinkingConfig", buildJsonObject {
                if (config.enableThinking) {
                    put("thinkingBudget", JsonPrimitive(-1)) // -1 表示无限预算
                } else {
                    put("thinkingBudget", JsonPrimitive(0))  // 0 表示禁用思考
                }
            })
        }

        try {
            val response: HttpResponse = client.post(apiUrl) {
                parameter("key", apiKey)
                headers { remove(HttpHeaders.Authorization) }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val bodyText = response.bodyAsText()
            val geminiResp = try {
                json.decodeFromString<GeminiResponse>(bodyText)
            } catch (e: Exception) {
                // 如果 GeminiResponse 反序列化失败，尝试从完整响应中提取文本
                val fullJson = json.decodeFromString<JsonObject>(bodyText)
                val candidates = fullJson["candidates"]?.jsonArray
                val content = candidates?.firstOrNull()?.jsonObject?.get("content")?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                return ChatResult(content = text ?: "未能获取到 Gemini API 响应。")
            }

            val text = geminiResp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "未能获取到 Gemini API 响应。"
            return ChatResult(content = text)
        } catch (e: Exception) {
            throw e
        }
    }

    // Claude 非流式请求
    private suspend fun sendClaudeChat(
        apiUrl: String,
        apiKey: String,
        config: LlmModel,
        fullMessages: List<ApiMessage>,
        systemTextOverride: String = ""
    ): ChatResult {
        val claudeMessages = fullMessages.map { ClaudeApiMessage(role = it.role, content = it.content) }
        val systemText = (systemTextOverride.ifBlank { config.systemPrompt }).ifBlank { null }
        val claudeThinking = buildClaudeThinkingConfig(config)

        val requestBody = ClaudeChatRequest(
            model = config.modelName,
            messages = claudeMessages.filterNot { it.role == "system" },
            stream = false,
            system = systemText,
            thinking = claudeThinking,
            outputConfig = if (claudeThinking?.type == "adaptive" && config.reasoningEffort != null) {
                ClaudeOutputConfig(effort = config.reasoningEffort)
            } else null
        )

        try {
            val responseJson: JsonObject = client.post(apiUrl) {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            // 解析 Claude 响应
            val contentArray = responseJson["content"]?.jsonArray
            val textBuilder = StringBuilder()
            val reasoningBuilder = StringBuilder()

            contentArray?.forEach { element ->
                val obj = element.jsonObject
                val type = obj["type"]?.jsonPrimitive?.contentOrNull
                when (type) {
                    "text" -> {
                        textBuilder.append(obj["text"]?.jsonPrimitive?.contentOrNull ?: "")
                    }
                    "thinking" -> {
                        reasoningBuilder.append(obj["thinking"]?.jsonPrimitive?.contentOrNull ?: "")
                    }
                }
            }

            return ChatResult(
                content = textBuilder.toString().ifEmpty { "未能获取到 Claude API 响应。" },
                reasoningText = reasoningBuilder.toString().ifEmpty { null }
            )
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 构建 OpenAI 格式的 ChatRequest，包含 thinking 参数
     * 所有支持思考的模型都必须显式传递 thinking 启用/禁用参数，
     * 否则模型可能会默认启用思考，浪费 token。
     *
     * 各平台思考模式参数对照：
     * - DeepSeek: thinking={"type": "enabled/disabled"}, reasoning_effort="high"/"max"
     * - OpenAI (o-series, GPT-5): reasoning_effort="low"/"medium"/"high"
     * - Azure OpenAI: 与 OpenAI 一致
     */
    private fun buildOpenAiChatRequest(
        config: LlmModel,
        fullMessages: List<ApiMessage>,
        stream: Boolean
    ): ChatRequest {
        val thinkingEnabled = config.enableThinking
        return ChatRequest(
            model = config.modelName,
            messages = fullMessages,
            stream = stream,
            thinking = if (config.provider == LlmProvider.DeepSeek) {
                // DeepSeek 必须显式传递 thinking 开关，否则模型可能默认启用思考
                buildJsonObject {
                    put("type", JsonPrimitive(if (thinkingEnabled) "enabled" else "disabled"))
                }
            } else null,
            enable_thinking = null,
            reasoning_effort = if (thinkingEnabled && config.reasoningEffort != null) {
                when (config.provider) {
                    LlmProvider.DeepSeek -> config.reasoningEffort  // "high" / "max"
                    LlmProvider.OpenAI -> config.reasoningEffort    // "low" / "medium" / "high"
                    else -> null
                }
            } else null
        )
    }
}

/**
 * 流式响应的数据块
 */
data class StreamChunk(
    val content: String = "",
    val reasoningText: String? = null
)

/**
 * 聊天请求结果
 */
data class ChatResult(
    val content: String,
    val reasoningText: String? = null,
    val mcpCalls: List<McpCallInfo> = emptyList() // 本次回复中调用的 MCP 工具记录
)

/**
 * Claude 流式事件（用于 JSON 解析）
 */
@kotlinx.serialization.Serializable
data class ClaudeStreamEvent(
    val type: String,
    val content_block: ClaudeContentBlock? = null,
    val delta: ClaudeStreamDelta? = null
)

@kotlinx.serialization.Serializable
data class ClaudeContentBlock(
    val type: String? = null,
    val thinking: String? = null,
    val text: String? = null
)

@kotlinx.serialization.Serializable
data class ClaudeStreamDelta(
    val type: String? = null,
    val text: String? = null,
    val thinking: String? = null
)

