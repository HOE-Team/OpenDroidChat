/*
OpenDroidChat Themed Markdown Text Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme

@Composable
fun ThemedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    textColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    codeBackgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
    codeTextColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val textColorArgb = textColor.toArgb()
    val codeBgArgb = codeBackgroundColor.toArgb()
    val codeTextArgb = codeTextColor.toArgb()

    val markwon = remember(codeBgArgb, codeTextArgb) {
        Markwon.builder(context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(codeBgArgb)
                        .codeTextColor(codeTextArgb)
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorArgb)
                if (style.fontSize.isSp) {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
                }
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            markwon.setMarkdown(textView, markdown)
        }
    )
}