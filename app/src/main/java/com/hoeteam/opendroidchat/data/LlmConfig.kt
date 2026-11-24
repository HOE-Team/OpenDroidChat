/*
OpenDroidChat LLM Config Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.hoeteam.opendroidchat.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * LLM 提供商。
 *
 * 除 Claude 外，所有提供商都走统一的 OpenAI 兼容协议
 * （/chat/completions + Bearer 鉴权 + 标准 tool calling）。
 * Gemini 使用 Google 官方提供的 OpenAI 兼容端点。
 * Claude 的原生协议实现已被移除，保留枚举仅用于兼容旧配置并给出明确提示。
 */
enum class LlmProvider(val displayName: String, val selectable: Boolean = true) {
    OpenAI("OpenAI / Azure OpenAI"),
    Gemini("Google Gemini（OpenAI 兼容）"),
    DeepSeek("DeepSeek"),
    Dashscope("阿里云百炼(Dashscope)"),
    Claude("Claude（原生协议已移除）", selectable = false),
    Custom("自定义 OpenAI 兼容 API")
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
    val reasoningEffort: String? = null, // 思考深度（OpenAI: low/medium/high, DeepSeek: high/max）
    val enableMcp: Boolean = false // 是否允许该模型调用 MCP 服务器工具
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
    val reasoningText: String? = null, // 思考内容（reasoning/reasoning_content）
    val segments: List<McpSegment> = emptyList() // 思考块与 MCP 调用按发生顺序堆叠
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

// ----------------- API 传输数据模型 (OpenAI 兼容协议) -----------------

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean? = null,
    val thinking: JsonElement? = null,          // DeepSeek: {"type": "enabled"/"disabled"}
    val enable_thinking: Boolean? = null,       // Qwen: true/false
    val reasoning_effort: String? = null,       // DeepSeek / OpenAI reasoning effort
    // 当开启思考时，自动跳过 temperature、top_p 等采样参数
    val temperature: JsonElement? = null,       // 思考模式下置空
    val top_p: JsonElement? = null,             // 思考模式下置空
    val tools: List<ToolDefinition>? = null,    // OpenAI 标准工具定义
    val tool_choice: JsonElement? = null        // "auto" / "none" / 指定工具
)

// OpenAI 标准工具定义
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ToolDefinition(
    @EncodeDefault
    val type: String = "function",
    val function: ToolFunctionDefinition
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ToolFunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null
)

// 非流式 / 历史消息中的工具调用
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiToolCall(
    @EncodeDefault
    val id: String = "",
    @EncodeDefault
    val type: String = "function",
    val function: ApiFunctionCall = ApiFunctionCall()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiFunctionCall(
    @EncodeDefault
    val name: String = "",
    @EncodeDefault
    val arguments: String = ""
)

// 流式增量中的工具调用分片
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiToolCallDelta(
    val index: Int = 0,
    val id: String? = null,
    val type: String? = null,
    val function: ApiFunctionCallDelta? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiFunctionCallDelta(
    val name: String? = null,
    val arguments: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Choice(
    val message: ApiMessage? = null,
    val delta: Delta? = null,
    val finish_reason: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Delta(
    val content: String? = null,
    val reasoning_content: String? = null, // DeepSeek/Qwen 思考内容字段
    val tool_calls: List<ApiToolCallDelta>? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiMessage(
    val role: String, // "system" / "user" / "assistant" / "tool"
    val content: String? = null,
    val reasoning_content: String? = null, // 思考内容（非流式响应用）
    val tool_calls: List<ApiToolCall>? = null, // assistant 的工具调用
    val tool_call_id: String? = null, // tool 消息对应的调用 id
    val name: String? = null // 可选：工具名
)
