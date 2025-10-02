package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.data.Message
import com.hoeteam.opendroidchat.data.Sender
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import dev.jeziellago.compose.markdowntext.MarkdownText

// --- 主聊天屏幕 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onNavigateToSettings: () -> Unit
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 监听错误
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg, "好的")
            }
        }
    }

    if (currentModel == null) {
        EmptyConfigScreen(onNavigateToSettings)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(currentModel?.name ?: "LLM Chat") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                text = inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                isLoading = isLoading
            )
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()

        // 自动滚动到最新消息
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = lazyListState
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
            if (isLoading) {
                item {
                    LoadingIndicator()
                }
            }
        }
    }
}

// ------------------- 子组件 -------------------

@Composable
fun EmptyConfigScreen(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "您尚未配置任何 LLM 模型",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNavigateToSettings,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("立即添加模型")
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.sender == Sender.USER
    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        message.text.contains("LLM 响应出错") -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cornerSize = 16.dp

    val bubbleShape = RoundedCornerShape(cornerSize).copy(
        bottomEnd = if (isUser) CornerSize(4.dp) else CornerSize(cornerSize),
        bottomStart = if (!isUser) CornerSize(4.dp) else CornerSize(cornerSize)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 60.dp else 8.dp,
                end = if (isUser) 8.dp else 60.dp
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            tonalElevation = 1.dp
        ) {
            if (isUser) {
                // 用户消息：普通 Text
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // LLM 消息：MarkdownText
                MarkdownText(
                    markdown = message.text,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor
                    )
                )
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("输入消息...") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = false,
                maxLines = 5,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            val isFabEnabled = text.isNotBlank() && !isLoading

            FloatingActionButton(
                onClick = if (isFabEnabled) onSend else ({ }),
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(if (isFabEnabled) 1f else 0.5f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text("LLM 正在输入...", style = MaterialTheme.typography.bodySmall)
    }
}
