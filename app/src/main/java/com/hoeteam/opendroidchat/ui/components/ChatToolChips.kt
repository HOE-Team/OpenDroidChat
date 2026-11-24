/*
OpenDroidChat Chat Tool Chips
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.R

/**
 * 聊天输入框上方的快捷开关：思考 / 思考强度 / MCP。
 *
 * - 药丸形 Chip，开启时使用主色描边与容器色
 * - 思考强度只在思考开启且模型支持时出现
 */
@Composable
fun ChatToolChips(
    thinkingSupported: Boolean,
    thinkingEnabled: Boolean,
    onThinkingChange: (Boolean) -> Unit,
    effort: String?,
    effortOptions: List<String>,
    onEffortChange: (String) -> Unit,
    mcpAvailable: Boolean,
    mcpEnabled: Boolean,
    onMcpChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 用 FlowRow 自动换行：chip 数量多/屏幕窄时不会再被水平滚动裁掉右半边
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (thinkingSupported) {
            ToolChip(
                label = stringResource(R.string.chat_chip_thinking),
                icon = Icons.Outlined.Psychology,
                active = thinkingEnabled,
                onClick = { onThinkingChange(!thinkingEnabled) }
            )

            if (thinkingEnabled && effortOptions.isNotEmpty()) {
                EffortChip(
                    effort = effort ?: effortOptions.first(),
                    options = effortOptions,
                    onSelect = onEffortChange
                )
            }
        }

        if (mcpAvailable) {
            ToolChip(
                label = stringResource(R.string.chat_chip_mcp),
                icon = Icons.Filled.Storage,
                active = mcpEnabled,
                onClick = { onMcpChange(!mcpEnabled) }
            )
        }
    }
}

/** 基础药丸开关 */
@Composable
private fun ToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val container = if (active) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val border = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val stateText = stringResource(
        if (active) R.string.chip_on else R.string.chip_off
    )

    Surface(
        shape = M3E_PILL_SHAPE,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stateText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 思考强度选择 Chip（带下拉菜单） */
@Composable
private fun EffortChip(
    effort: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = M3E_PILL_SHAPE,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = true }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.chat_chip_effort),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = effortLabel(effort),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = effortLabel(option),
                            fontWeight = if (option == effort) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (option == effort) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun effortLabel(value: String): String = when (value) {
    "low" -> stringResource(R.string.model_edit_effort_low)
    "medium" -> stringResource(R.string.model_edit_effort_medium)
    "high" -> stringResource(R.string.model_edit_effort_high)
    "max" -> stringResource(R.string.model_edit_effort_max)
    "xhigh" -> stringResource(R.string.model_edit_effort_xhigh)
    else -> value
}
