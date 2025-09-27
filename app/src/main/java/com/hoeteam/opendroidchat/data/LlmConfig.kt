package com.hoeteam.opendroidchat.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// LLM 提供商枚举
enum class LlmProvider(val displayName: String) {
    OpenAI("OpenAI / Azure OpenAI"),
    Gemini("Google Gemini API"),
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
    val systemPrompt: String = "你是一个乐于助人的 AI 助手，以简洁明了的方式回答问题。",
    val customApiUrl: String? = null
)

// 聊天消息 (用于 UI 展示)
@Immutable
data class Message(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Sender {
    USER,
    LLM
}

// ----------------- API 传输数据模型 (用于 Ktor 序列化) -----------------

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>
)

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ApiMessage
)

@Serializable
data class ApiMessage(
    val role: String, // "user" 或 "assistant"
    val content: String
)