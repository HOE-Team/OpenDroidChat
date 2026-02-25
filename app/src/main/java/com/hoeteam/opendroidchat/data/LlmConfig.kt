/*
OpenDroidChat LLM Config Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

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
    val systemPrompt: String, // 系统默认的提示词，这里没有加则意味着我想让用户自己选择加不加/怎么加
    val customApiUrl: String? = null,
    val appId: String? = null
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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Choice(
    val message: ApiMessage
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiMessage(
    val role: String, // "user" 或 "assistant"
    val content: String
)