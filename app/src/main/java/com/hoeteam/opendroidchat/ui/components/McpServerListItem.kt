/*
OpenDroidChat MCP Server List Item Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.McpServer

/**
 * MCP 服务器列表项：与模型列表共用 M3E 描边卡片。
 */
@Composable
fun McpServerListItem(
    server: McpServer,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: (McpServer) -> Unit
) {
    M3ECard(
        onClick = onEdit,
        selected = server.enabled,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        M3ECardRow {
            // 开关与模型列表的 RadioButton 位置保持一致
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
