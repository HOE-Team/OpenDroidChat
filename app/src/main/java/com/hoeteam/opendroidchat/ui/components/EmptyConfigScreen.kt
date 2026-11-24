/*
OpenDroidChat Empty Config Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SpeakerNotesOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.R

/**
 * 完全没有配置模型时的主页状态。
 *
 * 与模型列表页（ModelSettingsScreen）的空状态完全同构：
 * 圆形图标容器 + 大标题 + 说明文字 + 药丸形主操作按钮。
 */
@Composable
fun EmptyConfigScreen(onNavigateToModelSettings: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.SpeakerNotesOff,
        title = stringResource(R.string.chat_empty_config_title),
        description = stringResource(R.string.chat_empty_config_desc),
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(24.dp))
        M3EPillButton(
            text = stringResource(R.string.chat_empty_config_button),
            onClick = onNavigateToModelSettings,
            icon = Icons.AutoMirrored.Filled.ArrowForward
        )
    }
}
