/*
OpenDroidChat MCP Server List Item Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.McpServer

@Composable
fun McpServerListItem(
    server: McpServer,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: (McpServer) -> Unit
) {
    // 与模型列表保持同一套视觉语言：启用中的服务器使用强调色卡片
    val secondaryTextColor = if (server.enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    SettingsCard(
        selected = server.enabled,
        onClick = onEdit
    ) {
        // 开关移到容器最左侧，与模型列表的 RadioButton 位置保持一致
        Switch(
            checked = server.enabled,
            onCheckedChange = onToggleEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (server.enabled) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (server.description.isNotBlank()) {
                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = server.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        SettingsCardIconButton(
            icon = Icons.Filled.Edit,
            onClick = onEdit
        )

        SettingsCardIconButton(
            icon = Icons.Filled.Delete,
            tint = MaterialTheme.colorScheme.error,
            onClick = { onDelete(server) }
        )
    }
}
