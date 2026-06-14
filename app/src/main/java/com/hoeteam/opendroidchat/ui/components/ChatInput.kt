/*
OpenDroidChat Chat Input Component - M3 Expressive Refactored (Circular Nested & Transparent Background)
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isFabEnabled = text.isNotBlank() && !isLoading

    // 【核心修正】：彻底去掉了外层的 Surface 容器和 shadowElevation，改用无包围的纯透明 Box 进行手势与系统栏对齐
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // 保持底部全面屏虚拟导航栏的安全垫底
            .padding(horizontal = 16.dp, vertical = 12.dp)  // 适度增加外边距，让全圆角胶囊舱在悬浮时两端更舒展
    ) {
        // 原汁原味的 OutlinedTextField，按钮完美的内嵌在 trailingIcon 内部
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("输入消息...") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (isFabEnabled) onSend() }),
            singleLine = false,
            maxLines = 5,
            enabled = !isLoading,
            shape = CircleShape, // 两端完美的半圆胶囊线

            trailingIcon = {
                // 按钮弹性缩放物理反馈
                val fabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else if (isFabEnabled) 1f else 0.85f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy)
                )

                // 按钮圆角状态动画：加载中为矩形，平时为正圆
                val fabCorner by animateDpAsState(
                    targetValue = if (isLoading) 10.dp else 18.dp, // 18.dp 刚好实现 36.dp 按钮的正圆
                    animationSpec = spring(Spring.DampingRatioLowBouncy)
                )

                // 嵌套在输入框内部的 FloatingActionButton
                FloatingActionButton(
                    onClick = { if (isFabEnabled) onSend() },
                    containerColor = if (isFabEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFabEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(fabCorner),
                    modifier = Modifier
                        .padding(end = 4.dp) // 精致的右内边距
                        .size(36.dp)       // 精致的嵌套尺寸
                        .scale(fabScale),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    interactionSource = interactionSource
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5f.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        )
    }
}