package com.hoeteam.opendroidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    modelToEdit: LlmModel?, // 为 null 表示新增
    onSave: () -> Unit
) {
    // 初始化状态，如果是编辑则使用 modelToEdit 的值
    val isNewModel = modelToEdit == null
    val originalId = modelToEdit?.id ?: ""

    var name by remember(modelToEdit) { mutableStateOf(modelToEdit?.name ?: "") }
    var apiKey by remember(modelToEdit) { mutableStateOf(modelToEdit?.apiKey ?: "") }
    var provider by remember(modelToEdit) { mutableStateOf(modelToEdit?.provider ?: LlmProvider.OpenAI) }
    var modelName by remember(modelToEdit) { mutableStateOf(modelToEdit?.modelName ?: "") }
    var systemPrompt by remember(modelToEdit) { mutableStateOf(modelToEdit?.systemPrompt ?: "") }
    var customApiUrl by remember(modelToEdit) { mutableStateOf(modelToEdit?.customApiUrl ?: "") }
    var appId by remember(modelToEdit) { mutableStateOf(modelToEdit?.appId ?: "") } // <-- 确保 App ID 状态变量已声明

    var expanded by remember { mutableStateOf(false) }

    // 检查是否所有必填字段都已填写
    val isSaveEnabled = name.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewModel) "添加新模型" else "编辑模型: ${modelToEdit?.name}") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    // 修正点 1: 在保存时包含 customApiUrl 和 appId
                    val newModel = LlmModel(
                        id = originalId.ifBlank { java.util.UUID.randomUUID().toString() },
                        name = name.trim(),
                        provider = provider,
                        apiKey = apiKey.trim(),
                        modelName = modelName.trim(),
                        systemPrompt = systemPrompt.trim(),
                        // 确保只有当 provider 为 Custom 时才保存 customApiUrl
                        customApiUrl = customApiUrl.trim().takeIf { it.isNotBlank() && provider == LlmProvider.Custom },
                        // 确保保存 appId
                        appId = appId.trim().takeIf { it.isNotBlank() }
                    )
                    viewModel.addOrUpdateModel(newModel)
                    onSave()
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(if (isNewModel) "保存" else "保存修改")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 模型用户名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("模型名称 (自定义)*") },
                placeholder = { Text("例如：我的Qwen") },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. LLM 提供商选择 (DropdownMenu)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = provider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("LLM API 提供商") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

                                // 切换提供商时清空 customApiUrl 和 appId，避免混淆
                                customApiUrl = ""
                                appId = ""
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // 3. 模型名称 (API 专用)
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("模型调用名称 (Model Name)*") },
                placeholder = { Text("例如：qwen-turbo, gpt-4o, gemini-2.5-flash") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. API Key 输入 (密码形式)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key*") },
                placeholder = { Text("输入您的 LLM API 密钥") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. 自定义 API URL (仅当选择 "自定义 API" 时显示)
            if (provider == LlmProvider.Custom) {
                OutlinedTextField(
                    value = customApiUrl,
                    onValueChange = { customApiUrl = it },
                    label = { Text("自定义 API URL") },
                    placeholder = { Text("例如：https://your-custom-api.com/v1/chat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 修正点 2: App ID 输入框显示逻辑
            // 仅当选择 "Dashscope" 或 "自定义 API" 时显示 App ID
            if (provider == LlmProvider.Dashscope || provider == LlmProvider.Custom) {
                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = {
                        Text(
                            when (provider) {
                                LlmProvider.Dashscope -> "Dashscope 应用 ID (App ID, 可选)"
                                else -> "应用 ID / 额外参数 (可选)"
                            }
                        )
                    },
                    placeholder = { Text("输入应用 ID 或留空") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 7. 系统提示词 (System Prompt)
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("系统提示词 (Prompt)") },
                placeholder = { Text("输入 LLM 的初始指令，自定义语言模型的行为。") },
                maxLines = 5,
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "* 必填字段",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}