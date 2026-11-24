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

/**
 * 统一的 OpenAI 兼容客户端。
 *
 * 所有可用提供商（OpenAI/Azure、DeepSeek、Dashscope、Gemini OpenAI 兼容端点、
 * 自定义端点）都使用同一套 /chat/completions 请求、SSE 流式解析和标准 tool calling。
 * 不再维护 Claude/Gemini 的原生协议分支。
 */
class LlmApiService {
    companion object {
        /** 流式增量合并窗口：约 60ms 刷新一次，约合 16 帧，肉眼仍是逐字效果。 */
        const val STREAM_THROTTLE_MS = 60L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
        encodeDefaults = false
        coerceInputValues = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 300000
        }
        // 手动检查状态码，便于把服务端错误正文带给用户
        expectSuccess = false
    }

    private fun getApiUrl(provider: LlmProvider): String = when (provider) {
        LlmProvider.OpenAI -> "https://api.openai.com/v1/chat/completions"
        LlmProvider.Gemini -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
        LlmProvider.DeepSeek -> "https://api.deepseek.com/v1/chat/completions"
        LlmProvider.Dashscope -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        LlmProvider.Claude -> ""
        LlmProvider.Custom -> ""
    }

    /** 解析最终请求地址：自定义地址优先，并统一补齐 /v1/chat/completions。 */
    private fun resolveApiUrl(config: LlmModel): String {
        if (config.provider == LlmProvider.Claude) {
            throw IllegalStateException("Claude 原生协议已移除，请删除该模型或改用自定义 OpenAI 兼容端点。")
        }
        val custom = config.customApiUrl?.trim().orEmpty()
        if (custom.isNotEmpty()) return normalizeCustomUrl(custom)
        return getApiUrl(config.provider)
    }

    internal fun normalizeCustomUrl(raw: String): String {
        val trimmed = raw.trim()
        val queryIndex = trimmed.indexOf('?')
        val base = (if (queryIndex >= 0) trimmed.substring(0, queryIndex) else trimmed).trimEnd('/')
        val query = if (queryIndex >= 0) trimmed.substring(queryIndex) else ""
        val normalizedBase = when {
            base.endsWith("/chat/completions") -> base
            base.endsWith("/v1") -> base + "/chat/completions"
            base.endsWith("/openai") -> base + "/chat/completions"
            else -> base + "/v1/chat/completions"
        }
        return normalizedBase + query
    }

    private fun HttpRequestBuilder.applyOpenAiHeaders(
        config: LlmModel,
        apiKey: String,
        apiUrl: String
    ) {
        // Azure OpenAI 使用 api-key 头；部署地址通常还带 api-version 查询参数
        val isAzure = apiUrl.contains("openai.azure.com", ignoreCase = true) ||
            apiUrl.contains("api-version=", ignoreCase = true)
        if (isAzure) {
            header("api-key", apiKey)
        } else {
            header("Authorization", "Bearer " + apiKey)
        }
        if (config.provider == LlmProvider.Dashscope) {
            config.appId?.takeIf { it.isNotBlank() }?.let {
                header("X-DashScope-Appid", it)
            }
        }
        contentType(ContentType.Application.Json)
    }

    // ---------------------------- 流式传输 ----------------------------

    /**
     * 流式发送聊天请求。
     *
     * [throttleMillis] 用于合并高频增量：上游一个 token 一个 chunk，
     * 若逐条刷新 UI 会持续触发 Markdown 重排导致卡顿，这里按时间窗口合并，
     * 只把累计后的新增文本推给上层。
     */
    fun sendChatStream(
        messages: List<ApiMessage>,
        config: LlmModel,
        extraSystemPrompt: String = "",
        tools: List<ToolDefinition>? = null,
        throttleMillis: Long = 0L
    ): Flow<StreamChunk> = flow {
        val apiUrl = resolveApiUrl(config)
        val apiKey = config.apiKey
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        val systemText = buildSystemPrompt(config.systemPrompt, extraSystemPrompt)
        val fullMessages = listOf(ApiMessage(role = "system", content = systemText)) + messages
        val requestBody = buildOpenAiChatRequest(config, fullMessages, stream = true, tools = tools)

        client.preparePost(apiUrl) {
            applyOpenAiHeaders(config, apiKey, apiUrl)
            setBody(requestBody)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
                throw IllegalStateException(
                    "LLM API 请求失败（HTTP " + response.status.value + "）" +
                        if (errorBody.isBlank()) "" else "：" + errorBody.take(1000)
                )
            }

            val channel = response.bodyAsChannel()
            var reasoningBuffer = ""
            // 与上次推送的内容比较长度，只推送新增部分
            var emittedReasoningLength = 0
            var emittedContentLength = 0
            val contentBuffer = StringBuilder()
            val toolCallAccumulators = sortedMapOf<Int, ToolCallAccumulator>()
            var lastFlushAt = 0L

            suspend fun flush(force: Boolean) {
                val now = System.currentTimeMillis()
                if (!force && now - lastFlushAt < throttleMillis) return
                lastFlushAt = now

                if (reasoningBuffer.length > emittedReasoningLength) {
                    emit(StreamChunk(reasoningText = reasoningBuffer))
                    emittedReasoningLength = reasoningBuffer.length
                }
                if (contentBuffer.length > emittedContentLength) {
                    emit(StreamChunk(content = contentBuffer.substring(emittedContentLength)))
                    emittedContentLength = contentBuffer.length
                }
            }

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue

                val chatResp = runCatching {
                    json.decodeFromString<ChatResponse>(data)
                }.getOrNull() ?: continue

                val choice = chatResp.choices.firstOrNull() ?: continue
                val delta = choice.delta

                delta?.reasoning_content?.takeIf { it.isNotEmpty() }?.let {
                    reasoningBuffer += it
                }
                delta?.content?.takeIf { it.isNotEmpty() }?.let {
                    contentBuffer.append(it)
                }
                delta?.tool_calls?.forEach { toolDelta ->
                    toolCallAccumulators
                        .getOrPut(toolDelta.index) { ToolCallAccumulator() }
                        .append(toolDelta)
                }

                flush(force = false)
            }

            // 冲刷剩余内容，保证最后几个字不丢
            flush(force = true)

            if (toolCallAccumulators.isNotEmpty()) {
                emit(
                    StreamChunk(
                        toolCalls = toolCallAccumulators.map { (index, acc) -> acc.build(index) }
                    )
                )
            }
        }
    }

    // ---------------------------- 非流式传输 ----------------------------

    suspend fun sendChat(
        messages: List<ApiMessage>,
        config: LlmModel,
        extraSystemPrompt: String = "",
        tools: List<ToolDefinition>? = null
    ): ChatResult {
        val apiUrl = resolveApiUrl(config)
        val apiKey = config.apiKey
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        val systemText = buildSystemPrompt(config.systemPrompt, extraSystemPrompt)
        val fullMessages = listOf(ApiMessage(role = "system", content = systemText)) + messages
        val requestBody = buildOpenAiChatRequest(config, fullMessages, stream = false, tools = tools)

        val response = client.post(apiUrl) {
            applyOpenAiHeaders(config, apiKey, apiUrl)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            throw IllegalStateException(
                "LLM API 请求失败（HTTP " + response.status.value + "）" +
                    if (errorBody.isBlank()) "" else "：" + errorBody.take(1000)
            )
        }

        val chatResponse: ChatResponse = response.body()
        val message = chatResponse.choices.firstOrNull()?.message
        val toolCalls = message?.tool_calls.orEmpty()
        val content = message?.content.orEmpty()
        return ChatResult(
            content = if (content.isBlank() && toolCalls.isEmpty()) {
                "未能获取到 LLM API 响应。"
            } else {
                content
            },
            reasoningText = message?.reasoning_content,
            toolCalls = toolCalls
        )
    }

    private fun currentTimeString(): String {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(now) + "（" + java.util.TimeZone.getDefault().id + "）"
    }

    private fun buildSystemPrompt(basePrompt: String, extraPrompt: String): String {
        val timeInfo = "系统当前时间：" + currentTimeString()
        return listOfNotNull(
            basePrompt.ifBlank { null },
            timeInfo,
            extraPrompt.ifBlank { null }
        ).joinToString("\n\n")
    }

    /**
     * 构建 OpenAI 请求体。
     *
     * 思考参数只对明确支持的提供商发送：
     * - DeepSeek: thinking={"type": "enabled/disabled"} + reasoning_effort
     * - OpenAI: reasoning_effort
     * 其它兼容端点（包括 Gemini 的 OpenAI 端点）不发送未验证的私有字段。
     */
    private fun buildOpenAiChatRequest(
        config: LlmModel,
        fullMessages: List<ApiMessage>,
        stream: Boolean,
        tools: List<ToolDefinition>? = null
    ): ChatRequest {
        val thinkingEnabled = config.enableThinking
        return ChatRequest(
            model = config.modelName,
            messages = fullMessages,
            stream = stream,
            thinking = if (config.provider == LlmProvider.DeepSeek) {
                buildJsonObject {
                    put("type", JsonPrimitive(if (thinkingEnabled) "enabled" else "disabled"))
                }
            } else null,
            enable_thinking = null,
            reasoning_effort = if (thinkingEnabled && config.reasoningEffort != null) {
                when (config.provider) {
                    LlmProvider.DeepSeek, LlmProvider.OpenAI -> config.reasoningEffort
                    else -> null
                }
            } else null,
            tools = tools?.takeIf { it.isNotEmpty() }
        )
    }

    // ---------------------------- MCP 工具调用 ----------------------------

    internal data class ExposedTool(
        val definition: ToolDefinition,
        val server: McpServer,
        val tool: McpTool,
        val exposedName: String
    )

    internal fun buildExposedTools(
        usableServers: List<Pair<McpServer, List<McpTool>>>
    ): List<ExposedTool> {
        val usedNames = mutableSetOf<String>()
        val result = mutableListOf<ExposedTool>()

        usableServers.forEach { (server, tools) ->
            tools.forEach { tool ->
                val base = sanitizeToolName(server.name + "__" + tool.name)
                var name = base
                var suffix = 2
                while (!usedNames.add(name)) {
                    val suffixText = "_" + suffix
                    name = base.take((64 - suffixText.length).coerceAtLeast(1)) + suffixText
                    suffix++
                }
                result += ExposedTool(
                    definition = ToolDefinition(
                        type = "function",
                        function = ToolFunctionDefinition(
                            name = name,
                            description = tool.description.takeIf { it.isNotBlank() },
                            parameters = parseToolParameters(tool.inputSchema)
                        )
                    ),
                    server = server,
                    tool = tool,
                    exposedName = name
                )
            }
        }
        return result
    }

    private fun sanitizeToolName(raw: String): String {
        // OpenAI 工具名只允许 ASCII 字母、数字、下划线和连字符
        val sanitized = raw.map { c ->
            if ((c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c == '_' || c == '-') {
                c
            } else {
                '_'
            }
        }.joinToString("")
        return sanitized.ifBlank { "mcp_tool" }.take(64)
    }

    private fun parseToolParameters(schema: String?): JsonElement {
        val fallback = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
        }
        if (schema.isNullOrBlank()) return fallback
        return runCatching {
            val parsed = json.parseToJsonElement(schema)
            if (parsed is JsonObject) parsed else fallback
        }.getOrElse { fallback }
    }

    private fun parseToolArguments(raw: String): Map<String, JsonElement>? {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            json.parseToJsonElement(raw).jsonObject.mapValues { it.value }
        }.getOrNull()
    }

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
     * 优先使用 OpenAI 标准 tool calling：
     *   assistant(tool_calls) -> tool(tool_call_id) -> 下一轮
     * 如果模型仍输出文本协议（[MCP_CALL] / DSML），保留原有兜底解析。
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
        val usableServers = mutableListOf<Pair<McpServer, List<McpTool>>>()
        for (server in mcpServers) {
            val tools = mcpClient.listTools(server)
            if (tools.isNotEmpty()) usableServers += server to tools
        }

        if (usableServers.isEmpty()) {
            return sendChat(messages, config)
        }

        val exposedTools = buildExposedTools(usableServers)
        val toolDefinitions = exposedTools.map { it.definition }
        val toolsByName = exposedTools.associateBy { it.exposedName }
        val toolHint = buildString {
            append("你可以使用以下 MCP 工具；优先通过 tools 字段调用，")
            append("若当前接口不支持 tools，可输出一行 ")
            append("[MCP_CALL]{\"server\":\"<服务器名>\",\"tool\":\"<工具名>\",\"arguments\":{...}}。\n")
            exposedTools.forEach { exposed ->
                append("- ").append(exposed.server.name).append("/").append(exposed.tool.name)
                if (exposed.tool.description.isNotBlank()) {
                    append("：").append(exposed.tool.description)
                }
                append("\n")
                if (!exposed.tool.inputSchema.isNullOrBlank()) {
                    append("  参数: ").append(exposed.tool.inputSchema).append("\n")
                }
            }
        }

        val collectedCalls = mutableListOf<McpCallInfo>()
        val workingMessages = messages.toMutableList()
        val maxIterations = 6
        var nudgedAfterEmptyReply = false

        repeat(maxIterations) { roundIndex ->
            var roundText: String = ""
            var reasoning: String? = null
            var nativeToolCalls: List<ApiToolCall> = emptyList()

            if (stream && onChunk != null) {
                val roundContent = StringBuilder()
                var pending = StringBuilder()
                sendChatStream(
                    messages = workingMessages,
                    config = config,
                    extraSystemPrompt = toolHint,
                    tools = toolDefinitions,
                    throttleMillis = STREAM_THROTTLE_MS
                ).collect { chunk ->
                    chunk.toolCalls?.let { nativeToolCalls = it }
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
                if (pending.isNotBlank()) {
                    val leftover = ToolCallMarkup.stripMarkup(pending.toString())
                    if (leftover.isNotBlank()) onChunk(leftover, null)
                }
                roundText = roundContent.toString()
            } else {
                val result = sendChat(
                    messages = workingMessages,
                    config = config,
                    extraSystemPrompt = toolHint,
                    tools = toolDefinitions
                )
                roundText = result.content
                reasoning = result.reasoningText?.let { ToolCallMarkup.stripMarkup(it) }
                nativeToolCalls = result.toolCalls
            }

            // ---------- 1) 标准 OpenAI tool calling ----------
            if (nativeToolCalls.isNotEmpty()) {
                val normalizedCalls = nativeToolCalls.mapIndexed { index, call ->
                    if (call.id.isBlank()) call.copy(id = "call_" + roundIndex + "_" + index) else call
                }
                val visibleText = ToolCallMarkup.stripMarkup(roundText)
                workingMessages += ApiMessage(
                    role = "assistant",
                    content = visibleText.ifBlank { null },
                    // DeepSeek 思考模式下，带 tool_calls 的 assistant 消息需要回传思考内容
                    reasoning_content = if (
                        config.provider == LlmProvider.DeepSeek && config.enableThinking
                    ) {
                        reasoning
                    } else {
                        null
                    },
                    tool_calls = normalizedCalls
                )

                normalizedCalls.forEachIndexed { index, call ->
                    val exposed = toolsByName[call.function.name]
                    val serverName = exposed?.server?.name ?: "未知服务器"
                    val toolName = exposed?.tool?.name ?: call.function.name
                    onToolStart?.invoke(serverName, toolName)

                    var content: String
                    var errorText: String? = null
                    var durationMs = 0L

                    if (exposed == null) {
                        content = "工具调用失败：未找到名为 " + call.function.name + " 的工具。"
                        errorText = content
                    } else {
                        val arguments = parseToolArguments(call.function.arguments)
                        if (arguments == null) {
                            content = "工具调用失败：arguments 不是合法的 JSON 对象。"
                            errorText = content
                        } else {
                            val start = System.currentTimeMillis()
                            val result = mcpClient.callTool(exposed.server, exposed.tool.name, arguments)
                            durationMs = System.currentTimeMillis() - start
                            content = if (result.isError) {
                                "工具执行失败：" + result.text
                            } else {
                                result.text.ifBlank { "（工具返回空结果）" }
                            }
                            if (result.isError) errorText = result.text
                        }
                    }

                    val completed = McpCallInfo(
                        serverName = serverName,
                        toolName = toolName,
                        durationMs = durationMs,
                        error = errorText
                    )
                    collectedCalls += completed
                    onToolComplete?.invoke(completed)
                    workingMessages += ApiMessage(
                        role = "tool",
                        tool_call_id = normalizedCalls[index].id,
                        content = content
                    )
                }
                return@repeat
            }

            // ---------- 2) 文本协议兜底（旧模型/中转） ----------
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
                val visible = ToolCallMarkup.stripMarkup(roundText)
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

            val assistantText = ToolCallMarkup.stripMarkup(roundText)
            workingMessages += ApiMessage(
                role = "assistant",
                content = assistantText.ifBlank { " " }
            )

            val resultText = StringBuilder()
            for (call in calls) {
                onToolStart?.invoke(call.serverName, call.tool)
                val server = call.server
                if (server == null) {
                    val error = "未找到提供工具 \"" + call.tool + "\" 的 MCP 服务器"
                    val completed = McpCallInfo(
                        serverName = call.serverName,
                        toolName = call.tool,
                        durationMs = 0,
                        error = error
                    )
                    collectedCalls += completed
                    onToolComplete?.invoke(completed)
                    resultText.append("工具 \"" + call.tool + "\" 执行失败：" + error + "\n\n")
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
                resultText.append(
                    "工具 \"" + call.tool + "\"（服务器 " + server.name + "）返回结果：\n" +
                        result.text + "\n\n"
                )
            }

            val resultPayload = if (config.provider == LlmProvider.DeepSeek) {
                "<tool_result>\n" + resultText + "</tool_result>"
            } else {
                resultText.toString()
            }
            workingMessages += ApiMessage(
                role = "user",
                content = "你刚才请求调用 MCP 工具：" + calls.joinToString("、") { it.tool } + "。\n\n" +
                    "工具执行结果如下：\n\n" + resultPayload +
                    "请基于以上结果继续回答用户的问题，不要重复工具调用说明，除非还需要调用其他工具。"
            )
        }

        val last = sendChat(
            messages = workingMessages,
            config = config,
            extraSystemPrompt = toolHint,
            tools = toolDefinitions
        )
        return ChatResult(
            content = ToolCallMarkup.stripMarkup(last.content),
            reasoningText = last.reasoningText?.let { ToolCallMarkup.stripMarkup(it) },
            mcpCalls = collectedCalls,
            toolCalls = last.toolCalls
        )
    }
}

/** 流式响应的数据块。 */
data class StreamChunk(
    val content: String = "",
    val reasoningText: String? = null,
    /** 仅在本轮流结束时给出一次完整的工具调用列表。 */
    val toolCalls: List<ApiToolCall>? = null
)

/** 聊天请求结果。 */
data class ChatResult(
    val content: String,
    val reasoningText: String? = null,
    val mcpCalls: List<McpCallInfo> = emptyList(),
    val toolCalls: List<ApiToolCall> = emptyList()
)
