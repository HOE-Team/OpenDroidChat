/*
OpenDroidChat Markdown Free Text Field
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * 通用的多行输入框。
 *
 * **不要换回 OutlinedTextField / TextField**：
 * 当前解析到的 Compose Foundation 1.12 中，Material3 的 TextField 仍以旧方式
 * （`indication = LocalIndication`）调用 clickable，而 foundation 已强制要求
 * IndicationNodeFactory，一旦聚焦就会抛 IllegalArgumentException 崩溃。
 * 这里用 BasicTextField + 自绘边框，对外保持与 OutlinedTextField 接近的观感。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 5,
    minLines: Int = 1,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val labelText = label
    val placeholderText = placeholder

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            // 可用与只读（选择框）统一使用 outlineVariant：
            // outline 对比过强，会让输入框看着比同屏的选择框"更重"，破坏一致性
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (labelText != null) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium,
                    // 标签一律用主色。不看 enabled：选择框是以"禁用态只读展示"的方式实现的，
                    // 如果让标签跟随 enabled 变色，同屏就会出现"输入框标签青色、选择框标签黑色"的
                    // 两套文字规范，用户会误以为选择框坏掉了。
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.invoke()
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    textStyle = textStyle.copy(
                        // 真实内容用最高对比度。禁用态也不再降低透明度：
                        // 选择框的值是有效信息，不是"不可用的灰字"
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty() && placeholderText != null) {
                                Text(
                                    text = placeholderText,
                                    style = textStyle,
                                    // 占位文字必须明显弱于真实内容，否则会被误读成"已经填好的值"
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    maxLines = maxLines
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                trailingIcon?.invoke()
            }
        }
    }
}
