/*
OpenDroidChat About Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.ui.components.M3ECard
import com.hoeteam.opendroidchat.ui.components.M3ECardRow
import com.hoeteam.opendroidchat.ui.components.M3EPillButton
import com.hoeteam.opendroidchat.ui.components.M3EPillLabel
import com.hoeteam.opendroidchat.ui.components.M3EPillOutlinedButton
import com.hoeteam.opendroidchat.ui.components.M3ESection
import com.hoeteam.opendroidchat.network.UpdateCheckResult
import com.hoeteam.opendroidchat.network.VersionType
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToLegal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val currentVersion by remember { mutableStateOf(updateManager.getCurrentVersion()) }
    val currentVersionType by remember { mutableStateOf(updateManager.getCurrentVersionType()) }

    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val checkJob = remember { mutableStateOf<Job?>(null) }

    fun checkForUpdates() {
        checkJob.value?.cancel()
        checkJob.value = scope.launch {
            isChecking = true
            updateResult = null
            try {
                val result = withContext(Dispatchers.IO) {
                    updateManager.checkForUpdates(currentVersionType)
                }
                if (!isActive) return@launch
                updateResult = result
            } catch (e: Exception) {
                if (!isActive) return@launch
                updateResult = UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = currentVersion,
                    latestVersion = null,
                    versionType = currentVersionType,
                    error = context.getString(R.string.about_check_failed, e.message ?: "")
                )
            } finally {
                if (isActive) isChecking = false
                checkJob.value = null
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentVersionType != VersionType.NIGHTLY && updateResult == null) {
            checkForUpdates()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val backgroundColor = MaterialTheme.colorScheme.surface

            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about_title),
                        letterSpacing = (-0.2).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                    }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 项目介绍（M3E 描边卡片）
            M3ESection(title = stringResource(R.string.about_app_name)) {
                Text(
                    text = stringResource(R.string.about_app_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }

            // 版本信息卡片
            M3ESection(title = stringResource(R.string.about_title)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.about_version, currentVersion),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            M3EPillLabel(
                                text = when (currentVersionType) {
                                    VersionType.NIGHTLY -> stringResource(R.string.about_version_nightly)
                                    VersionType.BETA -> stringResource(R.string.about_version_beta)
                                    VersionType.STABLE -> stringResource(R.string.about_version_stable)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 更新控制区
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (currentVersionType == VersionType.NIGHTLY) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                M3EPillButton(
                                    text = stringResource(R.string.about_build_history),
                                    onClick = { updateManager.openActionsPage() },
                                    icon = Icons.Default.Build,
                                    modifier = Modifier.weight(1f)
                                )
                                M3EPillOutlinedButton(
                                    text = stringResource(R.string.about_check_update),
                                    onClick = { checkForUpdates() },
                                    icon = Icons.Default.Refresh,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // 检查时显示进度条
                            if (isChecking) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { clip = true; shape = RoundedCornerShape(2.dp) }
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // 始终显示"立即检查新版本"按钮（药丸形）
                                    M3EPillButton(
                                        text = stringResource(R.string.about_check_now),
                                        onClick = { checkForUpdates() },
                                        icon = Icons.Default.Refresh,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // 当检查结果有新版本时，显示"获取更新"按钮
                                    if (updateResult?.hasUpdate == true) {
                                        val versionTag = updateResult?.latestVersion ?: ""
                                        M3EPillButton(
                                            text = if (versionTag.isNotEmpty()) {
                                                stringResource(R.string.about_get_update_with_version, versionTag)
                                            } else {
                                                stringResource(R.string.about_get_update)
                                            },
                                            onClick = {
                                                updateManager.openDownloadPage(
                                                    updateResult?.latestVersion
                                                )
                                            },
                                            icon = Icons.Default.Download,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 只有存在错误消息时，才在下面渲染显示红色文本
                    if (!isChecking && updateResult?.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = updateResult?.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 许可协议入口卡片
            M3ECard(onClick = onNavigateToLicense) {
                M3ECardRow {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.about_license_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.about_license_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // 法律性页面入口卡片
            M3ECard(onClick = onNavigateToLegal) {
                M3ECardRow {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.about_legal_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.about_legal_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            // 版权信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.about_license_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
