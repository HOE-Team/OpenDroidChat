/*
OpenDroidChat Conversation Drawer Content
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.R
import com.hoeteam.opendroidchat.data.ConversationSummary

/**
 * 侧边抽屉里的历史对话内容：
 * 新建对话、最近对话列表、删除与清空。
 *
 * 桌面端常见的「左侧会话栏」形态，放进 [androidx.compose.material3.ModalNavigationDrawer]
 * 后不再单独占用一个页面，聊天页可以一直留在屏幕后面。
 */
@Composable
fun ConversationDrawerContent(
    conversations: List<ConversationSummary>,
    activeConversationId: String?,
    isGenerating: Boolean,
    onNewConversation: () -> Unit,
    onOpenConversation: (ConversationSummary) -> Unit,
    onDeleteConversation: (ConversationSummary) -> Unit,
    onClearAllConversations: () -> Unit
) {
    var conversationToDelete by remember { mutableStateOf<ConversationSummary?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        FilledTonalButton(
            onClick = onNewConversation,
            enabled = !isGenerating,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.chat_new_conversation))
        }

        if (isGenerating) {
            Text(
                text = stringResource(R.string.history_busy),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (conversations.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { summary ->
                    ConversationListItem(
                        summary = summary,
                        isActive = summary.id == activeConversationId,
                        onClick = { onOpenConversation(summary) },
                        onDelete = { conversationToDelete = summary },
                        enabled = !isGenerating
                    )
                }
            }

            HorizontalDivider()
            TextButton(
                onClick = { showClearAllDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.history_clear_all))
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    // 删除单条对话确认
    conversationToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(R.string.history_delete_confirm_title),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.history_delete_confirm_message,
                        target.title.ifBlank { stringResource(R.string.history_untitled) }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(target)
                        conversationToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.history_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text(stringResource(R.string.history_cancel))
                }
            }
        )
    }

    // 清空全部确认
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(R.string.history_clear_all_confirm_title),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = { Text(stringResource(R.string.history_clear_all_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllConversations()
                        showClearAllDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.history_clear_all_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.history_cancel))
                }
            }
        )
    }
}
