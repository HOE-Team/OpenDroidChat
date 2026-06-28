/*
OpenDroidChat Settings Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoeteam.opendroidchat.data.SettingsRepository
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.network.VersionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToChat: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val currentVersionType by remember { mutableStateOf(updateManager.getCurrentVersionType()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 自动更新检查开关
    val autoUpdateCheck by settingsRepository.autoUpdateCheckFlow.collectAsState(initial = true)

    fun getWarningText(): String? {
        return when (currentVersionType) {
            VersionType.NIGHTLY -> "技术预览版本 (Nightly)，极不稳定，可能导致数据丢失或发生意料之外的崩溃。"
            VersionType.BETA -> "发布前测试版本 (Beta)，不稳定，可能导致数据丢失。"
            VersionType.STABLE -> null
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val backgroundColor = MaterialTheme.colorScheme.surface

            // 配置完全配平的 containerColor，并消除默认的不安全 windowInsets 边距
            LargeTopAppBar(
                title = {
                    Text(
                        "应用设置",
                        letterSpacing = (-0.2).sp
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            val warningText = getWarningText()
            if (warningText != null) {
                // M3E 表现力警告容器：高对比度、大圆角
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "警告",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ===== 通用设置 =====
            SettingsGroup(
                title = "通用设置",
                dividerVisible = true
            ) {
                SettingsListItem(
                    headline = "深色模式",
                    subhead = "切换应用视觉主题",
                    icon = Icons.Filled.DarkMode,
                    trailing = {
                        Switch(
                            checked = currentDarkTheme,
                            onCheckedChange = onThemeToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                )
            }

            // ===== 更新设置 =====
            SettingsGroup(
                title = "更新",
                dividerVisible = true
            ) {
                SettingsListItem(
                    headline = "自动检查更新",
                    subhead = "启动时自动检查新版本并提醒",
                    icon = Icons.Default.Info,
                    trailing = {
                        Switch(
                            checked = autoUpdateCheck,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsRepository.setAutoUpdateCheck(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                )
            }

            // ===== 支持与关于 =====
            SettingsGroup(
                title = "支持与关于",
                dividerVisible = false
            ) {
                SettingsListItem(
                    headline = "关于 OpenDroidChat",
                    subhead = "版本信息、开源许可与项目详情",
                    icon = Icons.Filled.Info,
                    onClick = onNavigateToAbout,
                    trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * M3E 风格的设置列表项，集成了弹性物理反馈
 */
/**
 * 统一间距的设置组，包含标题行、列表项和底部可选分割线
 */
@Composable
private fun SettingsGroup(
    title: String,
    dividerVisible: Boolean,
    groupContent: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        groupContent()

        if (dividerVisible) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsListItem(
    headline: String,
    subhead: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ListItemScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isPressed) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent
    ) {
        ListItem(
            headlineContent = { Text(headline, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subhead) },
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}