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
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<LlmModel>>() {}.type

    // 1. 模型列表 Flow (READ: 从 DataStore 读取后 -> 解密 API Key)
    val allModelsFlow: Flow<List<LlmModel>> = dataStore.data.map { preferences ->
        val jsonString = preferences[PreferencesKeys.MODELS_LIST] ?: "[]"

        // 从 JSON 字符串解析出包含加密/占位符 key 的 LlmModel 列表
        val modelsToDecrypt = gson.fromJson<List<LlmModel>>(jsonString, listType) ?: emptyList()

        // 遍历列表，解密 API Key
        modelsToDecrypt.map { model ->
            // 检查是否有加密标记
            if (model.apiKey.startsWith("encrypted:")) {
                try {
                    val storageString = model.apiKey.substringAfter("encrypted:")
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
    }

    // 2. 当前选中的模型 Flow
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
                if (model.apiKey.isNotEmpty()) {
                    // 只有当传入的 Key 是明文时（没有 "encrypted:" 前缀）才进行加密
                    if (!model.apiKey.startsWith("encrypted:")) {
                        try {
                            val wrapper = encryptionUtil.encrypt(model.apiKey)
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
}