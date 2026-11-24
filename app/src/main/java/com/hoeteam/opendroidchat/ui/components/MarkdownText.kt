/*
OpenDroidChat Markdown Text Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

/**
 * 消息正文 / 思考内容的 Markdown 渲染。
 *
 * 使用 multiplatform-markdown-renderer：
 * - 纯 Compose 绘制（不再走 AndroidView + TextView，避免每次重组都做 View 测量）
 * - `-code` 模块提供代码块语法高亮（Highlights）
 * - `-coil3` 模块负责 Markdown 内图片加载
 *
 * [retainState] 为 true 时，内容变化会保留上一帧结果，
 * 流式输出不会闪成“加载中”。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    baseStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    retainState: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val markdownState = rememberMarkdownState(
        markdown,
        retainState = retainState
    )
    val highlightsBuilder = remember(isDark) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDark))
    }
    val components = remember(highlightsBuilder) {
        markdownComponents(
            codeBlock = {
                MarkdownHighlightedCodeBlock(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightsBuilder
                )
            },
            codeFence = {
                MarkdownHighlightedCodeFence(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightsBuilder
                )
            }
        )
    }

    Markdown(
        markdownState = markdownState,
        colors = markdownColor(
            text = textColor,
            codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
            inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        typography = markdownTypography(
            text = baseStyle,
            paragraph = baseStyle,
            h1 = MaterialTheme.typography.headlineSmall,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.titleSmall,
            h5 = MaterialTheme.typography.bodyLarge,
            h6 = MaterialTheme.typography.bodyMedium,
            code = baseStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            inlineCode = baseStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        ),
        imageTransformer = Coil3ImageTransformerImpl,
        components = components,
        modifier = modifier
    )
}
