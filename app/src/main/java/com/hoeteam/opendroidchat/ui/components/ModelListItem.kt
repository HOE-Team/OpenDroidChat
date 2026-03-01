/*
OpenDroidChat Model List Item Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(model.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(model.id) },
            modifier = Modifier.size(48.dp).wrapContentSize(Alignment.Center)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(model.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "API: ${model.provider.displayName} | 模型: ${model.modelName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "编辑实例")
        }

        if (canBeDeleted) {
            IconButton(onClick = { onDelete(model) }) {
                Icon(Icons.Filled.Delete, contentDescription = "删除实例", tint = MaterialTheme.colorScheme.error)
            }
        } else if (isDeletable && isSelected) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip { Text("请先切换到其他实例再删除") }
                },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = { /* Do nothing */ }, enabled = false) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除实例", tint = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}