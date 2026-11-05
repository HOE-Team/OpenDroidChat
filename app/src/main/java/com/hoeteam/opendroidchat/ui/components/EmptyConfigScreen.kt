/*
OpenDroidChat Empty Config Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SpeakerNotesOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoeteam.opendroidchat.R

/**
 * 完全没有配置模型时的主页空状态。
 *
 * 空状态部分与「模型列表 / MCP 列表」共用 [EmptyState] 的
 * 「大图标 + 大标题 + 小文字」设计，下方保留跳转到模型管理的 XL 按钮。
 */
@Composable
fun EmptyConfigScreen(onNavigateToModelSettings: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // M3E 弹性微动：超大按钮按下时的物理反馈
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "XLButtonScale"
    )

    EmptyState(
        icon = Icons.Filled.SpeakerNotesOff,
        title = stringResource(R.string.chat_empty_config_title),
        description = stringResource(R.string.chat_empty_config_desc),
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(40.dp))

        // M3E Extra Large Button (XL Button)：跳转到模型管理，逻辑保持不变
        Button(
            onClick = onNavigateToModelSettings,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(64.dp) // XL 按钮高度
                .scale(scale),
            shape = RoundedCornerShape(20.dp), // M3E 标志性大圆角
            contentPadding = PaddingValues(horizontal = 24.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = stringResource(R.string.chat_empty_config_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
