/*
OpenDroidChat MCP Client Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import com.hoeteam.opendroidchat.data.McpServer
import com.hoeteam.opendroidchat.data.McpTool
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * 一次模型希望执行的 MCP 工具调用（由模型输出中解析而来）。
 */
data class McpToolCall(
    val server: String,
    val tool: String,
    val arguments: Map<String, JsonElement>
)

/**
 * 一次 MCP 工具调用的执行结果。
 */
data class McpToolResult(
    val text: String,
    val isError: Boolean = false
)

/**
 * MCP (Model Context Protocol) 客户端。
 * 使用 JSON-RPC 2.0 over HTTP 与 MCP 服务器通信，支持 tools/list 与 tools/call。
 */
class McpClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 15000
        }
        expectSuccess = false
    }

    /**
     * 获取服务器暴露的工具列表。失败时返回空列表（调用方据此跳过该服务器）。
     */
    suspend fun listTools(server: McpServer): List<McpTool> {
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "tools/list")
        }

        val response: JsonElement = try {
            post(server, requestBody)
        } catch (e: Exception) {
            return emptyList()
        }

        val result = response.jsonObject["result"] as? JsonObject ?: return emptyList()
        val tools = result["tools"] as? JsonArray ?: return emptyList()

        return tools.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            McpTool(
                name = name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull ?: "",
                inputSchema = obj["inputSchema"]?.toString()
            )
        }
    }

    /**
     * 调用服务器上的某个工具，返回结果（含成功/失败标记）。
     */
    suspend fun callTool(server: McpServer, toolName: String, arguments: Map<String, JsonElement>): McpToolResult {
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "tools/call")
            put("params", buildJsonObject {
                put("name", toolName)
                put("arguments", buildJsonObject {
                    arguments.forEach { (key, value) -> put(key, value) }
                })
            })
        }

        val response: JsonElement = try {
            post(server, requestBody)
        } catch (e: Exception) {
            // 网络/连接异常
            return McpToolResult(text = "调用工具失败：${e.message ?: e::class.simpleName}", isError = true)
        }

        // JSON-RPC 错误对象
        val errObj = (response as? JsonObject)?.get("error")
        if (errObj != null) {
            val msg = if (errObj is JsonObject) {
                errObj["message"]?.jsonPrimitive?.contentOrNull ?: errObj.toString()
            } else errObj.toString()
            return McpToolResult(text = "调用工具失败：$msg", isError = true)
        }

        // MCP 返回 isError=true（工具执行出错）
        val resultObj = (response as? JsonObject)?.get("result") as? JsonObject
        if (resultObj?.get("isError")?.jsonPrimitive?.booleanOrNull == true) {
            return McpToolResult(text = "调用工具失败：${extractTextContent(response)}", isError = true)
        }

        return McpToolResult(text = extractTextContent(response))
    }

    private suspend fun post(server: McpServer, body: JsonObject): JsonElement {
        val response: HttpResponse = client.post(server.url) {
            contentType(ContentType.Application.Json)
            // 若配置了 API Key，则按 MCP 标准约定通过 Authorization: Bearer 传递
            if (server.apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${server.apiKey.trim()}")
            }
            setBody(body.toString())
        }
        val bodyText = response.bodyAsText()
        return try {
            json.parseToJsonElement(bodyText)
        } catch (e: Exception) {
            // 非 JSON 响应，原样包裹为文本
            buildJsonObject {
                put("result", buildJsonObject {
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", bodyText)
                        })
                    })
                })
            }
        }
    }

    /**
     * 从 MCP 响应中提取文本内容（兼容 result.content[].text 标准结构）。
     */
    private fun extractTextContent(response: JsonElement): String {
        val result = response.jsonObject["result"] as? JsonObject ?: return response.toString()
        val content = result["content"] as? JsonArray
            ?: return result["structuredContent"]?.toString() ?: result.toString()

        val sb = StringBuilder()
        content.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val type = obj["type"]?.jsonPrimitive?.contentOrNull
            when (type) {
                "text" -> {
                    obj["text"]?.jsonPrimitive?.contentOrNull?.let { sb.append(it) }
                }
                else -> sb.append(el.toString())
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    companion object {
        /** 见 [ToolCallMarkup]：这里保留原有入口，内部统一委托给标记解析器。 */
        fun extractMcpCallBlocks(text: String): List<String> =
            ToolCallMarkup.extractMcpCallBlocks(text)

        fun parseMcpCalls(text: String): List<McpToolCall> =
            ToolCallMarkup.parseMcpCalls(text)

        fun stripMcpCalls(text: String): String = ToolCallMarkup.stripMarkup(text)

        fun holdBackLength(text: String): Int = ToolCallMarkup.holdBackLength(text)
    }
}
