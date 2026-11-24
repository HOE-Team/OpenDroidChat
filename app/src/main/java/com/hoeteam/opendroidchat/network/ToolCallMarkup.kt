/*
OpenDroidChat Tool Call Markup
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import kotlinx.serialization.json.*

/**
 * 模型输出中可能包含的工具调用标记：
 *
 * 1. 本应用约定的文本协议：[MCP_CALL]{"server":"...","tool":"...","arguments":{...}}
 * 2. DeepSeek 的 DSML 标记：<｜｜DSML｜｜calls>...<｜｜DSML｜｜invoke name="...">...</｜｜DSML｜｜calls>
 *
 * 本对象负责三件事：
 * - 从模型输出中解析工具调用；
 * - 把工具调用标记从展示文本中剔除（支持流式分块）；
 * - 判断流式输出末尾需要暂存多少字符，避免标记漏到界面上。
 */
object ToolCallMarkup {

    const val MCP_MARKER = "[MCP_CALL]"

    // DSML 标记在不同模型/字体下可能带空格、全角竖线或全角尖括号，
    // 因此这里对 < ＞、竖线数量、空白都做兼容：
    //   <｜｜DSML｜｜calls>   < | | DSML | | calls >   ＜｜｜DSML｜｜calls＞
    private val dsmlTagRegex = Regex(
        "[<＜]\\s*(/?)\\s*(?:[|｜]\\s*)+DSML\\s*(?:[|｜]\\s*)+([A-Za-z]+)([^>＞]*)[>＞]"
    )
    private val dsmlCallsOpenRegex = Regex(
        "[<＜]\\s*(?:[|｜]\\s*)+DSML\\s*(?:[|｜]\\s*)+calls\\s*[>＞]"
    )
    private val dsmlCallsCloseRegex = Regex(
        "[<＜]\\s*/\\s*(?:[|｜]\\s*)+DSML\\s*(?:[|｜]\\s*)+calls\\s*[>＞]"
    )
    private val nameAttrRegex = Regex("name\\s*=\\s*\"([^\"]*)\"")
    private val stringAttrRegex = Regex("string\\s*=\\s*\"([^\"]*)\"")

