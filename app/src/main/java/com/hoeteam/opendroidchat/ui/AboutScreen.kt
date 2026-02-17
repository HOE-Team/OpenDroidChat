// AboutScreen.kt
package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.network.UpdateCheckResult
import com.hoeteam.opendroidchat.network.VersionParser
import com.hoeteam.opendroidchat.network.VersionType
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    // 当前版本
    val currentVersion by remember { mutableStateOf(updateManager.getCurrentVersion()) }
    val currentVersionType by remember { mutableStateOf(updateManager.getCurrentVersionType()) }

    // 更新检查结果状态
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    // 用于取消正在进行的检查的Job
    val checkJob = remember { mutableStateOf<Job?>(null) }

    // 检查更新函数
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

            } catch (e: CancellationException) {
                println("更新检查被取消")
            } catch (e: Exception) {
                if (!isActive) return@launch
                updateResult = UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = currentVersion,
                    latestVersion = null,
                    versionType = currentVersionType,
                    error = "检查失败: ${e.message}"
                )
            } finally {
                if (isActive) {
                    isChecking = false
                }
                checkJob.value = null
            }
        }
    }

    // 自动检查更新
    LaunchedEffect(Unit) {
        if (updateResult == null) {
            checkForUpdates()
        }
    }

    // 清理协程
    DisposableEffect(Unit) {
        onDispose {
            checkJob.value?.cancel()
        }
    }

    // 获取版本类型显示文本
    fun getVersionTypeText(): String {
        return when (currentVersionType) {
            VersionType.NIGHTLY -> "Nightly 测试版"
            VersionType.BETA -> "Beta 公测版"
            VersionType.STABLE -> "稳定版"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("关于", fontWeight = FontWeight.Normal) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 1. 应用简介卡片
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "OpenDroidChat 是一款适用于 Android 6(API23)+ 的 LLM API 聊天客户端，UI界面使用了Google的Material Design 3设计框架。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 2. 版本信息卡片
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

                // 当前版本显示
                Text(
                    text = "Version $currentVersion",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 版本类型标签
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = getVersionTypeText(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 更新检查区域
                when {
                    isChecking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在检查更新...")
                        }
                    }

                    updateResult == null -> {
                        OutlinedButton(
                            onClick = { checkForUpdates() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("检查更新")
                        }
                    }

                    else -> {
                        val result = updateResult!!

                        // 检查结果显示 - 使用 Material Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            when {
                                result.hasUpdate -> {
                                    Icon(
                                        Icons.Default.Update,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "发现新版本: ${result.latestVersion}",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                result.error != null -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "检查失败",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.Check,  // 使用 Check 图标替代 ✓
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "已是最新版本",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 两个按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { checkForUpdates() },
                                modifier = Modifier.weight(1f),
                                enabled = !isChecking
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重新检查")
                            }

                            Button(
                                onClick = {
                                    result.latestRelease?.let { release ->
                                        updateManager.openDownloadPage(release.tag_name)
                                    } ?: run {
                                        updateManager.openDownloadPage()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = result.hasUpdate && result.latestRelease != null && !isChecking,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (result.hasUpdate)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (result.hasUpdate)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("下载新版本")
                            }
                        }

                        // 错误详情
                        if (result.error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // 3. 版权信息卡片
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