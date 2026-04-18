/*
OpenDroidChat Update Checker
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import android.content.Context
import android.content.pm.PackageManager
import com.hoeteam.opendroidchat.data.SettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

class UpdateChecker(private val context: Context) {

    private val settingsRepository = SettingsRepository(context)

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/HOE-Team/OpenDroidChat/releases"
        private const val GITHUB_RELEASES_PAGE = "https://github.com/HOE-Team/OpenDroidChat/releases"
        private const val GITHUB_ACTIONS_URL = "https://github.com/HOE-Team/OpenDroidChat/actions"
    }

    /**
     * 获取当前应用版本号
     */
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    /**
     * 获取用于比较的清洁本地版本号
     */
    private fun getCleanLocalVersion(): String {
        val rawVersion = getCurrentVersion()
        return if (rawVersion.count { it == '-' } >= 2) {
            rawVersion.substringBeforeLast("-")
        } else {
            rawVersion
        }
    }

    /**
     * 获取当前版本类型
     */
    fun getCurrentVersionType(): VersionType {
        return VersionType.fromVersion(getCurrentVersion())
    }

    /**
     * 检查更新逻辑
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        val rawLocalVersion = getCurrentVersion()
        val cleanLocalVersion = getCleanLocalVersion()
        val allowOtherChannels = settingsRepository.allowOtherChannelsUpdateFlow.firstOrNull() ?: true

        val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
            }
        }

        return try {
            coroutineContext.ensureActive()
            val releases: List<GitHubRelease> = client.get(GITHUB_API_URL) {
                header("Accept", "application/vnd.github.v3+json")
            }.body()
            coroutineContext.ensureActive()

            if (releases.isEmpty()) {
                return UpdateCheckResult(false, null, rawLocalVersion, null, getCurrentVersionType(), "未找到任何发布版本")
            }

            val latestRelease = releases.first()
            val remoteTag = latestRelease.tag_name.trim()

            val currentParsed = VersionParser.parse(cleanLocalVersion)
            val latestParsed = VersionParser.parse(remoteTag)

            val hasUpdate = if (allowOtherChannels) {
                remoteTag != cleanLocalVersion.trim()
            } else {
                if (currentParsed != null && latestParsed != null && latestParsed.versionType == currentParsed.versionType) {
                    latestParsed.isNewerThan(currentParsed)
                } else false
            }

            UpdateCheckResult(
                hasUpdate = hasUpdate,
                latestRelease = latestRelease,
                currentVersion = rawLocalVersion,
                latestVersion = remoteTag,
                versionType = latestParsed?.versionType ?: VersionType.fromVersion(remoteTag),
                error = null
            )

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            UpdateCheckResult(false, null, rawLocalVersion, null, getCurrentVersionType(), "检查更新失败: ${e.message}")
        } finally {
            client.close()
        }
    }

    suspend fun checkForUpdates(targetType: VersionType): UpdateCheckResult = checkForUpdates()

    fun getReleasesPageUrl(): String = GITHUB_RELEASES_PAGE
    fun getActionsPageUrl(): String = GITHUB_ACTIONS_URL
    fun getVersionPageUrl(versionTag: String): String = "https://github.com/HOE-Team/OpenDroidChat/releases/tag/$versionTag"
}
