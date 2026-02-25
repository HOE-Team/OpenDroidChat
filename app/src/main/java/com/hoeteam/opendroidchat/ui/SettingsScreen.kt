/*
OpenDroidChat Settings Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onNavigateToAbout: () -> Unit, // 导航到 AboutScreen 的回调
    onNavigateToChat: () -> Unit,
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val currentVersionType by remember { mutableStateOf(updateManager.getCurrentVersionType()) }

    // 根据版本类型获取警告文本
    fun getWarningText(): String? {
        return when (currentVersionType) {
            VersionType.NIGHTLY -> "技术预览版本(Nightly)，极不稳定，可能导致数据丢失或发生意料之外的崩溃。"
            VersionType.BETA -> "发布前测试版本(Beta)，不稳定，可能导致数据丢失。"
            VersionType.STABLE -> null
        }
    }

    // 顶级 Column 作为容器
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. TopAppBar (固定在顶部)
        TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Normal) },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        // 2. 核心内容区域 (负责滚动)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            // 根据版本类型决定是否显示警告卡片
            val warningText = getWarningText()
            if (warningText != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 第一排：图标
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "警告",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // 间隔
                        // 第二排：显示警告文本
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // --- Padded ListItems Group (用于对齐 ListItem) ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // 2. 主题切换项 (ListItem)
                ListItem(
                    headlineContent = { Text("深色模式") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.DarkMode,
                            contentDescription = "深色模式",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = currentDarkTheme,
                            onCheckedChange = onThemeToggle,
                            enabled = true
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // 3. 关于页跳转项 (ListItem)
                ListItem(
                    headlineContent = { Text("关于 OpenDroidChat") },
                    supportingContent = { Text("版本信息和项目详情") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "关于程序",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAbout() }
                )

                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(16.dp)) // 底部留白
        }
    }
}