/*
OpenDroidChat Message Bubble Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.Message
import com.hoeteam.opendroidchat.data.Sender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 复制状态枚举
enum class CopyState {
    Idle,      // 未复制
    Copied     // 已复制
}

// 复制到剪贴板的辅助函数
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("聊天内容", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

@Composable
fun MessageBubble(
    message: Message,
    hybridMarkdown: @Composable (String, Modifier, androidx.compose.ui.graphics.Color) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copyState by remember { mutableStateOf(CopyState.Idle) }

    val isUser = message.sender == Sender.USER
    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // M3 Expressive: 使用较大的圆角 (24dp - 28dp)
    val cornerSize = 24.dp
    val bubbleShape = MaterialTheme.shapes.extraLarge.copy(
        bottomEnd = if (isUser) CornerSize(4.dp) else CornerSize(cornerSize),
        bottomStart = if (!isUser) CornerSize(4.dp) else CornerSize(cornerSize)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp
            ),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                border = if (!isUser) CardDefaults.outlinedCardBorder().copy(width = 0.5.dp) else null
            ) {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        hybridMarkdown(
                            message.text,
                            Modifier,
                            textColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .widthIn(min = 32.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            copyToClipboard(context, message.text)
                            copyState = CopyState.Copied
                            delay(2000)
                            copyState = CopyState.Idle
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = when (copyState) {
                            CopyState.Idle -> Icons.Default.ContentCopy
                            CopyState.Copied -> Icons.Default.DoneAll
                        },
                        contentDescription = if (copyState == CopyState.Idle) "复制内容" else "已复制",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}