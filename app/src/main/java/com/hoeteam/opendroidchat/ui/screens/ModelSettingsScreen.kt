/*
OpenDroidChat Model Settings Screen - M3 Expressive Refactored
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 【修改】：改用紧凑型 TopAppBar，并移除 navigationIcon (返回按钮)
            TopAppBar(
                title = {
                    Text(
                        "模型实例管理",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {}, // 【修改】：不需要返回按钮，置空
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditModel(null) },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加新实例")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (allModels.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            "您还没有配置任何模型。\n点击右下角的 '+' 按钮开始添加。",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                }
            }
        }
    }
}