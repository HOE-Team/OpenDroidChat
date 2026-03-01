/*
OpenDroidChat LICENSE Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.ui.components.LibraryCard

/**
 * 开源库数据类
 */
data class OpenSourceLibrary(
    val name: String,
    val type: String,
    val license: String,
    val licenseUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBack: () -> Unit
) {
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(libraries) { library ->
                LibraryCard(library = library)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}