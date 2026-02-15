package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.draw.alpha
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.data.Message
import com.hoeteam.opendroidchat.data.Sender
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import kotlinx.coroutines.launch

// --- 主聊天屏幕 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onNavigateToSettings: () -> Unit,
    // FIX 1: 新增导航到模型设置的回调
    onNavigateToModelSettings: () -> Unit
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
        // FIX 2: 切换到 ModelSettings 导航回调
        EmptyConfigScreen(onNavigateToModelSettings)
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
// FIX 3: 接收新的导航回调
fun EmptyConfigScreen(onNavigateToModelSettings: () -> Unit) {
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
            // FIX 4: 按钮点击事件更改为跳转到 ModelSettings
            onClick = onNavigateToModelSettings,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("立即添加模型")
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
// ... (MessageBubble 内容保持不变)
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
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                HybridMarkdown(
                    text = message.text,
                    modifier = Modifier.padding(10.dp),
                    normalTextColor = textColor
                )
            }
        }
    }
}

// ------------------- 混合 Markdown 渲染 -------------------
@Composable
fun HybridMarkdown(
// ... (HybridMarkdown 内容保持不变)
    text: String,
    modifier: Modifier = Modifier,
    normalTextColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val isDark = isSystemInDarkTheme()
    val codeBackground = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val codeTextColor = if (isDark) Color(0xFFCE9178) else Color(0xFFB00020)

    Column(modifier = modifier) {
        var insideCodeBlock = false
        val normalBuffer = mutableListOf<String>()
        val lines = text.lines()

        lines.forEach { line ->
            if (line.trim().startsWith("```")) {
                // 切换状态
                if (insideCodeBlock) {
                    // 结束代码块
                    // flush normalBuffer as code block
                    Surface(
                        color = codeBackground,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = normalBuffer.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            color = codeTextColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    normalBuffer.clear()
                } else {
                    // flush normalBuffer as normal markdown
                    if (normalBuffer.isNotEmpty()) {
                        val content = normalBuffer.joinToString("\n")
                        ThemedMarkdownText(
                            markdown = content,
                            modifier = Modifier.fillMaxWidth(),
                            textColor = normalTextColor,
                            codeBackgroundColor = codeBackground,
                            codeTextColor = codeTextColor
                        )
                        normalBuffer.clear()
                    }
                }
                insideCodeBlock = !insideCodeBlock
            } else {
                normalBuffer.add(line)
            }
        }

        // flush remaining buffer
        if (normalBuffer.isNotEmpty()) {
            if (insideCodeBlock) {
                Surface(
                    color = codeBackground,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = normalBuffer.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        color = codeTextColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                ThemedMarkdownText(
                    markdown = normalBuffer.joinToString("\n"),
                    modifier = Modifier.fillMaxWidth(),
                    textColor = normalTextColor,
                    codeBackgroundColor = codeBackground,
                    codeTextColor = codeTextColor
                )
            }
        }
    }
}


// ------------------- 主题感知 Markdown 渲染 -------------------
@Composable
fun ThemedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    codeBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    codeTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val textColorArgb = textColor.toArgb()
    val codeBgArgb = codeBackgroundColor.toArgb()
    val codeTextArgb = codeTextColor.toArgb()

    val markwon = remember(codeBgArgb, codeTextArgb) {
        Markwon.builder(context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(codeBgArgb)
                        .codeTextColor(codeTextArgb)
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorArgb)
                if (style.fontSize.isSp) {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
                }
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            markwon.setMarkdown(textView, markdown)
        }
    )
}

// ------------------- 输入框 -------------------
@Composable
fun ChatInput(
// ... (ChatInput 内容保持不变)
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
        Text("正在等待 LLM API 响应...", style = MaterialTheme.typography.bodySmall)
    }
}