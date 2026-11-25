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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
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
import com.hoeteam.opendroidchat.data.supportsThinking
import com.hoeteam.opendroidchat.data.selectFileFromUri
import com.hoeteam.opendroidchat.ui.components.*
import com.hoeteam.opendroidchat.viewmodel.ApiErrorState
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch

/** 自动滚动的触发信号：任一字段变化都代表"最后一条消息的可视内容变了" */
private data class AutoScrollTick(
    val messageCount: Int,
    val textLength: Int,
    val reasoningLength: Int,
    val segmentsWeight: Int
)

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
    val isResponding by viewModel.isResponding.collectAsState()
    val thinkingEnabled by viewModel.thinkingEnabled.collectAsState()
    val reasoningEffort by viewModel.reasoningEffort.collectAsState()
    val effortOptions by viewModel.reasoningEffortOptions.collectAsState()
    val mcpEnabled by viewModel.mcpEnabled.collectAsState()
    val mcpServers by viewModel.mcpServers.collectAsState()
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

    // ===== 自动跟随滚动 =====
    // autoFollow：true = 跟随新内容贴底；false = 用户已主动离开底部
    var autoFollow by remember { mutableStateOf(true) }
    // 在长时间存活的协程里读取最新值，避免把 autoFollow 当 LaunchedEffect 的 key
    val autoFollowState = rememberUpdatedState(autoFollow)

    // 1) 用户手指按下拖动 → 交出滚动控制权
    //    只用 isDragged 判定"用户接管"，绝不能用 canScrollForward 之类的几何量：
    //    我们自己的程序化滚动也会让它变成 true，从而把 autoFollow 自己关掉
    LaunchedEffect(isDragged) {
        if (isDragged) autoFollow = false
    }

    // 3) 跟随滚动。
    //
    // 触发源必须覆盖"最后一条消息的任何可视变化"，而不是只看正文：
    //   - 正文增长（text）
    //   - 思考内容增长（reasoningText）
    //   - MCP 调用块新增/更新（segments）
    // 旧实现只看 text.length，所以"只有正文会触发自动滚动"，
    // 思考与 MCP 调用阶段列表完全不动 —— 这就是本次问题的根因。
    LaunchedEffect(Unit) {
        snapshotFlow {
            val last = messages.lastOrNull()
            if (last == null) {
                AutoScrollTick(0, 0, 0, 0)
            } else {
                AutoScrollTick(
                    messages.size,
                    last.text.length,
                    last.reasoningText?.length ?: 0,
                    last.segments.sumOf { segment ->
                        when (segment) {
                            is com.hoeteam.opendroidchat.data.McpSegment.Text -> segment.text.length
                            is com.hoeteam.opendroidchat.data.McpSegment.Thinking -> segment.text.length
                            is com.hoeteam.opendroidchat.data.McpSegment.Calls -> segment.calls.size * 1000
                        }
                    }
                )
            }
        }
            .distinctUntilChanged()
            .sample(100)
            .collect { tick ->
                if (!autoFollowState.value || messages.isEmpty()) return@collect
                val target = lazyListState.layoutInfo.totalItemsCount - 1
                if (target >= 0) {
                    lazyListState.scrollToItem(target, Int.MAX_VALUE)
                }
            }
    }

    // 4) 兜底：任何一次布局把底部撑开，只要还在跟随就立刻贴回。
    //    这条不依赖具体字段，能覆盖"最后一个 chunk 落在两次采样之间"的情况。
    LaunchedEffect(lazyListState) {
        // 防抖：避免"滚动 → 布局 → 仍可前滚 → 再滚动"形成空转
        var lastScrollAt = 0L
        snapshotFlow { lazyListState.canScrollForward }
            .collect { canScrollForward ->
                val total = lazyListState.layoutInfo.totalItemsCount
                if (total <= 0) return@collect
                if (!canScrollForward) {
                    autoFollow = true
                } else if (autoFollowState.value) {
                    val now = System.currentTimeMillis()
                    if (now - lastScrollAt >= 80L) {
                        lastScrollAt = now
                        lazyListState.scrollToItem(total - 1, Int.MAX_VALUE)
                    }
                }
            }
    }

    // 5) 新消息追加时立刻贴底
    LaunchedEffect(messages.size) {
        if (!autoFollowState.value) return@LaunchedEffect
        val total = lazyListState.layoutInfo.totalItemsCount
        if (total > 0) lazyListState.scrollToItem(total - 1, Int.MAX_VALUE)
    }

    // 打开/切换历史对话后直接定位到最后一条消息
    LaunchedEffect(activeConversationId) {
        if (activeConversationId != null && messages.isNotEmpty()) {
            // 切换/恢复会话后回到「跟随」状态
            autoFollow = true
            lazyListState.scrollToItem(messages.lastIndex, scrollOffset = 100000)
        }
    }

    // 发送新消息后重新进入跟随状态
    LaunchedEffect(isLoading) {
        if (isLoading) autoFollow = true
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
                Column {
                    // 聊天页快捷开关：思考 / 思考强度 / MCP
                    ChatToolChips(
                        thinkingSupported = currentModel?.supportsThinking == true,
                        thinkingEnabled = thinkingEnabled,
                        onThinkingChange = viewModel::setThinkingEnabled,
                        effort = reasoningEffort,
                        effortOptions = effortOptions,
                        onEffortChange = viewModel::setReasoningEffort,
                        mcpAvailable = currentModel?.enableMcp == true &&
                            mcpServers.any { it.enabled },
                        mcpEnabled = mcpEnabled,
                        onMcpChange = viewModel::setMcpEnabled
                    )
                    ChatInput(
                        text = inputText,
                        onTextChange = viewModel::updateInputText,
                        onSend = viewModel::sendMessage,
                        // 思考阶段输入框保持可用，只有正文开始输出后才锁定
                        isLoading = isResponding,
                        selectedFile = selectedFile,
                        onAddFile = {
                            val intent = createFilePickerIntent()
                            filePickerLauncher.launch(intent)
                        },
                        onRemoveFile = viewModel::clearSelectedFile
                    )
                }
            }
        },
        floatingActionButton = {
            // 离开底部（autoFollow = false）且已有消息时显示「到底部」FAB
            val showFab = messages.isNotEmpty() && !autoFollow

            // M3E 强力弹性物理缩放进入/退出
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        // 先恢复跟随，再滚到底：否则滚动结束后几何判定会把它又关掉，
                        // 导致「点了到底部却不再自动跟随」
                        autoFollow = true
                        scope.launch {
                            val target = lazyListState.layoutInfo.totalItemsCount - 1
                            if (target >= 0) {
                                lazyListState.animateScrollToItem(target, Int.MAX_VALUE)
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
                        isLoading = isResponding,
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
                items(messages, key = { it.id }, contentType = { "message" }) { message ->
                    MessageBubble(
                        message = message,
                        isEditing = editingMessageId == message.id,
                        editingText = editingText,
                        onEditTextChange = viewModel::updateEditingText,
                        onSaveEdit = { viewModel.saveEditedMessage(message.id) },
                        onCancelEdit = viewModel::cancelEditing,
                        onEdit = { viewModel.startEditing(message.id) },
                        onRegenerate = { viewModel.regenerateMessage(message.id) },
                        isLoading = isLoading
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