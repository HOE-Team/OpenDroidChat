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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.McpCallInfo
import com.hoeteam.opendroidchat.data.McpSegment
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
    Toast.makeText(context, context.getString(com.hoeteam.opendroidchat.R.string.message_copied_to_clipboard), Toast.LENGTH_SHORT).show()
}

/**
 * 渲染一条消息。
 *
 * 之前通过参数传入 Markdown 渲染 lambda，导致列表项无法被 Compose 跳过，
 * 每次流式刷新所有可见气泡都会整体重组；现在改为内部实现，
 * 稳定性由参数类型保证，未变化的消息可直接跳过重组。
 */
@Composable
fun MessageBubble(
    message: Message,
    isEditing: Boolean = false,
    editingText: String = "",
    onEditTextChange: (String) -> Unit = {},
    onSaveEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onEdit: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copyState by remember { mutableStateOf(CopyState.Idle) }

    val isUser = message.sender == Sender.USER

    // 只依赖文本本身，流式刷新时不会因为其他字段变化而重复拼接
    val copyableText = remember(message.text, message.segments) {
        buildString {
            message.segments.filterIsInstance<McpSegment.Text>().forEach { seg ->
                if (seg.text.isNotBlank()) {
                    append(seg.text)
                    append("\n\n")
                }
            }
            append(message.text)
        }.trim()
    }
    val bubbleColor = when {
        isEditing -> MaterialTheme.colorScheme.surfaceContainerHigh
        isUser -> MaterialTheme.colorScheme.primary
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        isEditing -> MaterialTheme.colorScheme.onSurfaceVariant
        isUser -> MaterialTheme.colorScheme.onPrimary
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // M3E 标志性非对称大气圆角
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 60.dp else 8.dp,
                end = if (isUser) 8.dp else 60.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // 消息气泡 - 宽度自适应内容
            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                shadowElevation = 1.dp
            ) {
                if (isUser) {
                    Column(
                        modifier = Modifier.widthIn(min = 60.dp)
                    ) {
                        if (isEditing) {
                            // 编辑模式：将聊天框变为输入框
                            OutlinedTextField(
                                value = editingText,
                                onValueChange = onEditTextChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                placeholder = { Text(stringResource(com.hoeteam.opendroidchat.R.string.message_edit_placeholder)) },
                                singleLine = false,
                                maxLines = 5
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onCancelEdit) {
                                    Text(stringResource(com.hoeteam.opendroidchat.R.string.message_edit_cancel))
                                }
                                TextButton(
                                    onClick = onSaveEdit,
                                    enabled = editingText.isNotBlank() && !isLoading
                                ) {
                                    Text(stringResource(com.hoeteam.opendroidchat.R.string.message_edit_save))
                                }
                            }
                        } else {
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            // 用户消息底部显示文件 Chip（如果有）
                            if (message.selectedFile != null) {
                                FileInfoChip(
                                    fileName = message.selectedFile.fileName,
                                    isCodeFile = message.selectedFile.isCodeFile,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, end = 12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.widthIn(min = 60.dp)) {
                        // 按发生顺序堆叠渲染思考块与 MCP 调用组
                        message.segments.forEach { seg ->
                            when (seg) {
                                is McpSegment.Thinking -> ThinkingBlock(reasoningText = seg.text, isStreaming = false)
                                is McpSegment.Text -> if (seg.text.isNotBlank()) {
                                    MessageMarkdown(
                                        text = seg.text,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        textColor = textColor
                                    )
                                }
                                is McpSegment.Calls -> if (seg.calls.isNotEmpty()) McpCallsBlock(calls = seg.calls)
                            }
                        }

                        // 当前思考内容（进行中的思考）
                        if (!message.reasoningText.isNullOrBlank()) {
                            ThinkingBlock(
                                reasoningText = message.reasoningText,
                                isStreaming = message.isThinkingStreaming
                            )
                        }


                        // 常规内容
                        if (message.text.isNotBlank()) {
                            MessageMarkdown(
                                text = message.text,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = if (message.reasoningText.isNullOrBlank()) 12.dp else 8.dp
                                ),
                                textColor = textColor
                            )
                        }
                    }
                }
            }

            // 消息下方操作按钮（编辑 / 重新生成 / 复制）
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                // 用户消息：编辑按钮（点击后变为输入框进行修改/重写）
                if (isUser && !isEditing && !isLoading) {
                    MessageActionIconButton(
                        onClick = onEdit
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(com.hoeteam.opendroidchat.R.string.message_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // AI 消息：重新生成按钮
                if (!isUser && !message.text.contains("LLM 响应出错") && !isLoading) {
                    MessageActionIconButton(
                        onClick = onRegenerate
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(com.hoeteam.opendroidchat.R.string.message_regenerate),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // 复制按钮（带 M3E 物理反馈）
                MessageActionIconButton(
                    onClick = {
                        scope.launch {
                            copyToClipboard(context, copyableText)
                            copyState = CopyState.Copied
                            delay(2000)
                            copyState = CopyState.Idle
                        }
                    },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = when (copyState) {
                            CopyState.Idle -> Icons.Default.ContentCopy
                            CopyState.Copied -> Icons.Default.DoneAll
                        },
                        contentDescription = null,
                        tint = if (copyState == CopyState.Copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 消息下方操作按钮：每个按钮拥有独立的 interactionSource，
 * 避免悬停/按压时多个按钮同时显示高亮或阴影。
 */
@Composable
private fun MessageActionIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "MessageActionScale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(36.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
            icon()
        }
    }
}

/** 消息正文的 Markdown 渲染（含代码块高亮样式）。 */
@Composable
private fun MessageMarkdown(
    text: String,
    modifier: Modifier,
    textColor: Color
) {
    HybridMarkdown(
        text = text,
        modifier = modifier,
        normalTextColor = textColor,
        themedMarkdownText = { md, mod, txtColor, codeBg, codeTxtColor ->
            ThemedMarkdownText(
                markdown = md,
                modifier = mod,
                textColor = txtColor,
                codeBackgroundColor = codeBg,
                codeTextColor = codeTxtColor
            )
        }
    )
}

@Composable
private fun ThinkingBlock(
    reasoningText: String,
    isStreaming: Boolean
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 标题栏 - 可折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isStreaming) stringResource(com.hoeteam.opendroidchat.R.string.message_thinking_in_progress) else stringResource(com.hoeteam.opendroidchat.R.string.message_thinking_complete),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 思考内容
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = reasoningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FileInfoChip(
    fileName: String,
    isCodeFile: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
        modifier = modifier.height(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp)
        ) {
            Icon(
                imageVector = if (isCodeFile) Icons.Filled.Code else Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun McpCallsBlock(calls: List<McpCallInfo>) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 标题栏 - 可折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(
                        com.hoeteam.opendroidchat.R.string.message_mcp_tools_title,
                        calls.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 调用详情
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    calls.forEach { call ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (call.pending) {
                                // 调用进行中：显示加载转圈
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = context.getString(
                                        com.hoeteam.opendroidchat.R.string.message_mcp_loading,
                                        call.serverName,
                                        call.toolName
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            } else if (call.error != null) {
                                Icon(
                                    Icons.Filled.Warning,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${call.toolName} · ${call.serverName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = call.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    )
                                }
                            } else {
                                Text(
                                    text = context.getString(
                                        com.hoeteam.opendroidchat.R.string.message_mcp_call_item,
                                        call.serverName,
                                        call.toolName,
                                        call.durationMs
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

