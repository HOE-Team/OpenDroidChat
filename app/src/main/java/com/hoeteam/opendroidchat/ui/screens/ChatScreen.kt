/*
OpenDroidChat Chat Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.ui.components.ChatInput
import com.hoeteam.opendroidchat.ui.components.EmptyConfigScreen
import com.hoeteam.opendroidchat.ui.components.HybridMarkdown
import com.hoeteam.opendroidchat.ui.components.LoadingIndicator
import com.hoeteam.opendroidchat.ui.components.MessageBubble
import com.hoeteam.opendroidchat.ui.components.ThemedMarkdownText
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
    
    // 监听用户是否正在手动拖拽列表
    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()

    // 优化的置底判断逻辑
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                if (lastVisibleItem == null) return@derivedStateOf false
                
                val isLastItem = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                val itemBottom = lastVisibleItem.offset + lastVisibleItem.size
                val viewportEnd = layoutInfo.viewportEndOffset
                
                // 将缓冲区扩大到 200dp，防止流式输出内容剧增导致判定“脱离”
                // 只有当最后一条消息的底部明显离开视口（超过200dp）时，才认为用户已经向上翻阅了
                isLastItem && itemBottom <= viewportEnd + 200
            }
        }
    }

    // 自动滚动置底逻辑
    LaunchedEffect(messages, isLoading) {
        // 只有在用户没有手动拖拽列表时，才进行自动滚动
        // 使用 isDragged 代替 isScrollInProgress 避免了由于自动滚动动画导致的自身拦截
        if (isDragged) return@LaunchedEffect

        if (messages.isNotEmpty() && isAtBottom) {
            val lastIndex = if (isLoading) messages.size else messages.size - 1
            if (lastIndex >= 0) {
                val lastMessage = messages.lastOrNull()
                // 流式传输中，使用无动画的 scrollToItem 配合巨大偏移实现强制置底
                if (lastMessage?.isStreaming == true) {
                    lazyListState.scrollToItem(lastIndex, scrollOffset = 100000)
                } else {
                    lazyListState.animateScrollToItem(lastIndex, scrollOffset = 100000)
                }
            }
        }
    }

    // 错误处理
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg, "好的")
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
                title = { Text(currentModel?.name ?: "LLM Chat") },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = {
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
                    val layoutInfo = lazyListState.layoutInfo
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    if (lastVisibleItem == null) {
                        messages.isNotEmpty()
                    } else {
                        // 当最后一条消息的底部距离视口底部超过 150dp 时显示 FAB
                        lastVisibleItem.index < layoutInfo.totalItemsCount - 1 ||
                        lastVisibleItem.offset + lastVisibleItem.size > layoutInfo.viewportEndOffset + 150
                    }
                }
            }

            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                // 点击 FAB 强行滚回底部，由于 scrollOffset 很大，它会确保完全置底
                                lazyListState.animateScrollToItem(
                                    if (isLoading) messages.size else messages.size - 1,
                                    scrollOffset = 100000
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "滚动到底部")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = lazyListState
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        hybridMarkdown = { text: String, modifier: Modifier, textColor: Color ->
                            HybridMarkdown(
                                text = text,
                                modifier = modifier,
                                normalTextColor = textColor,
                                themedMarkdownText = { md: String, mod: Modifier, txtColor: Color, codeBg: Color, codeTxtColor: Color ->
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
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}
