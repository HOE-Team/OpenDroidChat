package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.network.ToolCallMarkup
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 工具调用标记的解析与清理测试：
 * - 本应用的 [MCP_CALL] 文本协议
 * - DeepSeek 的 DSML 标记（含全角竖线与 ASCII 竖线）
 * - 流式输出时对未完成标记的暂存
 */
class ToolCallMarkupTest {

    private val dsmlSample = "<\uFF5C\uFF5CDSML\uFF5C\uFF5Ccalls>" +
        "<\uFF5C\uFF5CDSML\uFF5C\uFF5Cinvoke name=\"extract\">" +
        "<\uFF5C\uFF5CDSML\uFF5C\uFF5Cparameter name=\"url\" string=\"true\">https://kernelsu.org/guide/module.html<\uFF5C\uFF5CDSML\uFF5C\uFF5Cparameter>" +
        "</\uFF5C\uFF5CDSML\uFF5C\uFF5Cinvoke>" +
        "<\uFF5C\uFF5CDSML\uFF5C\uFF5Cinvoke name=\"extract\">" +
        "<\uFF5C\uFF5CDSML\uFF5C\uFF5Cparameter name=\"url\" string=\"true\">https://github.com/KernelSU-Modules-Repo/meta-overlayfs<\uFF5C\uFF5CDSML\uFF5C\uFF5Cparameter>" +
        "</\uFF5C\uFF5CDSML\uFF5C\uFF5Cinvoke>" +
        "</\uFF5C\uFF5CDSML\uFF5C\uFF5Ccalls>"

    @Test
    fun parsesAndStripsMcpCallMarker() {
        val text = "我先查一下。\n[MCP_CALL]{\"server\":\"AnySearch\",\"tool\":\"batch_search\",\"arguments\":{\"query\":\"CVE\"}}\n稍等"
        val calls = ToolCallMarkup.parseMcpCalls(text)

        assertEquals(1, calls.size)
        assertEquals("AnySearch", calls[0].server)
        assertEquals("batch_search", calls[0].tool)

        val visible = ToolCallMarkup.stripMarkup(text)
        assertFalse(visible.contains("[MCP_CALL]"))
        assertTrue(visible.contains("我先查一下。"))
        assertTrue(visible.contains("稍等"))
    }

    @Test
    fun parsesDsmlCallsWithFullWidthBars() {
        val text = "Let me extract:\n" + dsmlSample
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(2, calls.size)
        assertEquals("extract", calls[0].tool)
        assertEquals(
            "https://kernelsu.org/guide/module.html",
            calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull
        )
        assertEquals(
            "https://github.com/KernelSU-Modules-Repo/meta-overlayfs",
            calls[1].arguments["url"]?.jsonPrimitive?.contentOrNull
        )

        val visible = ToolCallMarkup.stripMarkup(text)
        assertFalse(visible.contains("DSML"))
        assertFalse(visible.contains("kernelsu.org"))
        assertTrue(visible.contains("Let me extract:"))
    }

    @Test
    fun parsesDsmlCallsWithAsciiBars() {
        val text = dsmlSample.replace('\uFF5C', '|')
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(2, calls.size)
        assertEquals("extract", calls[0].tool)
        assertEquals(
            "https://kernelsu.org/guide/module.html",
            calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull
        )
    }

    @Test
    fun parsesNonStringParameterAsJson() {
        val text = "<|\uFF5CDSML|\uFF5Ccalls><|\uFF5CDSML|\uFF5Cinvoke name=\"search\">" +
            "<|\uFF5CDSML|\uFF5Cparameter name=\"limit\" string=\"false\">5<|\uFF5CDSML|\uFF5Cparameter>" +
            "</|\uFF5CDSML|\uFF5Cinvoke></|\uFF5CDSML|\uFF5Ccalls>"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("5", calls[0].arguments["limit"]?.jsonPrimitive?.contentOrNull)
        assertEquals(false, calls[0].arguments["limit"]?.jsonPrimitive?.isString)
    }

    @Test
    fun parsesDsmlWithUnslashedClosingTags() {
        // 实机截图中 parameter 的闭合标记不带斜杠
        val text = "<｜｜DSML｜｜calls><｜｜DSML｜｜invoke name=\"extract\">" +
            "<｜｜DSML｜｜parameter name=\"url\" string=\"true\">https://example.com/a<｜｜DSML｜｜parameter>" +
            "</｜｜DSML｜｜invoke></｜｜DSML｜｜calls>"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("extract", calls[0].tool)
        assertEquals("https://example.com/a", calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull)
        assertEquals("", ToolCallMarkup.stripMarkup(text))
    }

    @Test
    fun parsesSpacedDsmlTags() {
        val text = "< | | DSML | | calls>< | | DSML | | invoke name=\"extract\">" +
            "< | | DSML | | parameter name=\"url\" string=\"true\">https://example.com/x< | | DSML | | parameter>" +
            "</ | | DSML | | invoke></ | | DSML | | calls>"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("extract", calls[0].tool)
        assertEquals("https://example.com/x", calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull)
        assertEquals("", ToolCallMarkup.stripMarkup(text))
    }

