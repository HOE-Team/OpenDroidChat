package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.data.ChatResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 校验 OpenAI 流式/非流式响应里 tool_calls 的字段能被正确反序列化。
 */
class OpenAiToolCallParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun parsesStreamingToolCallDelta() {
        val data = """
            {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"id":"call_1","type":"function","function":{"name":"AnySearch__extract","arguments":"{\"url\":"}}
            ]},"finish_reason":null}]}
        """.trimIndent()

        val response = json.decodeFromString<ChatResponse>(data)
        val delta = response.choices.first().delta

        assertEquals("call_1", delta?.tool_calls?.first()?.id)
        assertEquals("AnySearch__extract", delta?.tool_calls?.first()?.function?.name)
        assertEquals("{\"url\":", delta?.tool_calls?.first()?.function?.arguments)
    }

    @Test
    fun parsesNonStreamingToolCallsWithNullContent() {
        val data = """
            {"choices":[{"index":0,"message":{"role":"assistant","content":null,"tool_calls":[
              {"id":"call_2","type":"function","function":{"name":"AnySearch__extract","arguments":"{\"url\":\"https://x\"}"}}
            ]},"finish_reason":"tool_calls"}]}
        """.trimIndent()

        val response = json.decodeFromString<ChatResponse>(data)
        val message = response.choices.first().message

        assertNull(message?.content)
        assertEquals("call_2", message?.tool_calls?.first()?.id)
        assertEquals(
            "{\"url\":\"https://x\"}",
            message?.tool_calls?.first()?.function?.arguments
        )
    }
}
