/*
OpenDroidChat Model Settings Screen
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.ui.components.EmptyState
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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.model_settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {},
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.model_settings_add))
            }
        }
    ) { paddingValues ->
        if (allModels.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.model_settings_empty_title),
                description = stringResource(R.string.model_settings_empty),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
