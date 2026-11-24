/*
OpenDroidChat MCP Server Data Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * 一个 MCP (Model Context Protocol) 服务器配置。
 * 模型可以通过这类服务器调用其暴露的工具（Tools）。
 */
@Immutable
data class McpServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val description: String = "",
    val enabled: Boolean = true, // 是否允许当前模型调用该服务器
    val apiKey: String = "" // 可选：调用该服务器时通过 Authorization: Bearer 传入的 API Key
)

/**
 * MCP 服务器暴露的单个工具。
 */
@Immutable
data class McpTool(
    val name: String,
    val description: String = "",
    val inputSchema: String? = null // JSON Schema 字符串
)

/**
 * 一次 MCP 工具调用的记录（用于 UI 展示调用服务器与耗时）。
 */
@Immutable
data class McpCallInfo(
    val serverName: String,
    val toolName: String,
    val durationMs: Long,
    val pending: Boolean = false, // true 表示调用仍在进行中（UI 显示加载转圈）
    val error: String? = null // 若调用失败，携带错误信息
)

/**
 * 消息中的内容分段：思考块、正文或 MCP 调用组，按发生顺序堆叠显示。
 * 正文分段用于在多次工具调用之间保留输出顺序。
 */
@Immutable
sealed class McpSegment {
    data class Thinking(val text: String) : McpSegment()
    data class Text(val text: String) : McpSegment()
    data class Calls(val calls: List<McpCallInfo>) : McpSegment()
}
