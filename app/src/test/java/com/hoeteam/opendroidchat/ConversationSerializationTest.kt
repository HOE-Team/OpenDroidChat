package com.hoeteam.opendroidchat

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hoeteam.opendroidchat.data.Conversation
import com.hoeteam.opendroidchat.data.McpCallInfo
import com.hoeteam.opendroidchat.data.McpSegment
import com.hoeteam.opendroidchat.data.McpSegmentTypeAdapter
import com.hoeteam.opendroidchat.data.Message
import com.hoeteam.opendroidchat.data.SelectedFile
import com.hoeteam.opendroidchat.data.Sender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 历史对话落盘依赖 Gson 反射序列化 Kotlin data class，
 * 这里用真实数据走一遍「保存 -> 读取」，确认消息、思考块、
 * MCP 调用与文件附件都能原样恢复。
 */
class ConversationSerializationTest {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(McpSegment::class.java, McpSegmentTypeAdapter())
        .create()

    @Test
    fun conversationRoundTripKeepsAllMessageDetails() {
        val conversation = Conversation(
            id = "conversation-1",
            title = "如何学习 Kotlin",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_500_000L,
            modelId = "model-1",
            modelName = "DeepSeek V4",
            messages = listOf(
                Message(
                    id = 1L,
                    text = "帮我看看这个文件",
                    sender = Sender.USER,
                    timestamp = 1_700_000_100_000L,
                    selectedFile = SelectedFile(
                        uri = "content://demo/file",
                        fileName = "Main.kt",
                        isCodeFile = true,
                        content = "fun main() {}"
                    )
                ),
                Message(
                    id = 2L,
                    text = "这是一个 Kotlin 入口函数。",
                    sender = Sender.LLM,
                    timestamp = 1_700_000_200_000L,
                    reasoningText = "先分析文件内容",
                    segments = listOf(
                        McpSegment.Thinking("先分析文件内容"),
                        McpSegment.Calls(
                            listOf(
                                McpCallInfo(
                                    serverName = "文件服务",
                                    toolName = "read_file",
                                    durationMs = 42L
                                )
                            )
                        )
                    )
                )
            )
        )

        val json = gson.toJson(conversation)
        val restored = gson.fromJson<Conversation>(json, Conversation::class.java)

        assertEquals(conversation.id, restored.id)
        assertEquals(conversation.title, restored.title)
        assertEquals(conversation.modelName, restored.modelName)
        assertEquals(2, restored.messages.size)

        val userMessage = restored.messages[0]
        assertEquals(Sender.USER, userMessage.sender)
        assertEquals("Main.kt", userMessage.selectedFile?.fileName)
        assertTrue(userMessage.selectedFile?.isCodeFile == true)
        assertEquals("fun main() {}", userMessage.selectedFile?.content)

        val llmMessage = restored.messages[1]
        assertEquals(Sender.LLM, llmMessage.sender)
        assertEquals("先分析文件内容", llmMessage.reasoningText)
        assertEquals(2, llmMessage.segments.size)
        assertTrue(llmMessage.segments[0] is McpSegment.Thinking)
        val calls = llmMessage.segments[1] as McpSegment.Calls
        assertEquals("read_file", calls.calls.single().toolName)
        assertEquals(42L, calls.calls.single().durationMs)
    }

    @Test
    fun summaryUsesLastRealMessageAndSkipsApiErrors() {
        val conversation = Conversation(
            title = "标题",
            messages = listOf(
                Message(text = "**第一条** `code`", sender = Sender.USER),
                Message(text = "LLM API 响应出错: timeout", sender = Sender.LLM)
            )
        )

        val summary = conversation.toSummary()

        assertEquals("标题", summary.title)
        assertEquals(2, summary.messageCount)
        // 预览会去掉 Markdown 标记，并把换行压成空格
        assertEquals("第一条 code", summary.preview)
    }

    @Test
    fun summaryListSurvivesIndexRoundTrip() {
        val summaries = listOf(
            Conversation(title = "A").toSummary(),
            Conversation(title = "B").toSummary()
        )
        val type = object : TypeToken<List<com.hoeteam.opendroidchat.data.ConversationSummary>>() {}.type
        val restored = gson.fromJson<List<com.hoeteam.opendroidchat.data.ConversationSummary>>(
            gson.toJson(summaries, type),
            type
        )
        assertEquals(listOf("A", "B"), restored.map { it.title })
    }
}