    @Test
    fun parsesFullWidthAngleBrackets() {
        val text = "＜｜｜DSML｜｜calls＞＜｜｜DSML｜｜invoke name=\"extract\"＞" +
            "＜｜｜DSML｜｜parameter name=\"url\" string=\"true\"＞https://example.com/y＜｜｜DSML｜｜parameter＞" +
            "＜/｜｜DSML｜｜invoke＞＜/｜｜DSML｜｜calls＞"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("https://example.com/y", calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull)
        assertEquals("", ToolCallMarkup.stripMarkup(text))
    }

    @Test
    fun looseParsesDsmlWithoutRecognizedTags() {
        val text = "< DSML calls >< DSML invoke name=\"extract\">" +
            "< DSML parameter name=\"url\" string=\"true\">https://example.com/z< DSML parameter>" +
            "</ DSML invoke></ DSML calls>"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("extract", calls[0].tool)
        assertEquals("https://example.com/z", calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun stripMarkupDropsUnknownDsmlLineRange() {
        val text = "before\n＜ DSML calls ＞\nsecret payload\n＜ / DSML calls ＞\nafter"
        val visible = ToolCallMarkup.stripMarkup(text)

        assertTrue(visible.contains("before"))
        assertTrue(visible.contains("after"))
        assertFalse(visible.contains("secret"))
        assertFalse(visible.contains("DSML"))
    }

    @Test
    fun stripMarkupRemovesEmptyCodeFenceLeftByToolCall() {
        val text = "before\n```\n" + dsmlSample + "\n```\nafter"
        val visible = ToolCallMarkup.stripMarkup(text)

        assertTrue(visible.contains("before"))
        assertTrue(visible.contains("after"))
        assertFalse(visible.contains("```"))
        assertFalse(visible.contains("DSML"))
    }

    @Test
    fun stripMarkupHandlesZeroWidthCharsInsideTags() {
        val text = "<\u200B｜｜DSML｜｜calls><｜｜DSML｜｜invoke name=\"extract\">" +
            "<｜｜DSML｜｜parameter name=\"url\" string=\"true\">https://example.com/w<｜｜DSML｜｜parameter>" +
            "</｜｜DSML｜｜invoke></｜｜DSML｜｜calls>"
        val calls = ToolCallMarkup.parseDsmlCalls(text)

        assertEquals(1, calls.size)
        assertEquals("https://example.com/w", calls[0].arguments["url"]?.jsonPrimitive?.contentOrNull)
        assertEquals("", ToolCallMarkup.stripMarkup(text))
    }

    @Test
    fun stripMarkupDropsUnclosedDsmlBlock() {
        val text = "before <｜｜DSML｜｜calls><｜｜DSML｜｜invoke name=\"extract\"> after"
        val visible = ToolCallMarkup.stripMarkup(text)

        assertFalse(visible.contains("DSML"))
        assertTrue(visible.contains("before"))
    }

    @Test
    fun stripsCompleteToolResultBlock() {
        val text = "before\n<tool_result>\nsecret table\n</tool_result>\nafter"
        val visible = ToolCallMarkup.stripMarkup(text)

        assertFalse(visible.contains("secret"))
        assertTrue(visible.contains("before"))
        assertTrue(visible.contains("after"))
    }

    @Test
    fun stripsUnclosedToolResultBlock() {
        val text = "before\n<tool_result>\nsecret table"
        val visible = ToolCallMarkup.stripMarkup(text)

        assertFalse(visible.contains("secret"))
        assertTrue(visible.contains("before"))
    }

    @Test
    fun splitStreamingHoldsUnclosedToolResult() {
        val (visible, rest) = ToolCallMarkup.splitStreaming("before <tool_result>\nsecret")
        assertEquals("before ", visible)
        assertTrue(rest.contains("<tool_result>"))
    }

    @Test
    fun splitStreamingRemovesCompleteDsmlBlock() {
        val (visible, rest) = ToolCallMarkup.splitStreaming("before " + dsmlSample + " after")
        assertEquals("before  after", visible)
        assertEquals("", rest)
    }

    @Test
    fun stripMarkupDropsInvalidMcpMarker() {
        val text = "answer [MCP_CALL]{\"server\":\"s\",\"tool\":"
        val visible = ToolCallMarkup.stripMarkup(text)
        assertFalse(visible.contains("[MCP_CALL]"))
        assertTrue(visible.contains("answer"))
    }

    @Test
    fun splitStreamingHidesPartialMarker() {
        val (visible, rest) = ToolCallMarkup.splitStreaming("hello [MCP_")
        assertEquals("hello ", visible)
        assertEquals("[MCP_", rest)
    }

    @Test
    fun splitStreamingHidesUnfinishedDsmlBlock() {
        val partial = "<\uFF5C\uFF5CDSML\uFF5C\uFF5Ccalls><\uFF5C\uFF5CDSML\uFF5C\uFF5Cinvoke name=\"extract\">"
        val (visible, rest) = ToolCallMarkup.splitStreaming("text " + partial)
        assertEquals("text ", visible)
        assertEquals(partial, rest)
    }

    @Test
    fun splitStreamingRemovesCompleteToolCalls() {
        val text = "A[MCP_CALL]{\"server\":\"s\",\"tool\":\"t\",\"arguments\":{}}B"
        val (visible, rest) = ToolCallMarkup.splitStreaming(text)
        assertEquals("AB", visible)
        assertEquals("", rest)
    }
}
