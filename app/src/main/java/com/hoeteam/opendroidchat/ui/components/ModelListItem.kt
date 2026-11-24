/*
OpenDroidChat Model List Item Component
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.LlmModel

/**
 * 模型列表项：使用 M3E 描边卡片，选中时描边高亮。
 */
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

    M3ECard(
        onClick = { onSelect(model.id) },
        selected = isSelected,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        M3ECardRow {
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
                    .padding(start = 8.dp, end = 4.dp)
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.modelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    M3EPillLabel(
                        text = model.provider.displayName,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
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
                            Text(stringResource(R.string.model_list_switch_first))
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
}
