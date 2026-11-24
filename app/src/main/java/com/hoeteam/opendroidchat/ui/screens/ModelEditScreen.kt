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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.LlmModel
import com.hoeteam.opendroidchat.data.LlmProvider
import com.hoeteam.opendroidchat.ui.components.AppTextField
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
    var enableThinking by remember(modelToEdit) { mutableStateOf(modelToEdit?.enableThinking ?: false) }
    var enableMcp by remember(modelToEdit) { mutableStateOf(modelToEdit?.enableMcp ?: false) }
    var reasoningEffort by remember(modelToEdit) {
        mutableStateOf(modelToEdit?.reasoningEffort ?: "high")
    }

    var expanded by remember { mutableStateOf(false) }
    var effortExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isProviderSupported = provider.selectable
    val isSaveEnabled = name.isNotBlank() && apiKey.isNotBlank() &&
            modelName.isNotBlank() && isProviderSupported

    val supportsThinking = provider == LlmProvider.DeepSeek ||
            provider == LlmProvider.OpenAI

    val supportsReasoningEffort = enableThinking && (
            provider == LlmProvider.DeepSeek ||
            provider == LlmProvider.OpenAI
            )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNewModel) stringResource(R.string.model_edit_title_add) else stringResource(R.string.model_edit_title_edit),
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.model_edit_back))
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
                            useStream = if (provider == LlmProvider.Custom) useStream else true,
                            enableThinking = enableThinking,
                            reasoningEffort = if (enableThinking && supportsReasoningEffort) reasoningEffort else null,
                            enableMcp = enableMcp
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
                        if (isNewModel) stringResource(R.string.model_edit_save_add) else stringResource(R.string.model_edit_save_edit),
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
            M3EInputSection(title = stringResource(R.string.model_edit_section_basic)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.model_edit_display_name),
                    placeholder = stringResource(R.string.model_edit_display_name_hint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    AppTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        enabled = false,
                        label = stringResource(R.string.model_edit_provider),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        singleLine = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        LlmProvider.entries.filter { it.selectable }.forEach { p ->
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

                if (!provider.selectable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.model_edit_claude_removed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            M3EInputSection(title = stringResource(R.string.model_edit_section_api)) {
                AppTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = stringResource(R.string.model_edit_model_id),
                    placeholder = stringResource(R.string.model_edit_model_id_hint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = stringResource(R.string.model_edit_api_key),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (provider == LlmProvider.Custom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppTextField(
                        value = customApiUrl,
                        onValueChange = { customApiUrl = it },
                        label = stringResource(R.string.model_edit_custom_url),
                        placeholder = stringResource(R.string.model_edit_custom_url_hint),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            M3EInputSection(title = stringResource(R.string.model_edit_section_advanced)) {
                AppTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = stringResource(R.string.model_edit_system_prompt),
                    placeholder = stringResource(R.string.model_edit_system_prompt_hint),
                    maxLines = 5,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
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
                            Text(stringResource(R.string.model_edit_streaming), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.model_edit_streaming_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = useStream, onCheckedChange = { useStream = it })
                    }
                }

                // ===== 思考模式开关 =====
                if (supportsThinking) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.model_edit_thinking), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                                when (provider) {
                                    LlmProvider.DeepSeek -> stringResource(R.string.model_edit_thinking_deepseek)
                                    LlmProvider.OpenAI -> stringResource(R.string.model_edit_thinking_openai)
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = enableThinking, onCheckedChange = { enableThinking = it })
                    }

                    // ===== 思考深度选择器 =====
                    if (enableThinking && supportsReasoningEffort) {
                        Spacer(modifier = Modifier.height(12.dp))

                        val effortOptions = when (provider) {
                            LlmProvider.DeepSeek -> listOf("high", "max")
                            LlmProvider.OpenAI -> listOf("low", "medium", "high")
                            else -> emptyList()
                        }

                        val effortLabels = mapOf(
                            "low" to stringResource(R.string.model_edit_effort_low),
                            "medium" to stringResource(R.string.model_edit_effort_medium),
                            "high" to stringResource(R.string.model_edit_effort_high),
                            "max" to stringResource(R.string.model_edit_effort_max),
                            "xhigh" to stringResource(R.string.model_edit_effort_xhigh)
                        )

                        ExposedDropdownMenuBox(
                            expanded = effortExpanded,
                            onExpandedChange = { effortExpanded = !effortExpanded }
                        ) {
                            AppTextField(
                                value = effortLabels[reasoningEffort] ?: reasoningEffort,
                                onValueChange = {},
                                label = stringResource(R.string.model_edit_thinking_effort),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = effortExpanded) },
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = effortExpanded,
                                onDismissRequest = { effortExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                effortOptions.forEach { effort ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                effortLabels[effort] ?: effort,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            reasoningEffort = effort
                                            effortExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

                // ===== MCP 工具调用开关 =====
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.model_edit_enable_mcp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.model_edit_enable_mcp_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = enableMcp, onCheckedChange = { enableMcp = it })
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
