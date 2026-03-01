/*
OpenDroidChat Hybrid Markdown Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun HybridMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    normalTextColor: Color = Color.Unspecified,
    themedMarkdownText: @Composable (String, Modifier, Color, Color, Color) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val codeBackground = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val codeTextColor = if (isDark) Color(0xFFCE9178) else Color(0xFFB00020)

    Column(modifier = modifier) {
        var insideCodeBlock = false
        val normalBuffer = mutableListOf<String>()
        val lines = text.lines()

        lines.forEach { line ->
            if (line.trim().startsWith("```")) {
                if (insideCodeBlock) {
                    Surface(
                        color = codeBackground,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = normalBuffer.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            color = codeTextColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    normalBuffer.clear()
                } else {
                    if (normalBuffer.isNotEmpty()) {
                        val content = normalBuffer.joinToString("\n")
                        themedMarkdownText(
                            content,
                            Modifier.fillMaxWidth(),
                            normalTextColor,
                            codeBackground,
                            codeTextColor
                        )
                        normalBuffer.clear()
                    }
                }
                insideCodeBlock = !insideCodeBlock
            } else {
                normalBuffer.add(line)
            }
        }

        if (normalBuffer.isNotEmpty()) {
            if (insideCodeBlock) {
                Surface(
                    color = codeBackground,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = normalBuffer.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        color = codeTextColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                themedMarkdownText(
                    normalBuffer.joinToString("\n"),
                    Modifier.fillMaxWidth(),
                    normalTextColor,
                    codeBackground,
                    codeTextColor
                )
            }
        }
    }
}