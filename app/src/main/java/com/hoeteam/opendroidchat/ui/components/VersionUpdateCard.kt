/*
OpenDroidChat Version Update Card Component
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.network.UpdateCheckResult
import com.hoeteam.opendroidchat.network.VersionType

@Composable
fun VersionUpdateCard(
    title: String,
    description: String,
    currentVersionType: VersionType,
    targetType: VersionType,
    result: UpdateCheckResult?,
    isChecking: Boolean,
    updateManager: com.hoeteam.opendroidchat.data.UpdateManager,
    onCheckUpdate: () -> Unit
) {
    val isCurrent = currentVersionType == targetType
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // M3E 物理动效：容器按下反馈
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "CardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        // M3E 高圆角
        shape = RoundedCornerShape(24.dp),
        color = if (isCurrent) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
        else 
            MaterialTheme.colorScheme.surfaceContainer,
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null,
        tonalElevation = if (isCurrent) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, // M3E 视觉强化
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isCurrent) {
                    // M3E 药丸形标签，强化“当前使用”
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(100),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "正在使用",
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            // 状态展示区
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)) {
                when {
                    isChecking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 3.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("同步中...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    result == null -> {
                        Button(
                            onClick = onCheckUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("获取最新动态")
                        }
                    }

                    else -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusIcon = when {
                                    result.hasUpdate -> Icons.Default.Update
                                    result.error != null -> Icons.Default.Error
                                    else -> Icons.Default.Check
                                }
                                val statusColor = when {
                                    result.hasUpdate -> MaterialTheme.colorScheme.primary
                                    result.error != null -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        result.hasUpdate -> "发现新版本: ${result.latestVersion}"
                                        result.error != null -> "网络连接异常"
                                        else -> "已是最新版本"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // M3E 标准的紧凑拼接按钮组 (Segmented-like style)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCheckUpdate,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isChecking,
                                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text("重新检查", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (result.hasUpdate && result.latestRelease != null) {
                                            updateManager.openDownloadPage(result.latestRelease.tag_name, currentVersionType)
                                        } else {
                                            updateManager.openDownloadPage(versionType = currentVersionType)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isChecking,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (result.hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("立即获取", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
