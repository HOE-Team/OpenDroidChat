package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var darkThemeEnabled by remember { mutableStateOf(false) } // 示例状态

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 使用与主页相同的 TopAppBar 样式
        TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Normal) }, // 使用粗体
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        Column(
            modifier = Modifier
                .weight(1f) // 占据剩余空间
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 主题切换开关
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "深色主题(暂不可用)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) // 标题使用粗体
                    )
                    Switch(
                        checked = darkThemeEnabled,
                        onCheckedChange = { darkThemeEnabled = it }
                    )
                }
            }

            // 版本说明
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
                        text = "关于 OpenDroidChat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // 标题使用粗体
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "OpenDroidChat 是一款适用于 Android 5(API24)+ 的 LLM API 聊天客户端，UI界面使用了Google的Material Design 3设计框架。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            // 应用简介卡片
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // 标题使用粗体
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text="Version Alpha-0.5-UIUpdate" ,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text="* 测试版本，极不稳定，可能导致数据丢失或安全问题。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                    )
                }
            }
            // 版权信息
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // 标题使用粗体
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "版权所有 ©2025 HOE Team ，保留所有权利。\n源码使用MIT协议开源",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}