/*
OpenDroidChat LLM Config Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// LLM 提供商枚举
enum class LlmProvider(val displayName: String) {
    OpenAI("OpenAI / Azure OpenAI"),
    Gemini("Google Gemini API"),
    DeepSeek("DeepSeek"),
    Dashscope("阿里云百炼(Dashscope)"),
    Claude("Claude"),
    Custom("自定义 API")
}

// 单个 LLM 模型的配置
@Immutable
data class LlmModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val provider: LlmProvider,
    val apiKey: String,
    val modelName: String,
    val systemPrompt: String, // 系统默认的提示词
    val customApiUrl: String? = null,
    val appId: String? = null,
    val useStream: Boolean = true, // 是否使用流式传输，默认为 true
    val enableThinking: Boolean = false, // 是否启用思考模式
    val reasoningEffort: String? = null // 思考深度（high/low/medium/max）
)

// 聊天消息 (用于 UI 展示)
@Immutable
data class Message(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isThinkingStreaming: Boolean = false, // 思考内容是否仍在流式传输
    val selectedFile: SelectedFile? = null,
    val reasoningText: String? = null // 思考内容（reasoning/reasoning_text）
)

enum class Sender {
    USER,
    LLM
}

// 所选文件的数据模型
data class SelectedFile(
    val uri: String,
    val fileName: String,
    val isCodeFile: Boolean,
    val content: String
)

// ----------------- API 传输数据模型 (用于 Ktor 序列化) -----------------

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean? = null,
    val thinking: JsonElement? = null,          // DeepSeek: {"type": "enabled"/"disabled"}
    val enable_thinking: Boolean? = null,       // Qwen: true/false
    val reasoning_effort: String? = null,       // DeepSeek reasoning effort
    // 当开启思考时，自动跳过 temperature、top_p 等采样参数
    val temperature: JsonElement? = null,       // 思考模式下置空
    val top_p: JsonElement? = null              // 思考模式下置空
)

// Claude 专用请求体 (Claude API 格式与 OpenAI 不同)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClaudeChatRequest(
    val model: String,
    val max_tokens: Int = 8192,
    val messages: List<ClaudeApiMessage>,
    val stream: Boolean? = null,
    val system: String? = null,
    val thinking: ClaudeThinkingConfig? = null,
    val outputConfig: ClaudeOutputConfig? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClaudeThinkingConfig(
    val type: String, // "enabled", "disabled", "adaptive"
    val budgetTokens: Int? = null, // 固定预算模式用
    val display: String? = null   // "summarized" 或 "omitted", 仅自适应模式
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClaudeOutputConfig(
    val effort: String? = null // "low", "medium", "high", "max" 控制思考深度
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClaudeApiMessage(
    val role: String,
    val content: String
)

// Gemini 思考配置
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GeminiThinkingConfig(
    val thinkingLevel: String? = null, // "high"/"off"
    val thinkingBudget: Int? = null    // -1/0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Choice(
    val message: ApiMessage? = null,
    val delta: Delta? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Delta(
    val content: String? = null,
    val reasoning_content: String? = null  // DeepSeek/Qwen 思考内容字段
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiMessage(
    val role: String, // "user" 或 "assistant"
    val content: String,
    val reasoning_content: String? = null  // 思考内容（非流式响应用）
)

// Gemini Streaming Models
@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)