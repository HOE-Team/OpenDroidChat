package com.hoeteam.opendroidchat.network

import com.hoeteam.opendroidchat.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

class LlmApiService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000 // 1分钟超时
        }
        expectSuccess = true
    }

    private fun getApiUrl(provider: LlmProvider): String {
        return when (provider) {
            LlmProvider.OpenAI -> "https://api.openai.com/v1/chat/completions"
            // 注意：Gemini URL 中的模型名需要替换
            LlmProvider.Gemini -> "https://generativelanguage.googleapis.com/v1beta/models/MODEL_PLACEHOLDER:generateContent"
            LlmProvider.Custom -> ""
        }
    }

    private val modelNamePlaceholder = "MODEL_PLACEHOLDER"

    suspend fun sendChat(
        messages: List<ApiMessage>,
        config: LlmModel
    ): String {
        // 1. 确定 API URL 和 Key
        var apiUrl = config.customApiUrl.takeIf { it?.isNotBlank() == true } ?: getApiUrl(config.provider)
        val apiKey = config.apiKey

        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw IllegalStateException("API URL 或 API Key 不能为空。请检查配置。")
        }

        if (config.provider == LlmProvider.Gemini) {
            apiUrl = apiUrl.replace(modelNamePlaceholder, config.modelName)
        }

        // 2. 构造请求体
        val systemMessage = ApiMessage(role = "system", content = config.systemPrompt)
        val fullMessages = listOf(systemMessage) + messages

        val requestBody = ChatRequest(
            model = config.modelName,
            messages = fullMessages
        )

        // 3. 发起请求
        try {
            val response: ChatResponse = client.post(apiUrl) {
                // 默认使用 Authorization Header
                header("Authorization", "Bearer $apiKey")

                // 针对 Gemini 示例，通常使用 URL 参数
                if (config.provider == LlmProvider.Gemini) {
                    parameter("key", apiKey)
                    headers {
                        remove(HttpHeaders.Authorization)
                    }
                }

                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            // 4. 提取响应文本
            return response.choices.firstOrNull()?.message?.content ?: "未能获取到 LLM 响应。"

        } catch (e: Exception) {
            val errorDetails = when (e) {
                is ClientRequestException -> "API 错误: ${e.response.status.value}. 可能是 Key 无效或请求格式错误。"
                is ServerResponseException -> "服务器错误: ${e.response.status.value}."
                is UnknownHostException -> "网络连接失败: 无法解析主机名。"
                else -> "未知错误: ${e.message}"
            }
            throw Exception(errorDetails, e)
        }
    }
}