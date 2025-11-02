package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.data.LlmModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory

// --- 模型列表主屏幕 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onBack: () -> Unit,
    onNavigateToEditModel: (String?) -> Unit // 传递模型 ID，null 表示新增
) {
    val allModels by viewModel.allModels.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM API 实例管理") },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditModel(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "添加新实例")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (allModels.isEmpty()) {
                item {
                    Text(
                        "点击右下角的 '+' 添加您的第一个 LLM API 实例。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(allModels, key = { it.id }) { model ->
                    ModelListItem(
                        model = model,
                        isSelected = model.id == currentModel?.id,
                        onSelect = viewModel::setCurrentModel,
                        onEdit = { onNavigateToEditModel(model.id) },
                        onDelete = viewModel::deleteModel,
                        allModelsCount = allModels.size
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListItem(
    model: LlmModel,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: (LlmModel) -> Unit,
    allModelsCount: Int
) {
    val isDeletable = allModelsCount > 1
    val canBeDeleted = isDeletable && !isSelected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(model.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(model.id) },
            modifier = Modifier.size(48.dp).wrapContentSize(Alignment.Center)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(model.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "API: ${model.provider.displayName} | 模型: ${model.modelName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "编辑实例")
        }

        if (canBeDeleted) {
            IconButton(onClick = { onDelete(model) }) {
                Icon(Icons.Filled.Delete, contentDescription = "删除实例", tint = MaterialTheme.colorScheme.error)
            }
        } else if (isDeletable && isSelected) {
            // 修正后的 TooltipBox (使用 positionProvider)
            TooltipBox(
                // 必须使用 positionProvider 参数
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip { Text("请先切换到其他实例再删除") }
                },
                state = rememberTooltipState() // 必须使用 rememberTooltipState()
            ) {
                IconButton(onClick = { /* Do nothing */ }, enabled = false) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除实例", tint = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}