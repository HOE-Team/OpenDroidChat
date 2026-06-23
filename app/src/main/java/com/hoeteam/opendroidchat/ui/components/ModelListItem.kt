/*
OpenDroidChat Model List Item Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // M3E 弹性微动：按下缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ItemScale"
    )

    // 选中状态的色调高度与背景色动态对比
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ContainerColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(model.id) }
            ),
        shape = RoundedCornerShape(20.dp), // M3E 高圆角
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelect(model.id) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    // M3E: 选中时通过加粗强化权重
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
                Text(
                    "API: ${model.provider.displayName} | 模型: ${model.modelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit, 
                        contentDescription = "编辑实例",
                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
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
                        tooltip = { PlainTooltip { Text("请先切换到其他实例再删除") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { /* Do nothing */ }, enabled = false) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "删除实例", 
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
