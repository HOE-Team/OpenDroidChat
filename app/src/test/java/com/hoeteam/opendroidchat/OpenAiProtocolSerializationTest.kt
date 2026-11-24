package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.data.ApiFunctionCall
import com.hoeteam.opendroidchat.data.ApiMessage
import com.hoeteam.opendroidchat.data.ApiToolCall
import com.hoeteam.opendroidchat.data.ChatRequest
import com.hoeteam.opendroidchat.data.ToolDefinition
import com.hoeteam.opendroidchat.data.ToolFunctionDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 校验统一后的 OpenAI 协议字段：
 * - tools[].type / function.name
 * - assistant.tool_calls
 * - tool 角色消息的 tool_call_id
 */
class OpenAiProtocolSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun serializesToolsAndToolMessages() {
        val request = ChatRequest(
            model = "deepseek-chat",
            messages = listOf(
                ApiMessage(role = "user", content = "查一下 CVE"),
                ApiMessage(
                    role = "assistant",
                    tool_calls = listOf(
                        ApiToolCall(
                            id = "call_1",
                            type = "function",
                            function = ApiFunctionCall(
                                name = "AnySearch__extract",
                                arguments = "{\"url\":\"https://example.com\"}"
                            )
                        )
                    )
                ),
                ApiMessage(
                    role = "tool",
                    tool_call_id = "call_1",
                    content = "页面正文"
                )
            ),
            tools = listOf(
                ToolDefinition(
                    type = "function",
                    function = ToolFunctionDefinition(
                        name = "AnySearch__extract",
                        description = "提取网页",
                        parameters = buildJsonObject {
                            put("type", "object")
                        }
                    )
                )
            )
        )

        val encoded = json.encodeToString(request)
        val root = json.parseToJsonElement(encoded).jsonObject

        val tool = root["tools"]!!.jsonArray[0].jsonObject
        assertEquals("function", tool["type"]!!.jsonPrimitive.content)
        assertEquals(
            "AnySearch__extract",
            tool["function"]!!.jsonObject["name"]!!.jsonPrimitive.content
        )

        val messages = root["messages"]!!.jsonArray
        val assistant = messages[1].jsonObject
        assertEquals("assistant", assistant["role"]!!.jsonPrimitive.content)
        assertFalse(assistant.containsKey("content"))
        val toolCall = assistant["tool_calls"]!!.jsonArray[0].jsonObject
        assertEquals("call_1", toolCall["id"]!!.jsonPrimitive.content)
        assertEquals("function", toolCall["type"]!!.jsonPrimitive.content)
        assertEquals(
            "{\"url\":\"https://example.com\"}",
            toolCall["function"]!!.jsonObject["arguments"]!!.jsonPrimitive.content
        )

        val toolMessage = messages[2].jsonObject
        assertEquals("tool", toolMessage["role"]!!.jsonPrimitive.content)
        assertEquals("call_1", toolMessage["tool_call_id"]!!.jsonPrimitive.content)
        assertEquals("页面正文", toolMessage["content"]!!.jsonPrimitive.content)
    }
}
