package com.hoeteam.opendroidchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// DataStore 实例 (在 Context 扩展属性中定义)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val MODELS_LIST = stringPreferencesKey("llm_models_list")
        val CURRENT_MODEL_ID = stringPreferencesKey("current_model_id")
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<LlmModel>>() {}.type

    // 1. 模型列表 Flow
    val allModelsFlow: Flow<List<LlmModel>> = dataStore.data.map { preferences ->
        val jsonString = preferences[PreferencesKeys.MODELS_LIST] ?: "[]"
        gson.fromJson(jsonString, listType) ?: emptyList()
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

    // 4. 添加新模型 (或更新现有模型)
    suspend fun addOrUpdateModel(newModel: LlmModel) {
        allModelsFlow.firstOrNull()?.let { currentModels ->
            val updatedList = currentModels
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

    // 内部函数：保存整个模型列表
    private suspend fun saveModelsList(models: List<LlmModel>) {
        dataStore.edit { preferences ->
            val jsonString = gson.toJson(models)
            preferences[PreferencesKeys.MODELS_LIST] = jsonString
        }
    }
}