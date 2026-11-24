/*
OpenDroidChat Markdown Segments
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

/** 一段 Markdown 正文，或被 ``` 围起来的一段代码。 */
internal sealed class MarkdownSegment {
    data class Markdown(val text: String) : MarkdownSegment()
    data class Code(val text: String) : MarkdownSegment()
}

/**
 * 把消息正文切分成「Markdown 正文 / 代码块」若干段。
 *
 * 结果是纯数据，调用方可以用 remember(text) 缓存，避免每次重组都重新切分。
 */
internal fun splitMarkdownSegments(text: String): List<MarkdownSegment> {
    if (text.isEmpty()) return emptyList()

    val segments = mutableListOf<MarkdownSegment>()
    val buffer = mutableListOf<String>()
    var insideCodeBlock = false

    fun flushNonEmpty() {
        if (buffer.isNotEmpty()) {
            segments += MarkdownSegment.Markdown(buffer.joinToString("\n"))
            buffer.clear()
        }
    }

    text.lineSequence().forEach { line ->
        if (line.trim().startsWith("```")) {
            if (insideCodeBlock) {
                segments += MarkdownSegment.Code(buffer.joinToString("\n"))
                buffer.clear()
            } else {
                flushNonEmpty()
            }
            insideCodeBlock = !insideCodeBlock
        } else {
            buffer.add(line)
        }
    }

    if (buffer.isNotEmpty()) {
        segments += if (insideCodeBlock) {
            MarkdownSegment.Code(buffer.joinToString("\n"))
        } else {
            MarkdownSegment.Markdown(buffer.joinToString("\n"))
        }
    }
    return segments
}
