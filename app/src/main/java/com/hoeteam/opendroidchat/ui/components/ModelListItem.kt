/*
OpenDroidChat Model List Item Component
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.LlmModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListItem(
    model: LlmModel,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: (LlmModel) -> Unit,
    allModelsCount: Int
) {
    val isDeletable = allModelsCount > 1
    val canBeDeleted = isDeletable && !isSelected
    val secondaryTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    SettingsCard(
        selected = isSelected,
        onClick = { onSelect(model.id) }
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(model.id) },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
            )
            Text(
                text = "API: ${model.provider.displayName} | 模型: ${model.modelName}",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }

        SettingsCardIconButton(
            icon = Icons.Filled.Edit,
            onClick = onEdit
        )

        if (canBeDeleted) {
            SettingsCardIconButton(
                icon = Icons.Filled.Delete,
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDelete(model) }
            )
        } else if (isDeletable && isSelected) {
            // 当前选中的模型不允许直接删除，避免聊天中途失去配置
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(com.hoeteam.opendroidchat.R.string.model_list_switch_first))
                    }
                },
                state = rememberTooltipState()
            ) {
                SettingsCardIconButton(
                    icon = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}
