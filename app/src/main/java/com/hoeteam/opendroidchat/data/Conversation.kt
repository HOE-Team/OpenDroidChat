/*
OpenDroidChat Conversation Data Module
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.UUID

/**
 * 一次完整的对话，也是「历史对话」功能的持久化单位。
 *
 * [messages] 就是聊天界面里展示的同一份消息列表，因此打开历史对话时
 * 不需要做任何格式转换，直接把消息灌回 UI 状态即可继续聊天。
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String? = null,
    val modelName: String? = null,
    val messages: List<Message> = emptyList()
) {
    /**
     * 历史列表只需要标题、时间和预览。
     * 单独抽成轻量摘要后，列表页无需解析整段消息 JSON。
     */
    fun toSummary(): ConversationSummary = ConversationSummary(
        id = id,
        title = title,
        updatedAt = updatedAt,
        messageCount = messages.size,
        preview = messages
            .lastOrNull { it.text.isNotBlank() && !it.text.contains(ERROR_MARKER) }
            ?.text
            // 预览是纯文本，去掉 Markdown 的粗体/行内代码标记，读起来更干净
            ?.replace(MARKDOWN_MARKUP_REGEX, "")
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim()
            ?.take(PREVIEW_MAX_LENGTH)
            .orEmpty(),
        modelName = modelName
    )

    companion object {
        // 与 ChatViewModel 中错误消息的前缀保持一致，用于生成预览时跳过报错文本
        internal const val ERROR_MARKER = "LLM API 响应出错"
        private const val PREVIEW_MAX_LENGTH = 120
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val MARKDOWN_MARKUP_REGEX = Regex("[*`]+")
    }
}

/**
 * 历史对话列表中的一行：只包含展示所需的最小信息。
 */
data class ConversationSummary(
    val id: String,
    val title: String = "",
    val updatedAt: Long = 0L,
    val messageCount: Int = 0,
    val preview: String = "",
    val modelName: String? = null
)

/**
 * Gson 无法自动还原 Kotlin sealed class 的多态类型，
 * 这里为 [McpSegment] 显式写入类型标记，保证「思考块 / MCP 调用组」
 * 在保存并重新打开对话后仍然按原有顺序显示。
 */
internal class McpSegmentTypeAdapter : JsonSerializer<McpSegment>, JsonDeserializer<McpSegment> {

    override fun serialize(
        src: McpSegment,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = JsonObject()
        when (src) {
            is McpSegment.Thinking -> {
                json.addProperty(FIELD_TYPE, TYPE_THINKING)
                json.addProperty(FIELD_TEXT, src.text)
            }
            is McpSegment.Calls -> {
                json.addProperty(FIELD_TYPE, TYPE_CALLS)
                json.add(FIELD_CALLS, context.serialize(src.calls, CALLS_LIST_TYPE))
            }
        }
        return json
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): McpSegment {
        if (!json.isJsonObject) return McpSegment.Thinking("")
        val obj = json.asJsonObject
        return when (obj.get(FIELD_TYPE)?.asString) {
            TYPE_CALLS -> McpSegment.Calls(
                context.deserialize<List<McpCallInfo>>(obj.get(FIELD_CALLS), CALLS_LIST_TYPE)
                    ?: emptyList()
            )
            else -> McpSegment.Thinking(obj.get(FIELD_TEXT)?.asString.orEmpty())
        }
    }

    private companion object {
        const val FIELD_TYPE = "type"
        const val FIELD_TEXT = "text"
        const val FIELD_CALLS = "calls"
        const val TYPE_THINKING = "thinking"
        const val TYPE_CALLS = "calls"
        val CALLS_LIST_TYPE: Type = object : TypeToken<List<McpCallInfo>>() {}.type
    }
}
