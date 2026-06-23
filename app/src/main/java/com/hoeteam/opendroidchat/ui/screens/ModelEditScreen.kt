/*
OpenDroidChat Model Edit Screen
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // 改用标准的 TopAppBar 滚动行为，或者固定不滚动
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isSaveEnabled = name.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 【修改】：不需要 LargeTopBar，改用紧凑的标准 TopAppBar
            TopAppBar(
                title = {
                    Text(
                        if (isNewModel) "添加新模型" else "配置实例",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Transparent
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
                        .height(64.dp)
                        .scale(scale),
                    shape = RoundedCornerShape(20.dp),
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        if (isNewModel) "保存并启用" else "保存修改",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            M3EInputSection(title = "基本信息") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("显示名称", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("例如：Qwen-Max-专业版") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("接口提供商", fontWeight = FontWeight.Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        LlmProvider.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName, fontWeight = FontWeight.Medium) },
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
            }

            M3EInputSection(title = "API 配置") {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型标识 (Model ID)", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("例如：gpt-4o") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API 密钥 (API Key)", fontWeight = FontWeight.Bold) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                if (provider == LlmProvider.Custom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customApiUrl,
                        onValueChange = { customApiUrl = it },
                        label = { Text("接口地址 (Base URL)", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("https://api.example.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            M3EInputSection(title = "进阶偏好") {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("系统提示词 (System Prompt)", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("定义 AI 的身份和行为...") },
                    maxLines = 5,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                if (provider == LlmProvider.Custom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("流式传输 (Streaming)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("开启后可实时查看打字机效果", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = useStream, onCheckedChange = { useStream = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun M3EInputSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}