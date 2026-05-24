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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("LLM API 实例管理", fontWeight = FontWeight.SemiBold) },
                //windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { onNavigateToEditModel(null) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加新实例", modifier = Modifier.size(30.dp))
            }
        }
    ) { paddingValues ->
        if (allModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                OutlinedCard(
                    modifier = Modifier.padding(24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "暂无配置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击右下角的 '+' 添加您的第一个 LLM API 实例。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allModels, key = { it.id }) { model ->
                    ModelListItem(
                        model = model,
                        isSelected = model.id == currentModel?.id,
                        onSelect = { viewModel.setCurrentModel(it) },
                        onEdit = { onNavigateToEditModel(model.id) },
                        onDelete = { viewModel.deleteModel(it) },
                        allModelsCount = allModels.size
                    )
                }
            }
        }
    }
}