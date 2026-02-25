/*
OpenDroidChat Update Manager
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hoeteam.opendroidchat.network.UpdateChecker
import com.hoeteam.opendroidchat.network.UpdateCheckResult
import com.hoeteam.opendroidchat.network.VersionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.updateDataStore by preferencesDataStore(name = "update_settings")

/**
 * 更新管理器，负责检查更新和记录已忽略的版本
 */
class UpdateManager(private val context: Context) {

    private val updateChecker = UpdateChecker(context)

    private object PreferencesKeys {
        val LAST_CHECK_TIME = stringPreferencesKey("last_check_time")
        val IGNORED_VERSION = stringPreferencesKey("ignored_version")
        val UPDATE_NOTIFICATION_SHOWN = booleanPreferencesKey("update_notification_shown")
    }

    /**
     * 获取当前版本类型
     */
    fun getCurrentVersionType(): VersionType {
        return updateChecker.getCurrentVersionType()
    }

    /**
     * 检查是否有新版本 - 根据当前版本类型自动检查
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                coroutineContext.ensureActive()
                updateChecker.checkForUpdates()
            } catch (e: Exception) {
                UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = getCurrentVersion(),
                    latestVersion = null,
                    versionType = getCurrentVersionType(),
                    error = "检查失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 检查是否有新版本 - 指定版本类型
     */
    suspend fun checkForUpdates(targetType: VersionType): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                coroutineContext.ensureActive()
                updateChecker.checkForUpdates(targetType)
            } catch (e: Exception) {
                UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = getCurrentVersion(),
                    latestVersion = null,
                    versionType = targetType,
                    error = "检查失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取检查结果 Flow
     */
    fun checkForUpdatesFlow(): Flow<UpdateCheckResult> = flow {
        emit(checkForUpdates())
    }

    /**
     * 获取当前版本号
     */
    fun getCurrentVersion(): String {
        return updateChecker.getCurrentVersion()
    }

    /**
     * 打开下载页面 (GitHub Releases 或 Actions)
     */
    fun openDownloadPage(versionTag: String? = null, versionType: VersionType = VersionType.STABLE) {
        val url = when {
            versionTag != null -> updateChecker.getVersionPageUrl(versionTag)
            versionType == VersionType.NIGHTLY -> updateChecker.getActionsPageUrl()
            else -> updateChecker.getReleasesPageUrl()
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 打开 GitHub Actions 页面（用于 Nightly 版本）
     */
    fun openActionsPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateChecker.getActionsPageUrl()))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 记录已忽略的版本
     */
    suspend fun ignoreVersion(versionTag: String) {
        context.updateDataStore.edit { preferences ->
            preferences[PreferencesKeys.IGNORED_VERSION] = versionTag
        }
    }

    /**
     * 获取已忽略的版本
     */
    suspend fun getIgnoredVersion(): String? {
        return context.updateDataStore.data.map { preferences ->
            preferences[PreferencesKeys.IGNORED_VERSION]
        }.firstOrNull()
    }

    /**
     * 标记更新通知已显示
     */
    suspend fun markNotificationShown() {
        context.updateDataStore.edit { preferences ->
            preferences[PreferencesKeys.UPDATE_NOTIFICATION_SHOWN] = true
        }
    }

    /**
     * 获取更新通知是否已显示
     */
    suspend fun isNotificationShown(): Boolean {
        return context.updateDataStore.data.map { preferences ->
            preferences[PreferencesKeys.UPDATE_NOTIFICATION_SHOWN] ?: false
        }.firstOrNull() ?: false
    }

    /**
     * 重置通知状态（当有更新版本时）
     */
    suspend fun resetNotificationState() {
        context.updateDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.UPDATE_NOTIFICATION_SHOWN)
        }
    }
}