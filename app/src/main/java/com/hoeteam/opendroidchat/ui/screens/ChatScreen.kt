/*
OpenDroidChat Chat Screen - M3 Expressive Refactored
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.ui.components.*
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onNavigateToSettings: () -> Unit,
    onNavigateToModelSettings: () -> Unit
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) true
            else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
                val isLastItem = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                val itemBottom = lastVisibleItem.offset + lastVisibleItem.size
                itemBottom <= layoutInfo.viewportEndOffset + 200
            }
        }
    }

    LaunchedEffect(messages, isLoading) {
        if (isDragged) return@LaunchedEffect
        if (messages.isNotEmpty() && isAtBottom) {
            val lastIndex = if (isLoading) messages.size else messages.size - 1
            if (lastIndex >= 0) {
                if (messages.lastOrNull()?.isStreaming == true) {
                    lazyListState.scrollToItem(lastIndex, scrollOffset = 100000)
                } else {
                    lazyListState.animateScrollToItem(lastIndex, scrollOffset = 100000)
                }
            }
        }
    }

    if (currentModel == null) {
        EmptyConfigScreen(onNavigateToModelSettings = onNavigateToModelSettings)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        currentModel?.name ?: "OpenDroidChat",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            // 底部输入框在 M3E 中被赋予最高的视觉权重（已在组件中重构）
            ChatInput(
                text = inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                isLoading = isLoading
            )
        },
        floatingActionButton = {
            val showFab by remember {
                derivedStateOf {
                    val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
                    if (lastVisibleItem == null) messages.isNotEmpty()
                    else lastVisibleItem.index < lazyListState.layoutInfo.totalItemsCount - 1 ||
                            lastVisibleItem.offset + lastVisibleItem.size > lazyListState.layoutInfo.viewportEndOffset + 150
                }
            }

            // M3E 强力弹性物理缩放进入/退出
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                lazyListState.animateScrollToItem(if (isLoading) messages.size else messages.size - 1, 100000)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp), // M3E 大圆角 FAB
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "到底部")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), // 加大间距梯度
                state = lazyListState
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        hybridMarkdown = { text, modifier, textColor ->
                            HybridMarkdown(
                                text = text,
                                modifier = modifier,
                                normalTextColor = textColor,
                                themedMarkdownText = { md, mod, txtColor, codeBg, codeTxtColor ->
                                    // 修正：显式使用命名参数以避开中间的可选参数 style
                                    ThemedMarkdownText(
                                        markdown = md,
                                        modifier = mod,
                                        textColor = txtColor,
                                        codeBackgroundColor = codeBg,
                                        codeTextColor = codeTxtColor
                                    )
                                }
                            )
                        }
                    )
                }
                if (isLoading) {
                    item(key = "loading_indicator") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}
