/*
OpenDroidChat Chat Input Component - M3 Expressive Refactored (Circular Nested & Transparent Background)
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.data.SelectedFile

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    selectedFile: SelectedFile? = null,
    onAddFile: () -> Unit = {},
    onRemoveFile: () -> Unit = {}
) {
    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    val addFileInteractionSource = remember { MutableInteractionSource() }
    val isAddPressed by addFileInteractionSource.collectIsPressedAsState()

    val isFabEnabled = (text.isNotBlank() || selectedFile != null) && !isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (selectedFile != null) {
            FileChip(
                fileName = selectedFile.fileName,
                isCodeFile = selectedFile.isCodeFile,
                onRemove = onRemoveFile,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("输入消息...") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (isFabEnabled) onSend() }),
            singleLine = false,
            maxLines = 5,
            enabled = !isLoading,
            shape = CircleShape,

            leadingIcon = {
                val addBtnScale by animateFloatAsState(
                    targetValue = if (isAddPressed) 0.9f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy)
                )

                IconButton(
                    onClick = onAddFile,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(addBtnScale)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "添加文件",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },

            trailingIcon = {
                val fabScale by animateFloatAsState(
                    targetValue = if (isFabPressed) 0.9f else if (isFabEnabled) 1f else 0.85f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy)
                )

                val fabCorner by animateDpAsState(
                    targetValue = if (isLoading) 10.dp else 18.dp,
                    animationSpec = spring(Spring.DampingRatioLowBouncy)
                )

                FloatingActionButton(
                    onClick = { if (isFabEnabled) onSend() },
                    containerColor = if (isFabEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFabEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(fabCorner),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp)
                        .scale(fabScale),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    interactionSource = fabInteractionSource
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5f.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FileChip(
    fileName: String,
    isCodeFile: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 8.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = if (isCodeFile) Icons.Filled.Code else Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "移除文件",
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onRemove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
