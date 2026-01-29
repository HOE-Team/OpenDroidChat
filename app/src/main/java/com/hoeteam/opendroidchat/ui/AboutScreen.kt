// AboutScreen.kt
package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("关于" , fontWeight = FontWeight.Normal) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )
        // ----------------------------------------------------
        // 1. 应用简介卡片 (来自原 SettingsScreen.kt)
        // ----------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            // 使用 DayCounterScreen.kt 中的 Card 默认样式 (Material Design 3)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "关于 OpenDroidChat",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "OpenDroidChat 是一款适用于 Android 6(API23)+ 的 LLM API 聊天客户端，UI界面使用了Google的Material Design 3设计框架。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ----------------------------------------------------
        // 2. 版本信息卡片 (来自原 SettingsScreen.kt)
        // ----------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "版本信息",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text="Version STABLE-0.8Fix-UIComp.Removed(8)" ,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "你正在使用Stable版本！\n请定期访问 https://github.com/HOE-Team/OpenDroidChat/releases 以获取最新版本",
                    // text="* 测试版本，极不稳定，可能导致数据丢失或安全问题。",
                    style = MaterialTheme.typography.bodyMedium,
                    // color = Color.Red,
                )
            }
        }

        // ----------------------------------------------------
        // 3. 版权信息卡片 (来自原 SettingsScreen.kt)
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "版权信息",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "版权所有 ©2025-2026 HOE Team ，保留所有权利。\n源码使用MIT协议开源\n\n" +
                            "Copyright ©2025-2026 HOE Team. All rights reserved.\nLicense: MIT",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}