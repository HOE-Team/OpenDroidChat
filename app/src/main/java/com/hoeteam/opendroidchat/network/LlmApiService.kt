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
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

class LlmApiService {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
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

    fun sendChatStream(
        messages: List<ApiMessage>,
        config: LlmModel
    ): Flow<String> = flow {
        var apiUrl = config.customApiUrl.takeIf { it?.isNotBlank() == true } ?: getApiUrl(config.provider, true)
        val apiKey = config.apiKey

        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        if (config.provider == LlmProvider.Gemini) {
            apiUrl = apiUrl.replace(modelNamePlaceholder, config.modelName)
        }

        val systemMessage = ApiMessage(role = "system", content = config.systemPrompt)
        val fullMessages = listOf(systemMessage) + messages

        val requestBody = ChatRequest(
            model = config.modelName,
            messages = fullMessages,
            stream = true
        )

        try {
            client.preparePost(apiUrl) {
                header("Authorization", "Bearer $apiKey")
                when (config.provider) {
                    LlmProvider.Gemini -> {
                        parameter("key", apiKey)
                        headers { remove(HttpHeaders.Authorization) }
                    }
                    LlmProvider.Dashscope -> {
                        config.appId?.takeIf { it.isNotBlank() }?.let {
                            header("X-DashScope-Appid", it)
                        }
                    }
                    else -> {}
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isEmpty()) continue

                    if (config.provider == LlmProvider.Gemini) {
                        // Gemini stream returns an array of objects or individual objects
                        // Simple parsing for text in Gemini response
                        try {
                            val cleanLine = line.trim().removePrefix("[").removeSuffix(",").removeSuffix("]")
                            if (cleanLine.isNotEmpty()) {
                                val geminiResp = json.decodeFromString<GeminiResponse>(cleanLine)
                                geminiResp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.let {
                                    emit(it)
                                }
                            }
                        } catch (e: Exception) {
                            // Skip lines that aren't valid JSON chunks
                        }
                    } else {
                        // OpenAI format: data: {"choices":[{"delta":{"content":"..."}}]}
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()
                            if (data == "[DONE]") break
                            try {
                                val chatResp = json.decodeFromString<ChatResponse>(data)
                                chatResp.choices.firstOrNull()?.delta?.content?.let {
                                    emit(it)
                                }
                            } catch (e: Exception) {
                                // Skip
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendChat(
        messages: List<ApiMessage>,
        config: LlmModel
    ): String {
        var apiUrl = config.customApiUrl.takeIf { it?.isNotBlank() == true } ?: getApiUrl(config.provider, false)
        val apiKey = config.apiKey

        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。")
        }

        if (config.provider == LlmProvider.Gemini) {
            apiUrl = apiUrl.replace(modelNamePlaceholder, config.modelName)
        }

        val systemMessage = ApiMessage(role = "system", content = config.systemPrompt)
        val fullMessages = listOf(systemMessage) + messages

        val requestBody = ChatRequest(
            model = config.modelName,
            messages = fullMessages,
            stream = false
        )

        try {
            val response: ChatResponse = client.post(apiUrl) {
                header("Authorization", "Bearer $apiKey")
                when (config.provider) {
                    LlmProvider.Gemini -> {
                        parameter("key", apiKey)
                        headers { remove(HttpHeaders.Authorization) }
                    }
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

            return response.choices.firstOrNull()?.message?.content ?: "未能获取到 LLM API 响应。"

        } catch (e: Exception) {
            throw e
        }
    }
}
