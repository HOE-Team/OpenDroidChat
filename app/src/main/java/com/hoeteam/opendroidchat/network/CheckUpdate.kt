/*
OpenDroidChat Update Checker
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.network

import android.content.Context
import android.content.pm.PackageManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

class UpdateChecker(private val context: Context) {

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
     * 获取当前版本类型
     */
    fun getCurrentVersionType(): VersionType {
        return VersionType.fromVersion(getCurrentVersion())
    }

    /**
     * 检查是否有新版本 - 根据当前版本类型自动检查对应类型的更新
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        val currentVersion = getCurrentVersion()
        val currentType = VersionType.fromVersion(currentVersion)

        return checkForUpdates(currentType)
    }

    /**
     * 检查是否有新版本 - 指定要检查的版本类型
     */
    suspend fun checkForUpdates(targetType: VersionType): UpdateCheckResult {
        val currentVersion = getCurrentVersion()
        val currentParsed = VersionParser.parse(currentVersion)

        if (currentParsed == null) {
            return UpdateCheckResult(
                hasUpdate = false,
                latestRelease = null,
                currentVersion = currentVersion,
                latestVersion = null,
                versionType = targetType,
                error = "无法解析当前版本格式: $currentVersion"
            )
        }

        // 如果是 Nightly 版本，返回特殊提示
        if (targetType == VersionType.NIGHTLY) {
            return UpdateCheckResult(
                hasUpdate = false,
                latestRelease = null,
                currentVersion = currentVersion,
                latestVersion = null,
                versionType = targetType,
                error = "Nightly版本通过GitHub Actions自动构建，请访问Actions页面下载最新构建"
            )
        }

        // 创建新的 HttpClient 实例（仅用于 Beta/Stable）
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
            // 检查协程是否被取消
            coroutineContext.ensureActive()

            // 获取所有 Releases
            val releases: List<GitHubRelease> = client.get(GITHUB_API_URL) {
                header("Accept", "application/vnd.github.v3+json")
            }.body()

            // 检查协程是否被取消
            coroutineContext.ensureActive()

            // 根据目标类型过滤版本（大小写不敏感）
            val filteredReleases = when (targetType) {
                VersionType.NIGHTLY -> emptyList() // Nightly 不检查 Releases
                VersionType.BETA -> releases.filter {
                    it.tag_name.lowercase().startsWith("beta")
                }
                VersionType.STABLE -> releases.filter {
                    it.tag_name.lowercase().startsWith("stable")
                }
            }

            if (filteredReleases.isEmpty()) {
                return UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = currentVersion,
                    latestVersion = null,
                    versionType = targetType,
                    error = "未找到 ${targetType.name.lowercase()} 版本"
                )
            }

            // 按发布时间排序
            val sortedReleases = filteredReleases.sortedByDescending { it.published_at }
            val latestRelease = sortedReleases.first()

            // 解析最新版本
            val latestParsed = VersionParser.parse(latestRelease.tag_name)

            if (latestParsed == null) {
                return UpdateCheckResult(
                    hasUpdate = false,
                    latestRelease = null,
                    currentVersion = currentVersion,
                    latestVersion = latestRelease.tag_name,
                    versionType = targetType,
                    error = "无法解析最新版本格式: ${latestRelease.tag_name}"
                )
            }

            // 比较版本
            val hasUpdate = latestParsed.isNewerThan(currentParsed)

            UpdateCheckResult(
                hasUpdate = hasUpdate,
                latestRelease = latestRelease,
                currentVersion = currentVersion,
                latestVersion = latestRelease.tag_name,
                versionType = targetType,
                error = null
            )

        } catch (e: CancellationException) {
            // 重新抛出取消异常，让上层处理
            throw e
        } catch (e: Exception) {
            UpdateCheckResult(
                hasUpdate = false,
                latestRelease = null,
                currentVersion = currentVersion,
                latestVersion = null,
                versionType = targetType,
                error = when (e) {
                    is ClientRequestException -> "网络请求错误: ${e.response.status.value}"
                    is UnknownHostException -> "网络连接失败，请检查网络"
                    else -> "检查更新失败: ${e.message}"
                }
            )
        } finally {
            client.close()
        }
    }

    /**
     * 获取 GitHub Releases 页面 URL
     */
    fun getReleasesPageUrl(): String = GITHUB_RELEASES_PAGE

    /**
     * 获取 GitHub Actions 页面 URL（用于 Nightly 版本）
     */
    fun getActionsPageUrl(): String = GITHUB_ACTIONS_URL

    /**
     * 获取指定版本的下载页面 URL
     */
    fun getVersionPageUrl(versionTag: String): String {
        return "https://github.com/HOE-Team/OpenDroidChat/releases/tag/$versionTag"
    }
}