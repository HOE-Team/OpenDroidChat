/*
OpenDroidChat OpenAI Tool Call Accumulator
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import com.hoeteam.opendroidchat.data.ApiFunctionCall
import com.hoeteam.opendroidchat.data.ApiToolCall
import com.hoeteam.opendroidchat.data.ApiToolCallDelta

/**
 * OpenAI 流式响应中的 tool_calls 是分片到达的：
 * 第一片带 id/name，后续片只带 arguments 的片段。
 * 这里按 index 聚合，最后生成完整调用。
 */
internal class ToolCallAccumulator {
    private var id: String = ""
    private var type: String = "function"
    private var name: String = ""
    private val arguments = StringBuilder()

    fun append(delta: ApiToolCallDelta) {
        delta.id?.takeIf { it.isNotBlank() }?.let { id = it }
        delta.type?.takeIf { it.isNotBlank() }?.let { type = it }
        delta.function?.name?.let { name += it }
        delta.function?.arguments?.let { arguments.append(it) }
    }

    fun build(index: Int): ApiToolCall = ApiToolCall(
        id = id.ifBlank {
            "call_" + java.util.UUID.randomUUID().toString().take(8) + "_" + index
        },
        type = type,
        function = ApiFunctionCall(name = name, arguments = arguments.toString())
    )
}
