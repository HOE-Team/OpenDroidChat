// LicenseScreen.kt
package com.hoeteam.opendroidchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 开源库数据类
 */
data class OpenSourceLibrary(
    val name: String,           // 库名称
    val type: String,           // 库类型/描述
    val license: String,        // 许可证类型
    val licenseUrl: String      // 许可证URL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 开源库列表
    val libraries = listOf(
        OpenSourceLibrary(
            name = "AndroidX",
            type = "Android 扩展库",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt"
        ),
        OpenSourceLibrary(
            name = "Markwon",
            type = "Markdown 渲染库",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/noties/Markwon/blob/master/LICENSE"
        ),
        OpenSourceLibrary(
            name = "OkHttp",
            type = "HTTP 客户端",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/square/okhttp/blob/master/LICENSE.txt"
        ),
        // 可以继续添加更多库...
        OpenSourceLibrary(
            name = "Ktor",
            type = "异步网络框架",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/ktorio/ktor/blob/main/LICENSE"
        ),
        OpenSourceLibrary(
            name = "Kotlin Coroutines",
            type = "协程支持库",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"
        ),
        OpenSourceLibrary(
            name = "Jetpack Compose",
            type = "UI 工具包",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt"
        ),
        OpenSourceLibrary(
            name = "Material Icons",
            type = "Material 图标库",
            license = "Apache-2.0",
            licenseUrl = "https://github.com/google/material-design-icons/blob/master/LICENSE"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("开放源代码许可", fontWeight = FontWeight.Normal)
                    Text(
                        text = "OpenDroidChat使用了以下开源库",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 使用 LazyColumn 显示库列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(libraries) { library ->
                LibraryCard(library = library)
            }

            // 添加底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LibraryCard(
    library: OpenSourceLibrary
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题：开源库名称
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 正文：开源库类型
            Text(
                text = library.type,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 许可证信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧显示许可证类型
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = library.license,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // 右侧按钮：查看许可证
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(library.licenseUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "查看许可证",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}