package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.data.McpServer
import com.hoeteam.opendroidchat.data.McpTool
import com.hoeteam.opendroidchat.network.LlmApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolExposureTest {

    private val server = McpServer(
        id = "server-1",
        name = "Any Search",
        url = "https://example.com/mcp"
    )

    @Test
    fun sanitizesAndNamespacesToolNames() {
        val tool = McpTool(
            name = "get sub-domains",
            description = "查询子域",
            inputSchema = "{\"type\":\"object\",\"properties\":{\"q\":{\"type\":\"string\"}}}"
        )

        val exposed = LlmApiService().buildExposedTools(listOf(server to listOf(tool)))

        assertEquals(1, exposed.size)
        val definition = exposed.single().definition
        assertEquals("function", definition.type)
        assertTrue(definition.function.name.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        assertTrue(definition.function.name.contains("Any_Search"))
        assertEquals("查询子域", definition.function.description)
    }

    @Test
    fun nonAsciiNamesBecomeAscii() {
        val chineseServer = McpServer(name = "搜索服务", url = "https://example.com/mcp")
        val chineseTool = McpTool(name = "查询")

        val exposed = LlmApiService().buildExposedTools(
            listOf(chineseServer to listOf(chineseTool))
        )

        assertTrue(
            exposed.single().definition.function.name.matches(Regex("[A-Za-z0-9_-]{1,64}"))
        )
    }

    @Test
    fun avoidsDuplicateNames() {
        val first = McpTool(name = "search")
        val second = McpTool(name = "search")
        val exposed = LlmApiService().buildExposedTools(
            listOf(server to listOf(first, second))
        )

        assertEquals(2, exposed.size)
        assertEquals(2, exposed.map { it.exposedName }.toSet().size)
        assertTrue(exposed.all { it.exposedName.length <= 64 })
    }

    @Test
    fun fallsBackToEmptyObjectSchema() {
        val tool = McpTool(name = "no_schema", inputSchema = null)

        val exposed = LlmApiService().buildExposedTools(listOf(server to listOf(tool)))
        val parameters = exposed.single().definition.function.parameters

        assertTrue(parameters != null)
        assertTrue(parameters.toString().contains("\"type\":\"object\""))
    }
}
