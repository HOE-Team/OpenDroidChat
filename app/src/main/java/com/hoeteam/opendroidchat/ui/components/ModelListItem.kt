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

    OutlinedCard(
        onClick = { onSelect(model.id) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else 
                MaterialTheme.colorScheme.surfaceContainer
        ),
        border = CardDefaults.outlinedCardBorder(enabled = isSelected).copy(
            width = if (isSelected) 1.dp else 0.5.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            headlineContent = {
                Text(
                    model.name, 
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    "API: ${model.provider.displayName} | 模型: ${model.modelName}",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(model.id) }
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit, 
                            contentDescription = "编辑实例",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (canBeDeleted) {
                        IconButton(onClick = { onDelete(model) }) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "删除实例", 
                                tint = MaterialTheme.colorScheme.error
                            )
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
                                Icon(
                                    Icons.Filled.Delete, 
                                    contentDescription = "删除实例", 
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}