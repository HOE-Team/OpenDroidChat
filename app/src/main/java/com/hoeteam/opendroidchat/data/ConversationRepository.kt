/*
OpenDroidChat Conversation Repository
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Type

/**
 * 历史对话的本地存储。
 *
 * 存储结构（全部位于应用私有目录，不需要任何额外依赖）：
 *
 *     files/conversations/
 *         index.json          -> 列表页需要的轻量摘要（标题 / 时间 / 预览）
 *         <conversationId>.json -> 单个会话的完整消息
 *
 * 之所以「一个会话一个文件」，是为了让一次保存只追加/覆盖当前会话，
 * 而不是像 DataStore 单键那样每次都重写全部历史；索引单独存放则让
 * 列表页不必解析所有会话的消息体。
 */
class ConversationRepository(context: Context) {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)
    private val indexFile = File(directory, INDEX_FILE_NAME)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(McpSegment::class.java, McpSegmentTypeAdapter())
        .create()

    private val conversationType: Type = object : TypeToken<Conversation>() {}.type
    private val summaryListType: Type = object : TypeToken<List<ConversationSummary>>() {}.type

    private val _conversations = MutableStateFlow<List<ConversationSummary>>(emptyList())

    /** 按最近更新时间倒序排列的会话摘要，供历史列表直接订阅 */
    val conversations: StateFlow<List<ConversationSummary>> = _conversations.asStateFlow()

    // 所有磁盘读写串行化，避免并发保存互相覆盖索引
    private val mutex = Mutex()

    @Volatile
    private var indexLoaded = false

    /** 读取（或首次重建）索引，应用启动时调用一次即可 */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { loadIndexLocked() }
    }

    /** 读取某个会话的完整内容，不存在或损坏时返回 null */
    suspend fun load(conversationId: String): Conversation? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readConversation(conversationFile(conversationId))
        }
    }

    /** 新建或更新一个会话（按 id 判断），并同步刷新列表摘要 */
    suspend fun save(conversation: Conversation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadIndexLocked()
            ensureDirectory()

            val summary = conversation.toSummary()
            // 先写正文，成功后再写索引：即使进程在两步之间被杀掉，
            // 索引里也不会出现指向残缺文件的条目
            writeAtomically(conversationFile(conversation.id), gson.toJson(conversation))

            val updated = (_conversations.value.filterNot { it.id == summary.id } + summary)
                .sortedWith(compareByDescending { it.updatedAt })
            writeIndexLocked(pruneLocked(updated))
        }
    }

    /** 删除单个会话 */
    suspend fun delete(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadIndexLocked()
            conversationFile(conversationId).delete()
            writeIndexLocked(_conversations.value.filterNot { it.id == conversationId })
        }
    }

    /** 清空全部历史对话 */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureDirectory()
            directory.listFiles()?.forEach { it.delete() }
            indexLoaded = true
            publish(emptyList())
        }
    }

    // ------------------- 内部实现 -------------------

    private fun loadIndexLocked() {
        if (indexLoaded) return
        indexLoaded = true
        ensureDirectory()

        // 索引缺失（首次使用 / 被清理）时，扫描会话文件重建
        val cached = readIndex()
        if (cached != null) {
            publish(cached.sortedWith(compareByDescending { it.updatedAt }))
        } else {
            rebuildIndexLocked()
        }
    }

    private fun rebuildIndexLocked() {
        val summaries = directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(FILE_SUFFIX) && it.name != INDEX_FILE_NAME }
            ?.mapNotNull { file -> readConversation(file)?.toSummary() }
            ?.sortedWith(compareByDescending { it.updatedAt })
            .orEmpty()
        writeIndexLocked(summaries)
    }

    private fun readIndex(): List<ConversationSummary>? {
        if (!indexFile.exists()) return null
        return try {
            gson.fromJson<List<ConversationSummary>>(indexFile.readText(), summaryListType)
        } catch (_: Exception) {
            // 索引损坏时返回 null，交给重建逻辑兜底
            null
        }
    }

    private fun readConversation(file: File): Conversation? {
        if (!file.exists()) return null
        return try {
            gson.fromJson<Conversation>(file.readText(), conversationType)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeIndexLocked(summaries: List<ConversationSummary>) {
        ensureDirectory()
        writeAtomically(indexFile, gson.toJson(summaries, summaryListType))
        publish(summaries)
    }

    private fun publish(summaries: List<ConversationSummary>) {
        _conversations.value = summaries
    }

    /** 超出上限时删除最旧的会话，避免历史记录无限增长 */
    private fun pruneLocked(summaries: List<ConversationSummary>): List<ConversationSummary> {
        if (summaries.size <= MAX_CONVERSATIONS) return summaries
        summaries.drop(MAX_CONVERSATIONS).forEach { conversationFile(it.id).delete() }
        return summaries.take(MAX_CONVERSATIONS)
    }

    private fun conversationFile(conversationId: String) = File(directory, conversationId + FILE_SUFFIX)

    private fun ensureDirectory() {
        if (!directory.exists()) directory.mkdirs()
    }

    /**
     * 先写临时文件再重命名，尽量避免「写了一半被系统杀掉」导致的半截 JSON。
     */
    private fun writeAtomically(target: File, content: String) {
        val temp = File(directory, target.name + TEMP_SUFFIX)
        temp.writeText(content)
        if (temp.renameTo(target)) return

        // 某些文件系统不允许覆盖式重命名，退化为「删除旧文件再重命名」
        target.delete()
        if (temp.renameTo(target)) return

        temp.copyTo(target, overwrite = true)
        temp.delete()
    }

    private companion object {
        const val DIRECTORY_NAME = "conversations"
        const val INDEX_FILE_NAME = "index.json"
        const val FILE_SUFFIX = ".json"
        const val TEMP_SUFFIX = ".tmp"

        /** 最多保留的会话数量，超出后自动淘汰最旧的 */
        const val MAX_CONVERSATIONS = 100
    }
}
