/*
OpenDroidChat Chat Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.createFilePickerIntent
import com.hoeteam.opendroidchat.data.selectFileFromUri
import com.hoeteam.opendroidchat.ui.components.*
import com.hoeteam.opendroidchat.viewmodel.ApiErrorState
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onOpenHistoryDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModelSettings: () -> Unit
) {
    val context = LocalContext.current
    val currentModel by viewModel.currentModel.collectAsState()
    val conversationTitle by viewModel.conversationTitle.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val editingMessageId by viewModel.editingMessageId.collectAsState()
    val editingText by viewModel.editingText.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()

    // 收集错误状态并显示 Snackbar 带重试按钮
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { error ->
            val result = snackbarHostState.showSnackbar(
                message = error.message,
                actionLabel = context.getString(R.string.chat_retry),
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLastRequest()
            }
        }
    }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val uri: Uri? = data.data
            if (uri != null) {
                val file = selectFileFromUri(context, uri)
                if (file != null) {
                    viewModel.setSelectedFile(file)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.chat_file_read_error))
                    }
                }
            }
        }
    }

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

    // 打开/切换历史对话后直接定位到最后一条消息
    LaunchedEffect(activeConversationId) {
        if (activeConversationId != null && messages.isNotEmpty()) {
            lazyListState.scrollToItem(messages.lastIndex, scrollOffset = 100000)
        }
    }

    if (currentModel == null) {
        EmptyConfigScreen(onNavigateToModelSettings = onNavigateToModelSettings)
        return
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val snackbarMessage: String = data.visuals.message
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = snackbarMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        data.visuals.actionLabel?.let { action ->
                            TextButton(onClick = { data.performAction() }) {
                                Text(
                                    text = action,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            currentModel?.name ?: "OpenDroidChat",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 当前会话标题，方便确认正在延续哪一段历史
                        conversationTitle?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // 展开历史对话抽屉
                    IconButton(onClick = onOpenHistoryDrawer) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = stringResource(R.string.chat_history)
                        )
                    }
                    // 新建对话：保存当前会话后清空并从新对话开始
                    IconButton(onClick = { viewModel.clearChat() }, enabled = !isLoading) {
                        Icon(
                            Icons.Filled.AddComment,
                            contentDescription = stringResource(R.string.chat_new_conversation)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            // 空状态时输入框在内容区居中显示，因此这里隐藏底部输入框
            if (messages.isNotEmpty()) {
                ChatInput(
                    text = inputText,
                    onTextChange = viewModel::updateInputText,
                    onSend = viewModel::sendMessage,
                    isLoading = isLoading,
                    selectedFile = selectedFile,
                    onAddFile = {
                        val intent = createFilePickerIntent()
                        filePickerLauncher.launch(intent)
                    },
                    onRemoveFile = viewModel::clearSelectedFile
                )
            }
        },
        floatingActionButton = {
            // derivedStateOf 的 lambda 只在首次组合时捕获 messages，
            // 这里用 rememberUpdatedState 保证切换/新建对话后读到的是最新列表，
            // 否则「到底部」FAB 会被上一段对话的状态错误地继承下来
            val latestMessages by rememberUpdatedState(messages)
            val showFab by remember {
                derivedStateOf {
                    val layoutInfo = lazyListState.layoutInfo
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    latestMessages.isNotEmpty() && lastVisibleItem != null && (
                        lastVisibleItem.index < layoutInfo.totalItemsCount - 1 ||
                            lastVisibleItem.offset + lastVisibleItem.size > layoutInfo.viewportEndOffset + 150
                    )
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
                    Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.chat_scroll_to_bottom))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (messages.isEmpty() && !isLoading) {
                // 主页空状态：居中显示问候标题与输入框
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.chat_empty_greeting),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    ChatInput(
                        text = inputText,
                        onTextChange = viewModel::updateInputText,
                        onSend = viewModel::sendMessage,
                        isLoading = isLoading,
                        selectedFile = selectedFile,
                        onAddFile = {
                            val intent = createFilePickerIntent()
                            filePickerLauncher.launch(intent)
                        },
                        onRemoveFile = viewModel::clearSelectedFile
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), // 加大间距梯度
                    state = lazyListState
                ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isEditing = editingMessageId == message.id,
                        editingText = editingText,
                        onEditTextChange = viewModel::updateEditingText,
                        onSaveEdit = { viewModel.saveEditedMessage(message.id) },
                        onCancelEdit = viewModel::cancelEditing,
                        onEdit = { viewModel.startEditing(message.id) },
                        onRegenerate = { viewModel.regenerateMessage(message.id) },
                        isLoading = isLoading,
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
}