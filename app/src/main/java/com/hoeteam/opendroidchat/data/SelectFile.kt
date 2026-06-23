/*
OpenDroidChat Select File Module - SAF (Storage Access Framework)
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

// 支持的文本文件 MIME 类型
private val SUPPORTED_TEXT_MIME_TYPES = arrayOf(
    "text/*",
    "application/json",
    "application/xml",
    "application/javascript",
    "application/x-php",
    "application/x-yaml",
    "application/x-sh",
    "application/x-python-code",
    "application/x-java-archive",
    "application/x-toml",
    "application/x-httpd-php",
    "application/octet-stream" // 对于没有 MIME 的文件，尝试读取
)

// 代码文件扩展名列表
private val CODE_EXTENSIONS = setOf(
    "kt", "java", "kts", "py", "js", "ts", "tsx", "jsx",
    "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb",
    "php", "swift", "scala", "groovy", "gradle",
    "xml", "json", "yaml", "yml", "toml", "ini", "cfg",
    "sh", "bash", "zsh", "ps1", "bat", "cmd",
    "sql", "r", "m", "mm", "dart", "lua", "pl",
    "html", "htm", "css", "scss", "sass", "less",
    "md", "txt", "properties", "env", "gitignore",
    "dockerfile", "makefile", "cmake"
)

/**
 * 使用 SAF 框架选择文件
 * 返回一个 Intent 以便启动文件选择器
 */
fun createFilePickerIntent(): android.content.Intent {
    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(android.content.Intent.CATEGORY_OPENABLE)
        type = "*/*"
        // 优先显示文本文件
        putExtra(android.content.Intent.EXTRA_MIME_TYPES, SUPPORTED_TEXT_MIME_TYPES)
    }
    return intent
}

/**
 * 判断文件名是否是代码文件
 */
fun isCodeFile(fileName: String): Boolean {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in CODE_EXTENSIONS
}

/**
 * 判断文件是否是可读的文本文件（基于文件大小限制）
 */
fun isTextFile(context: Context, uri: Uri): Boolean {
    return try {
        val sizeLimit = 10 * 1024 * 1024 // 10MB 限制
        val size = getFileSize(context, uri)
        size <= sizeLimit
    } catch (e: Exception) {
        false
    }
}

/**
 * 获取文件名
 */
fun getFileName(context: Context, uri: Uri): String {
    var name = "unknown_file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex) ?: "unknown_file"
        }
    }
    return name
}

/**
 * 获取文件大小
 */
private fun getFileSize(context: Context, uri: Uri): Long {
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex >= 0 && cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }
    return size
}

/**
 * 读取文本文件内容
 */
fun readFileContent(context: Context, uri: Uri): String {
    val content = StringBuilder()
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("无法打开文件")
    inputStream.use { stream ->
        BufferedReader(InputStreamReader(stream, "UTF-8")).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                content.append(line)
                content.append('\n')
                line = reader.readLine()
            }
        }
    }
    return content.toString()
}

/**
 * 根据 Uri 选择并读取文件，返回 SelectedFile
 */
fun selectFileFromUri(context: Context, uri: Uri): SelectedFile? {
    return try {
        val fileName = getFileName(context, uri)
        if (!isTextFile(context, uri)) return null
        val content = readFileContent(context, uri)
        val codeFile = isCodeFile(fileName)
        SelectedFile(
            uri = uri.toString(),
            fileName = fileName,
            isCodeFile = codeFile,
            content = content
        )
    } catch (e: Exception) {
        null
    }
}