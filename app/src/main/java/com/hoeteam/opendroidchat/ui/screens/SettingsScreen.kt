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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.SettingsRepository
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.network.VersionType
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.platform.LocalConfiguration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

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

    // 启动时是否恢复上次对话（默认关闭，即每次启动都是新对话）
    val resumeLastConversation by settingsRepository.resumeLastConversationFlow.collectAsState(initial = false)

    // 应用语言
    val appLanguage by settingsRepository.appLanguageFlow.collectAsState(initial = "")
    var showLanguageDialog by remember { mutableStateOf(false) }

    // 语言选项
    val languageOptions = listOf(
        "" to stringResource(R.string.settings_language_follow_system),
        "zh-CN" to stringResource(R.string.settings_language_zh_cn),
        "zh-TW" to stringResource(R.string.settings_language_zh_tw),
        "en" to stringResource(R.string.settings_language_en)
    )

    // 获取当前语言显示名称
    val currentLanguageDisplay = remember(appLanguage) {
        languageOptions.find { it.first == appLanguage }?.second
            ?: languageOptions[0].second
    }

    fun getWarningText(): String? {
        return when (currentVersionType) {
            VersionType.NIGHTLY -> context.getString(R.string.settings_warning_nightly)
            VersionType.BETA -> context.getString(R.string.settings_warning_beta)
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
                        stringResource(R.string.settings_title),
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
                            contentDescription = null,
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
                title = stringResource(R.string.settings_section_general),
                dividerVisible = true
            ) {
                SettingsListItem(
                    headline = stringResource(R.string.settings_dark_mode),
                    subhead = stringResource(R.string.settings_dark_mode_desc),
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

                SettingsListItem(
                    headline = stringResource(R.string.settings_resume_last_conversation),
                    subhead = stringResource(R.string.settings_resume_last_conversation_desc),
                    icon = Icons.Filled.History,
                    trailing = {
                        Switch(
                            checked = resumeLastConversation,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsRepository.setResumeLastConversation(enabled)
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

            // ===== 语言设置 =====
            SettingsGroup(
                title = stringResource(R.string.settings_section_language),
                dividerVisible = true
            ) {
                SettingsListItem(
                    headline = stringResource(R.string.settings_app_language),
                    subhead = currentLanguageDisplay,
                    icon = Icons.Filled.Language,
                    onClick = { showLanguageDialog = true },
                    trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp)) }
                )
            }

            // 语言选择对话框
            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text(
                            text = stringResource(R.string.settings_app_language),
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    text = {
                        Column {
                            languageOptions.forEach { (code, displayName) ->
                                val onLanguageSelected: () -> Unit = {
                                    scope.launch {
                                        settingsRepository.setAppLanguage(code)
                                        showLanguageDialog = false
                                        // 立即应用语言设置
                                        val localeList = when (code) {
                                            "zh-CN" -> LocaleListCompat.forLanguageTags("zh-CN")
                                            "zh-TW" -> LocaleListCompat.forLanguageTags("zh-TW")
                                            "en" -> LocaleListCompat.forLanguageTags("en")
                                            else -> LocaleListCompat.getEmptyLocaleList()
                                        }
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onLanguageSelected)
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appLanguage == code,
                                        onClick = onLanguageSelected
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.update_dialog_cancel))
                        }
                    }
                )
            }



            // ===== 更新设置 =====
            SettingsGroup(
                title = stringResource(R.string.settings_section_update),
                dividerVisible = true
            ) {
                SettingsListItem(
                    headline = stringResource(R.string.settings_auto_update),
                    subhead = stringResource(R.string.settings_auto_update_desc),
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
                title = stringResource(R.string.settings_section_about),
                dividerVisible = false
            ) {
                SettingsListItem(
                    headline = stringResource(R.string.settings_about_app),
                    subhead = stringResource(R.string.settings_about_app_desc),
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
            .padding(horizontal = 16.dp, vertical = 2.dp)
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