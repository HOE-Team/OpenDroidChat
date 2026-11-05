/*
OpenDroidChat Legal Consent Dialog
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoeteam.opendroidchat.R

/**
 * 首次启动时的法律条款同意弹窗。
 *
 * - 未打开过 [LegalScreen] 时，「同意并继续」不可点击
 * - 不能点击遮罩或返回键关闭，必须选择同意或退出
 */
@Composable
fun LegalConsentDialog(
    hasReadLegal: Boolean,
    onViewLegal: () -> Unit,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    AlertDialog(
        // 必须显式做出选择，禁止点外部/返回键关闭
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.legal_consent_title),
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.legal_consent_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = onViewLegal,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.legal_consent_view))
                }

                if (!hasReadLegal) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.legal_consent_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAgree,
                enabled = hasReadLegal
            ) {
                Text(
                    text = stringResource(R.string.legal_consent_agree),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) {
                Text(
                    text = stringResource(R.string.legal_consent_disagree),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
