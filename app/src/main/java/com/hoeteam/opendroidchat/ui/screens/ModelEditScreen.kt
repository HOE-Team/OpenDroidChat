/*
OpenDroidChat Model Edit Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.data.LlmModel
import com.hoeteam.opendroidchat.data.LlmProvider
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelEditScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current)),
    modelToEdit: LlmModel?,
    onSave: () -> Unit
) {
    val isNewModel = modelToEdit == null
    val originalId = modelToEdit?.id ?: ""

    var name by remember(modelToEdit) { mutableStateOf(modelToEdit?.name ?: "") }
    var apiKey by remember(modelToEdit) { mutableStateOf(modelToEdit?.apiKey ?: "") }
    var provider by remember(modelToEdit) { mutableStateOf(modelToEdit?.provider ?: LlmProvider.OpenAI) }
    var modelName by remember(modelToEdit) { mutableStateOf(modelToEdit?.modelName ?: "") }
    var systemPrompt by remember(modelToEdit) { mutableStateOf(modelToEdit?.systemPrompt ?: "") }
    var customApiUrl by remember(modelToEdit) { mutableStateOf(modelToEdit?.customApiUrl ?: "") }
    var appId by remember(modelToEdit) { mutableStateOf(modelToEdit?.appId ?: "") }
    var useStream by remember(modelToEdit) { 
        mutableStateOf(modelToEdit?.useStream ?: (provider != LlmProvider.Custom)) 
    }

    var expanded by remember { mutableStateOf(false) }

    val isSaveEnabled = name.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isNewModel) "添加新实例" else "编辑实例: ${modelToEdit?.name}",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                //windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Button(
                    onClick = {
                        val newModel = LlmModel(
                            id = originalId.ifBlank { java.util.UUID.randomUUID().toString() },
                            name = name.trim(),
                            provider = provider,
                            apiKey = apiKey.trim(),
                            modelName = modelName.trim(),
                            systemPrompt = systemPrompt.trim(),
                            customApiUrl = customApiUrl.trim().takeIf { it.isNotBlank() && provider == LlmProvider.Custom },
                            appId = appId.trim().takeIf { it.isNotBlank() },
                            useStream = if (provider == LlmProvider.Custom) useStream else true
                        )
                        viewModel.addOrUpdateModel(newModel)
                        onSave()
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (isNewModel) "创建实例" else "保存修改", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称 (例如：My Qwen)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = provider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("API 提供商") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LlmProvider.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.displayName) },
                            onClick = {
                                provider = p
                                expanded = false
                                if (p != LlmProvider.Custom) {
                                    useStream = true
                                    customApiUrl = ""
                                } else {
                                    useStream = false
                                }
                                appId = ""
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("模型标识 (Model Name)") },
                placeholder = { Text("例如：qwen-max, gpt-4o") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            if (provider == LlmProvider.Custom) {
                OutlinedTextField(
                    value = customApiUrl,
                    onValueChange = { customApiUrl = it },
                    label = { Text("API 地址 (URL)") },
                    placeholder = { Text("https://example.com/v1/chat/completions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "流式传输 (Stream)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "开启后实时显示输出",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useStream,
                            onCheckedChange = { useStream = it }
                        )
                    }
                }
            }

            if (provider == LlmProvider.Dashscope || provider == LlmProvider.Custom) {
                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = {
                        Text(
                            when (provider) {
                                LlmProvider.Dashscope -> "应用 ID (App ID, 可选)"
                                else -> "额外参数 (可选)"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
            }

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("系统提示词 (System Prompt)") },
                placeholder = { Text("设置模型的行为准则...") },
                maxLines = 8,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            if (!isSaveEnabled) {
                Text(
                    text = "请填写所有必填字段 (*) 以保存配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}