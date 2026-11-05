package com.hoeteam.opendroidchat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoeteam.opendroidchat.data.Conversation
import com.hoeteam.opendroidchat.data.ConversationRepository
import com.hoeteam.opendroidchat.data.Message
import com.hoeteam.opendroidchat.data.Sender
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 历史对话文件存储的仪器测试：验证保存、读取、排序、删除与索引重建。
 */
@RunWith(AndroidJUnit4::class)
class ConversationRepositoryTest {

    private lateinit var directory: File
    private lateinit var repository: ConversationRepository

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.filesDir, "conversations")
        directory.deleteRecursively()

        repository = ConversationRepository(context)
        repository.refresh()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun savesAndLoadsConversation() = runBlocking {
        val conversation = Conversation(
            title = "第一条对话",
            messages = listOf(Message(text = "你好", sender = Sender.USER))
        )

        repository.save(conversation)

        assertEquals(1, repository.conversations.value.size)
        assertEquals("第一条对话", repository.conversations.value.single().title)

        val loaded = repository.load(conversation.id)
        assertNotNull(loaded)
        assertEquals("第一条对话", loaded!!.title)
        assertEquals("你好", loaded.messages.single().text)
        assertEquals(Sender.USER, loaded.messages.single().sender)
    }

    @Test
    fun listIsSortedByUpdatedAtDescending() = runBlocking {
        val older = Conversation(title = "旧对话", updatedAt = 1_000L)
        val newer = Conversation(title = "新对话", updatedAt = 2_000L)

        repository.save(older)
        repository.save(newer)

        assertEquals(listOf("新对话", "旧对话"), repository.conversations.value.map { it.title })
    }

    @Test
    fun deleteRemovesOnlyTargetConversation() = runBlocking {
        val first = Conversation(title = "保留")
        val second = Conversation(title = "删除")
        repository.save(first)
        repository.save(second)

        repository.delete(second.id)

        assertEquals(listOf("保留"), repository.conversations.value.map { it.title })
        assertNotNull(repository.load(first.id))
        assertNull(repository.load(second.id))
    }

    @Test
    fun deleteAllClearsEverything() = runBlocking {
        repository.save(Conversation(title = "A"))
        repository.save(Conversation(title = "B"))

        repository.deleteAll()

        assertTrue(repository.conversations.value.isEmpty())
        assertEquals(0, directory.listFiles()?.size ?: 0)
    }

    @Test
    fun indexIsRebuiltWhenMissing() = runBlocking {
        repository.save(Conversation(title = "可重建"))

        File(directory, "index.json").delete()

        val freshRepository = ConversationRepository(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        freshRepository.refresh()

        assertEquals(listOf("可重建"), freshRepository.conversations.value.map { it.title })
    }
}
