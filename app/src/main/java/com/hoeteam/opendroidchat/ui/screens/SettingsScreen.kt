/*
OpenDroidChat Settings Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.network.VersionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    allowOtherChannelsUpdate: Boolean,
    onAllowOtherChannelsUpdateChange: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val currentVersionType by remember { mutableStateOf(updateManager.getCurrentVersionType()) }

    fun getWarningText(): String? {
        return when (currentVersionType) {
            VersionType.NIGHTLY -> "技术预览版本(Nightly)，极不稳定，可能导致数据丢失或发生意料之外的崩溃。"
            VersionType.BETA -> "发布前测试版本(Beta)，不稳定，可能导致数据丢失。"
            VersionType.STABLE -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                //windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            val warningText = getWarningText()
            if (warningText != null) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "警告",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "版本警告",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // --- 外观分组 ---
            SettingsGroupHeader("外观")
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                ListItem(
                    headlineContent = { Text("深色模式", fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Switch(
                            checked = currentDarkTheme,
                            onCheckedChange = onThemeToggle
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            // --- 更新分组 ---
            SettingsGroupHeader("更新")
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                ListItem(
                    headlineContent = { Text("允许获取其他分发渠道的更新", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("开启后可获取 Stable/Beta 等其他渠道的更新推送") },
                    leadingContent = {
                        Icon(Icons.Filled.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Switch(
                            checked = allowOtherChannelsUpdate,
                            onCheckedChange = onAllowOtherChannelsUpdateChange
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            // --- 杂项分组 ---
            SettingsGroupHeader("杂项")
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                ListItem(
                    headlineContent = { Text("关于 OpenDroidChat", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("版本信息、开发团队及项目详情") },
                    leadingContent = {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToAbout() },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}