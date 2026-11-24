package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.ui.components.MarkdownSegment
import com.hoeteam.opendroidchat.ui.components.splitMarkdownSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSegmentsTest {

    @Test
    fun splitsMarkdownAndCodeBlocks() {
        val text = "说明\n```kotlin\nval a = 1\n```\n结尾"
        val segments = splitMarkdownSegments(text)

        assertEquals(3, segments.size)
        assertTrue(segments[0] is MarkdownSegment.Markdown)
        assertEquals("说明", (segments[0] as MarkdownSegment.Markdown).text)
        assertTrue(segments[1] is MarkdownSegment.Code)
        assertEquals("val a = 1", (segments[1] as MarkdownSegment.Code).text)
        assertTrue(segments[2] is MarkdownSegment.Markdown)
        assertEquals("结尾", (segments[2] as MarkdownSegment.Markdown).text)
    }

    @Test
    fun keepsUnclosedCodeBlockAsCode() {
        val segments = splitMarkdownSegments("前文\n```\nstreaming...")

        assertEquals(2, segments.size)
        assertTrue(segments[1] is MarkdownSegment.Code)
        assertEquals("streaming...", (segments[1] as MarkdownSegment.Code).text)
    }

    @Test
    fun plainTextIsSingleMarkdownSegment() {
        val segments = splitMarkdownSegments("只有正文")

        assertEquals(1, segments.size)
        assertEquals("只有正文", (segments.single() as MarkdownSegment.Markdown).text)
    }

    @Test
    fun emptyTextProducesNoSegments() {
        assertTrue(splitMarkdownSegments("").isEmpty())
    }
}
