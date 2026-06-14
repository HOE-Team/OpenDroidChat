/*
OpenDroidChat LICENSE Screen - M3 Expressive Refactored
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoeteam.opendroidchat.ui.components.LibraryCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val backgroundColor = MaterialTheme.colorScheme.surface
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "开放源代码许可",
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "感谢为开源社区做出贡献的开发者",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    // 【核心修改】：通过赋予相同颜色，令其折叠时背景不改变，无缝融入底色
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp, // 压缩内容与打薄后 TopBar 的间距梯度
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            itemsIndexed(libraries) { index, library ->
                val animatedScale = remember { Animatable(0.9f) }
                val animatedAlpha = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    launch {
                        animatedScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        animatedAlpha.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessVeryLow))
                    }
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = animatedScale.value
                            scaleY = animatedScale.value
                            alpha = animatedAlpha.value
                        }
                ) {
                    LibraryCard(library = library)
                }
            }
        }
    }
}