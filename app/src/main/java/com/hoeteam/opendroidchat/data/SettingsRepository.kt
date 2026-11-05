/*
OpenDroidChat Settings Repository
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.booleanPreferencesKey

// DataStore 实例 (在 Context 扩展属性中定义)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    // 实例化加密工具
    private val encryptionUtil = KeyStoreEncryptionUtil()

    private object PreferencesKeys {
        val MODELS_LIST = stringPreferencesKey("llm_models_list")
        val CURRENT_MODEL_ID = stringPreferencesKey("current_model_id")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val AUTO_UPDATE_CHECK = booleanPreferencesKey("auto_update_check")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val MCP_SERVERS_LIST = stringPreferencesKey("mcp_servers_list")
        val ACTIVE_CONVERSATION_ID = stringPreferencesKey("active_conversation_id")
        val RESUME_LAST_CONVERSATION = booleanPreferencesKey("resume_last_conversation")
        val LEGAL_ACCEPTED = booleanPreferencesKey("legal_accepted")
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<LlmModel>>() {}.type
    private val mcpServerListType = object : TypeToken<List<McpServer>>() {}.type

    // 内存缓存：缓存解密后的模型列表，避免重复解析 JSON 和解密 API Key
    @Volatile
    private var cachedDecryptedModels: List<LlmModel>? = null

    // 内存缓存：当前的模型 ID（用于快速判断是否变化）
    @Volatile
    private var lastRawJson: String? = null

    // 1. 模型列表 Flow (READ: 从 DataStore 读取后 -> 解密 API Key)
    //    增加内存缓存，避免重复解密
    val allModelsFlow: Flow<List<LlmModel>> = dataStore.data.map { preferences ->
        val jsonString = preferences[PreferencesKeys.MODELS_LIST] ?: "[]"

        // 如果 JSON 未变化且已有缓存，直接返回缓存结果
        if (jsonString == lastRawJson && cachedDecryptedModels != null) {
            return@map cachedDecryptedModels!!
        }

        // 更新缓存标记
        lastRawJson = jsonString

        // 从 JSON 字符串解析出包含加密/占位符 key 的 LlmModel 列表
        val modelsToDecrypt = gson.fromJson<List<LlmModel>>(jsonString, listType) ?: emptyList()

        // 遍历列表，解密 API Key
        val decryptedModels = modelsToDecrypt.map { model ->
            // 检查是否有加密标记（apiKey 可能为 null：异常/旧数据）
            if (model.apiKey?.startsWith("encrypted:") == true) {
                try {
                    val storageString = model.apiKey!!.substringAfter("encrypted:")
                    val wrapper = storageString.toCiphertextWrapper()
                    val decryptedKey = encryptionUtil.decrypt(wrapper)

                    // 返回一个包含解密后 key 的新模型实例 (供应用内部使用)
                    model.copy(apiKey = decryptedKey)
                } catch (e: Exception) {
                    // 解密失败（Keystore 错误、密钥损坏等）
                    model.copy(apiKey = "", name = model.name + " (Key Decryption Failed)")
                }
            } else {
                // 如果没有加密标记，出于安全考虑，返回空 Key
                model.copy(apiKey = "", name = model.name + " (Key Storage Invalid)")
            }
        }

        // 写入内存缓存
        cachedDecryptedModels = decryptedModels
        decryptedModels
    }

    // 2. 当前选中的模型 Flow (使用缓存优化)
    val currentModelFlow: Flow<LlmModel?> = combine(
        allModelsFlow,
        dataStore.data.map { it[PreferencesKeys.CURRENT_MODEL_ID] }
    ) { models, currentId ->
        if (models.isEmpty()) {
            null
        } else {
            models.find { it.id == currentId } ?: models.firstOrNull()
        }
    }

    // 3. 设置当前使用的模型 ID
    suspend fun setCurrentModel(modelId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_MODEL_ID] = modelId
        }
    }

    // ==================== 深浅主题设置 ====================
    
    // 6. 主题模式 Flow
    val darkThemeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_THEME_ENABLED] ?: false
    }

    // 7. 设置主题模式
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_ENABLED] = isDark
        }
    }

    // ==================== 自动更新检查设置 ====================

    // 8. 自动更新检查开关 Flow（默认开启）
    val autoUpdateCheckFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_UPDATE_CHECK] ?: true
    }

    // 9. 设置自动更新检查
    suspend fun setAutoUpdateCheck(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_UPDATE_CHECK] = enabled
        }
    }

    // ==================== 应用语言设置 ====================

    // 10. 应用语言 Flow（默认空字符串表示跟随系统）
    val appLanguageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: ""
    }

    // 11. 设置应用语言
    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    // ==================== MCP 服务器设置 ====================

    // 12. MCP 服务器列表 Flow (READ: 读取后解密 API Key)
    val mcpServersFlow: Flow<List<McpServer>> = dataStore.data.map { preferences ->
        val jsonString = preferences[PreferencesKeys.MCP_SERVERS_LIST] ?: "[]"
        val servers = gson.fromJson<List<McpServer>>(jsonString, mcpServerListType) ?: emptyList()
        servers.map { server ->
            // 检查是否有加密标记（apiKey 可能为 null：旧数据未包含该字段）
            if (server.apiKey?.startsWith("encrypted:") == true) {
                try {
                    val storageString = server.apiKey!!.substringAfter("encrypted:")
                    val wrapper = storageString.toCiphertextWrapper()
                    server.copy(apiKey = encryptionUtil.decrypt(wrapper))
                } catch (e: Exception) {
                    server.copy(apiKey = "")
                }
            } else {
                // 未加密的 Key 出于安全考虑不返回明文
                server.copy(apiKey = "")
            }
        }
    }

    // 13. 添加或更新 MCP 服务器
    suspend fun addOrUpdateMcpServer(server: McpServer) {
        mcpServersFlow.firstOrNull()?.let { currentServers ->
            val updatedList = currentServers
                .filter { it.id != server.id }
                .toMutableList()
            updatedList.add(server)
            saveMcpServersList(updatedList)
        }
    }

    // 14. 删除 MCP 服务器
    suspend fun deleteMcpServer(server: McpServer) {
        mcpServersFlow.firstOrNull()?.let { currentServers ->
            val updatedList = currentServers.filter { it.id != server.id }
            saveMcpServersList(updatedList)
        }
    }

    // 15. 更新某个 MCP 服务器的启用状态
    suspend fun setMcpServerEnabled(serverId: String, enabled: Boolean) {
        mcpServersFlow.firstOrNull()?.let { currentServers ->
            val updatedList = currentServers.map { server ->
                if (server.id == serverId) server.copy(enabled = enabled) else server
            }
            saveMcpServersList(updatedList)
        }
    }

    // 内部函数：保存整个 MCP 服务器列表 (WRITE: 加密 API Key 后保存)
    private suspend fun saveMcpServersList(servers: List<McpServer>) {
        // 在后台线程执行加密操作
        withContext(Dispatchers.Default) {
            val serversToSave = servers.map { server ->
                val key = server.apiKey.orEmpty()
                if (key.isNotEmpty()) {
                    // 只有当传入的是明文（没有 "encrypted:" 前缀）时才进行加密
                    if (!key.startsWith("encrypted:")) {
                        try {
                            val wrapper = encryptionUtil.encrypt(key)
                            server.copy(apiKey = "encrypted:${wrapper.toStorageString()}")
                        } catch (e: Exception) {
                            server.copy(apiKey = "")
                        }
                    } else {
                        server // 已经加密的直接使用
                    }
                } else {
                    server.copy(apiKey = "") // Key 为空时，清空
                }
            }

            dataStore.edit { preferences ->
                val jsonString = gson.toJson(serversToSave)
                preferences[PreferencesKeys.MCP_SERVERS_LIST] = jsonString
            }
        }
    }

    // 4. 添加新模型 (或更新现有模型)
    suspend fun addOrUpdateModel(newModel: LlmModel) {
        // 先获取明文列表
        allModelsFlow.firstOrNull()?.let { currentModels ->
            val updatedList = currentModels
                // 确保要更新的模型传入的是未加密的明文 Key
                .filter { it.id != newModel.id }
                .toMutableList()
            updatedList.add(newModel)
            saveModelsList(updatedList)
        }
    }

    // 5. 删除模型
    suspend fun deleteModel(model: LlmModel) {
        allModelsFlow.firstOrNull()?.let { currentModels ->
            val updatedList = currentModels.filter { it.id != model.id }
            saveModelsList(updatedList)
        }
    }

    // 内部函数：保存整个模型列表 (WRITE: 必须加密 API Key 后 -> 保存到 DataStore)
    private suspend fun saveModelsList(models: List<LlmModel>) {
        // 在后台线程执行加密操作
        withContext(Dispatchers.Default) {
            // 遍历列表，加密 API Key
            val modelsToSave = models.map { model ->
                val key = model.apiKey.orEmpty()
                if (key.isNotEmpty()) {
                    // 只有当传入的 Key 是明文时（没有 "encrypted:" 前缀）才进行加密
                    if (!key.startsWith("encrypted:")) {
                        try {
                            val wrapper = encryptionUtil.encrypt(key)
                            val storageString = wrapper.toStorageString()

                            // 存储加密后的 key，并加上前缀标记
                            model.copy(apiKey = "encrypted:$storageString")
                        } catch (e: Exception) {
                            // 加密失败，不保存 key
                            model.copy(apiKey = "")
                        }
                    } else {
                        model // 已经加密的直接使用
                    }
                } else {
                    model.copy(apiKey = "") // Key 为空时，清空
                }
            }

            // 序列化并保存到 DataStore
            dataStore.edit { preferences ->
                val jsonString = gson.toJson(modelsToSave)
                preferences[PreferencesKeys.MODELS_LIST] = jsonString
            }
        }
    }

    // ==================== 历史对话 ====================

    // 16. 当前打开的会话 ID Flow（null 表示当前是未保存的新对话）
    //     记录它可以让应用在冷启动后自动恢复上次的对话
    val activeConversationIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_CONVERSATION_ID]
    }

    // 17. 记住/清除当前会话 ID
    suspend fun setActiveConversationId(conversationId: String?) {
        dataStore.edit { preferences ->
            if (conversationId.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.ACTIVE_CONVERSATION_ID)
            } else {
                preferences[PreferencesKeys.ACTIVE_CONVERSATION_ID] = conversationId
            }
        }
    }

    // ==================== 启动行为 ====================

    // 18. 启动时是否恢复上次对话 Flow（默认 false：每次启动都从新对话开始）
    val resumeLastConversationFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.RESUME_LAST_CONVERSATION] ?: false
    }

    // 19. 设置启动时是否恢复上次对话
    suspend fun setResumeLastConversation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_LAST_CONVERSATION] = enabled
        }
    }

    // ==================== 法律条款同意 ====================

    // 20. 是否已同意法律与隐私政策（缺失时视为未同意）
    val legalAcceptedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LEGAL_ACCEPTED] ?: false
    }

    // 21. 记录用户已同意法律条款
    suspend fun setLegalAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEGAL_ACCEPTED] = accepted
        }
    }
}

