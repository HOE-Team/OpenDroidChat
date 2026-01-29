// SettingsScreen.kt
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit, // 导航到 AboutScreen 的回调
    onNavigateToChat: () -> Unit,
) {
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

            // --- 2. Padded ListItems Group (用于对齐 ListItem) ---
            // ListItems 所在的 Column 仍然需要水平填充来对齐其内容
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
                    // ListItem 默认背景色与父容器颜色保持一致即可，不需要额外设置
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