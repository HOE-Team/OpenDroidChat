/*
OpenDroidChat Model Settings Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.ui.components.ModelListItem
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    onBack: () -> Unit,
    onNavigateToEditModel: (String?) -> Unit
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
                        onSelect = { viewModel.setCurrentModel(it) },
                        onEdit = { onNavigateToEditModel(model.id) },
                        onDelete = { viewModel.deleteModel(it) },
                        allModelsCount = allModels.size
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}