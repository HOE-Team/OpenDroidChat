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
import androidx.compose.ui.text.font.FontWeight
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
                isLastItem && itemBottom <= viewportEnd + 200
            }
        }
    }

    // 自动滚动置底逻辑
    LaunchedEffect(messages, isLoading) {
        if (isDragged) return@LaunchedEffect

        if (messages.isNotEmpty() && isAtBottom) {
            val lastIndex = if (isLoading) messages.size else messages.size - 1
            if (lastIndex >= 0) {
                val lastMessage = messages.lastOrNull()
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
                title = { 
                    Text(
                        currentModel?.name ?: "LLM Chat",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                //windowInsets = WindowInsets(0, 0, 0, 0),
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
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                lazyListState.animateScrollToItem(
                                    if (isLoading) messages.size else messages.size - 1,
                                    scrollOffset = 100000
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.extraLarge
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        Box(modifier = Modifier.padding(vertical = 8.dp)) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}