    // 部分模型会用 <tool_result> / <tool_call> 等标签承载工具协议内容，
    // 这些属于内部数据，不应出现在聊天气泡里
    private const val PROTOCOL_TAGS =
        "tool_result|tool_response|tool_call|tool_use|function_call|function_result|observation"
    private val protocolBlockRegex = Regex(
        "<(?:__TAGS__)(?:\\s[^>]*)?>[\\s\\S]*?</(?:__TAGS__)\\s*>".replace("__TAGS__", PROTOCOL_TAGS),
        RegexOption.IGNORE_CASE
    )
    private val protocolOpenRegex = Regex(
        "<(?:__TAGS__)(?:\\s[^>]*)?>".replace("__TAGS__", PROTOCOL_TAGS),
        RegexOption.IGNORE_CASE
    )
    private val protocolCloseRegex = Regex(
        "</(?:__TAGS__)\\s*>".replace("__TAGS__", PROTOCOL_TAGS),
        RegexOption.IGNORE_CASE
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 从文本中提取所有完整的 "[MCP_CALL]{...}" 块（含前缀）。
     * 使用平衡花括号扫描器，支持任意嵌套（对象/数组/转义字符串）。
     */
    fun extractMcpCallBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val start = text.indexOf(MCP_MARKER, searchFrom)
            if (start < 0) break
            var braceStart = start + MCP_MARKER.length
            while (braceStart < text.length && text[braceStart].isWhitespace()) braceStart++
            if (braceStart >= text.length || text[braceStart] != '{') {
                searchFrom = start + 1
                continue
            }
            var depth = 0
            var inString = false
            var escaped = false
            var end = -1
            for (j in braceStart until text.length) {
                val c = text[j]
                if (inString) {
                    if (escaped) escaped = false
                    else if (c == '\\') escaped = true
                    else if (c == '"') inString = false
                } else {
                    when (c) {
                        '"' -> inString = true
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                end = j
                                break
                            }
                        }
                    }
                }
            }
            if (end < 0) {
                searchFrom = start + 1
                continue
            }
            blocks += text.substring(start, end + 1)
            searchFrom = end + 1
        }
        return blocks
    }

    /** 解析本应用约定的 [MCP_CALL] 工具调用。 */
    fun parseMcpCalls(text: String): List<McpToolCall> {
        val result = mutableListOf<McpToolCall>()
        for (block in extractMcpCallBlocks(text)) {
            val jsonText = block.substringAfter(MCP_MARKER).trim()
            try {
                val obj = json.parseToJsonElement(jsonText).jsonObject
                val server = obj["server"]?.jsonPrimitive?.contentOrNull ?: continue
                val tool = obj["tool"]?.jsonPrimitive?.contentOrNull ?: continue
                val args = (obj["arguments"] as? JsonObject)?.mapValues { it.value } ?: emptyMap()
                result += McpToolCall(server = server, tool = tool, arguments = args)
            } catch (_: Exception) {
                // 忽略解析失败的标记
            }
        }
        return result
    }

    /**
     * 解析 DeepSeek DSML 工具调用。DSML 只带工具名，不带服务器名，
     * 需要调用方按工具名自行匹配 MCP 服务器。
     */
    fun parseDsmlCalls(text: String): List<RawDsmlToolCall> {
        val strict = parseDsmlCallsStrict(text)
        if (strict.isNotEmpty()) return strict
        // 标记被模型改写（缺尖括号、异常空格）时退化为关键字解析
        return parseDsmlCallsLoose(text)
    }

    private fun parseDsmlCallsStrict(text: String): List<RawDsmlToolCall> {
        val calls = mutableListOf<RawDsmlToolCall>()
        var inCalls = false
        var currentTool: String? = null
        var args = linkedMapOf<String, JsonElement>()
        var pendingParam: String? = null
        var pendingIsString = false

        fun flushParameter(raw: String) {
            val name = pendingParam ?: return
            val value = raw.trim()
            args[name] = if (pendingIsString) {
                JsonPrimitive(value)
            } else {
                runCatching { json.parseToJsonElement(value) }.getOrElse { JsonPrimitive(value) }
            }
            pendingParam = null
        }

        fun finishTool() {
            val tool = currentTool
            if (!tool.isNullOrBlank()) {
                calls += RawDsmlToolCall(tool, args.toMap())
            }
            currentTool = null
            args = linkedMapOf()
            pendingParam = null
        }

        var lastEnd = 0
        for (match in dsmlTagRegex.findAll(text)) {
            val between = text.substring(lastEnd, match.range.first)
            // 标签之间的文本属于上一段参数值
            if (pendingParam != null) flushParameter(between)

            val closing = match.groupValues[1] == "/"
            val tag = match.groupValues[2].lowercase()
            val attrs = match.groupValues[3]

            when (tag) {
                "calls" -> {
                    if (!inCalls) {
                        inCalls = true
                    } else {
                        finishTool()
                        inCalls = false
                    }
                }
                "invoke" -> if (inCalls) {
                    val name = nameAttrRegex.find(attrs)?.groupValues?.get(1)
                    if (name != null && !closing) {
                        finishTool()
                        currentTool = name
                    } else {
                        finishTool()
                    }
                }
                "parameter" -> if (inCalls && currentTool != null) {
                    val name = nameAttrRegex.find(attrs)?.groupValues?.get(1)
                    if (name != null && !closing) {
                        pendingParam = name
                        pendingIsString = stringAttrRegex.find(attrs)?.groupValues?.get(1) == "true"
                    } else {
                        flushParameter(between)
                    }
                }
            }
            lastEnd = match.range.last + 1
        }

        if (inCalls && currentTool != null) {
            if (pendingParam != null) flushParameter(text.substring(lastEnd))
            finishTool()
        }
        return calls
    }

    /**
     * 兜底解析：即使 DSML 外层标记不完整，只要还能看到
     * invoke name="..." / parameter name="..." 就尽量把调用还原出来。
     */
    private fun parseDsmlCallsLoose(text: String): List<RawDsmlToolCall> {
        if (!text.contains("DSML", ignoreCase = true)) return emptyList()

        val result = mutableListOf<RawDsmlToolCall>()
        val invokeRegex = Regex("invoke\\s+name\\s*=\\s*\"([^\"]+)\"")
        val invokes = invokeRegex.findAll(text).toList()
        for ((index, match) in invokes.withIndex()) {
            val tool = match.groupValues[1]
            val regionStart = match.range.last + 1
            val regionEnd = invokes.getOrNull(index + 1)?.range?.first ?: text.length
            val region = text.substring(regionStart, regionEnd)

            val args = linkedMapOf<String, JsonElement>()
            Regex("parameter\\s+name\\s*=\\s*\"([^\"]+)\"([^>＞]*)[>＞]([^<＜]*)")
                .findAll(region)
                .forEach { param ->
                    val name = param.groupValues[1]
                    val attrs = param.groupValues[2]
                    val raw = param.groupValues[3].trim()
                    val isString = stringAttrRegex.find(attrs)?.groupValues?.get(1) == "true"
                    args[name] = if (isString) {
                        JsonPrimitive(raw)
                    } else {
                        runCatching { json.parseToJsonElement(raw) }.getOrElse { JsonPrimitive(raw) }
                    }
                }
            result += RawDsmlToolCall(tool, args.toMap())
        }
        return result
    }

    /** 移除所有工具调用标记，得到可展示的纯文本。 */
    fun stripMarkup(text: String): String {
        if (text.isEmpty()) return text
        var out = cleanInvisible(text)
        for (block in extractMcpCallBlocks(out)) {
            out = out.replace(block, "")
        }
        out = removeDsmlCalls(out)
        // 兜底：即使标签形状无法配对，也不能把带 DSML 的整块内容直接显示出来。
        // 必须在剔除残留标签之前判断，否则标签没了、参数值却会留在正文里。
        if (out.contains("DSML", ignoreCase = true)) {
            out = removeDsmlLineRange(out)
        }
        out = dsmlTagRegex.replace(out, "")
        // <tool_result> / <tool_call> 等协议标签属于内部数据，整块移除；
        // 未闭合时从标签开始丢弃到末尾，避免把工具原始数据当正文展示
        out = protocolBlockRegex.replace(out, "")
        val openProtocol = protocolOpenRegex.find(out)
        if (openProtocol != null) {
            out = out.substring(0, openProtocol.range.first)
        }
        out = protocolCloseRegex.replace(out, "")
        // 处理无法配对/解析的残标记，避免 “[MCP_CALL]{" 之类的内容漏到界面
        out = Regex("\\[MCP_CALL\\][^\\n]*").replace(out, "")
        // 模型常把工具标记包在代码块里，清理后避免留下空的代码块
        out = Regex("```[A-Za-z0-9_-]*\\s*\\n?\\s*```").replace(out, "")
        return out.trim()
    }

    /**
     * 流式输出切分：返回（可立即展示的文本，需要继续暂存的尾部）。
     * 已完整的工具调用块会被移除；未闭合的标记会暂存，避免漏到界面。
     */
    fun splitStreaming(text: String): Pair<String, String> {
        if (text.isEmpty()) return "" to ""

        var cleaned = cleanInvisible(text)
        for (block in extractMcpCallBlocks(cleaned)) {
            cleaned = cleaned.replace(block, "")
        }
        cleaned = removeCompleteDsmlCalls(cleaned)
        cleaned = protocolBlockRegex.replace(cleaned, "")

        var holdFrom = cleaned.length
        val lastMcp = cleaned.lastIndexOf(MCP_MARKER)
        if (lastMcp >= 0) holdFrom = minOf(holdFrom, lastMcp)

        var hasUnclosedDsmlBlock = false
        val open = dsmlCallsOpenRegex.findAll(cleaned).lastOrNull()
        if (open != null) {
            val close = dsmlCallsCloseRegex.find(cleaned, open.range.last + 1)
            if (close == null) {
                holdFrom = minOf(holdFrom, open.range.first)
                hasUnclosedDsmlBlock = true
            }
        }

        // 未知格式的 DSML：从包含 DSML 的那一行开始暂存，交给收尾的 stripMarkup 清理
        if (!hasUnclosedDsmlBlock) {
            val unknownDsml = cleaned.indexOf("DSML", ignoreCase = true)
            if (unknownDsml >= 0) {
                val lineStart = cleaned.lastIndexOf('\n', unknownDsml).let { if (it < 0) 0 else it + 1 }
                holdFrom = minOf(holdFrom, lineStart)
            }
        }

        // 未闭合的 <tool_result> / <tool_call> 等协议标签：暂存到块结束
        val openProtocol = protocolOpenRegex.find(cleaned)
        if (openProtocol != null) {
            val close = protocolCloseRegex.find(cleaned, openProtocol.range.last + 1)
            if (close == null) holdFrom = minOf(holdFrom, openProtocol.range.first)
        }

        val prefixHold = prefixHoldLength(cleaned)
        val end = minOf(holdFrom, cleaned.length - prefixHold).coerceAtLeast(0)
        return cleaned.substring(0, end) to cleaned.substring(end)
    }

    /** 文本末尾可能是工具标记前缀时，需要暂存的字符数。 */
    fun holdBackLength(text: String): Int {
        val lastMcp = text.lastIndexOf(MCP_MARKER)
        if (lastMcp >= 0) return text.length - lastMcp

        val open = dsmlCallsOpenRegex.findAll(text).lastOrNull()
        if (open != null) {
            val close = dsmlCallsCloseRegex.find(text, open.range.last + 1)
            if (close == null) return text.length - open.range.first
        }
        val unknownDsml = text.indexOf("DSML", ignoreCase = true)
        if (unknownDsml >= 0) {
            val lineStart = text.lastIndexOf('\n', unknownDsml).let { if (it < 0) 0 else it + 1 }
            return text.length - lineStart
        }
        return prefixHoldLength(text)
    }

    /** 去掉零宽字符，避免模型输出的不可见字符把标记拆散。 */
    private fun cleanInvisible(text: String): String = text
        .replace("\u200B", "")
        .replace("\u200C", "")
        .replace("\u200D", "")
        .replace("\uFEFF", "")

    /** 兜底清理：删除包含 DSML 的整段行区间（用于形状无法识别的标记）。 */
    private fun removeDsmlLineRange(text: String): String {
        val lines = text.split("\n")
        val first = lines.indexOfFirst { it.contains("DSML", ignoreCase = true) }
        if (first < 0) return text
        val last = lines.indexOfLast { it.contains("DSML", ignoreCase = true) }
        return (lines.subList(0, first) + lines.subList(last + 1, lines.size)).joinToString("\n")
    }

    private fun prefixHoldLength(text: String): Int {
        val markers = listOf(
            MCP_MARKER,
            "<||DSML||calls>",
            "<｜｜DSML｜｜calls>",
            "<tool_result>", "<tool_response>", "<tool_call>",
            "<tool_use>", "<function_call>", "<function_result>", "<observation>"
        )
        var hold = 0
        for (marker in markers) {
            val limit = minOf(marker.length - 1, text.length)
            for (len in limit downTo 1) {
                if (text.endsWith(marker.substring(0, len))) {
                    hold = maxOf(hold, len)
                    break
                }
            }
        }
        return hold
    }

    /** 移除已闭合的 DSML calls 块，未闭合的部分保留给流式暂存逻辑。 */
    private fun removeCompleteDsmlCalls(text: String): String {
        val sb = StringBuilder()
        var index = 0
        while (true) {
            val open = dsmlCallsOpenRegex.find(text, index) ?: break
            val close = dsmlCallsCloseRegex.find(text, open.range.last + 1)
            if (close == null) break
            sb.append(text, index, open.range.first)
            index = close.range.last + 1
        }
        sb.append(text, index, text.length)
        return sb.toString()
    }

    /** 移除所有 DSML calls 块；未闭合时丢弃到文本末尾，避免半截标记泄露。 */
    private fun removeDsmlCalls(text: String): String {
        val sb = StringBuilder()
        var index = 0
        while (true) {
            val open = dsmlCallsOpenRegex.find(text, index) ?: break
            sb.append(text, index, open.range.first)
            val close = dsmlCallsCloseRegex.find(text, open.range.last + 1)
            if (close == null) {
                index = text.length
                break
            }
            index = close.range.last + 1
        }
        sb.append(text, index, text.length)
        return sb.toString()
    }
}

/** DSML 解析结果：只包含工具名与参数，服务器由调用方匹配。 */
data class RawDsmlToolCall(
    val tool: String,
    val arguments: Map<String, JsonElement>
)
