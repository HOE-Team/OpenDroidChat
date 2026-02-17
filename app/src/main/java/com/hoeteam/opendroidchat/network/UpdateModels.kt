package com.hoeteam.opendroidchat.network

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GitHubRelease(
    val tag_name: String,           // 版本标签，如 "nightly-1.0-20260216a" 或 "beta-1.0-20260216" 或 "v1.0.0"
    val name: String,                // 发布名称
    val body: String,                // 发布说明
    val prerelease: Boolean,         // 是否为预发布
    val published_at: String,        // 发布时间
    val html_url: String,            // Release 页面URL
    val assets: List<ReleaseAsset>?  // 发布的资源文件
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ReleaseAsset(
    val name: String,                // 文件名
    val browser_download_url: String // 下载URL
)

/**
 * 版本类型枚举
 */
enum class VersionType {
    NIGHTLY,    //  nightly-前缀，日期版本
    BETA,       //  beta-前缀，日期版本
    STABLE;     //  v前缀，语义化版本

    companion object {
        fun fromVersion(version: String): VersionType {
            return when {
                version.lowercase().startsWith("nightly") -> NIGHTLY
                version.lowercase().startsWith("Beta") -> BETA
                version.lowercase().startsWith("Stable") -> STABLE
                else -> STABLE // 默认当作稳定版
            }
        }

        fun fromTag(tag: String): VersionType {
            return when {
                tag.lowercase().startsWith("nightly") -> NIGHTLY
                tag.lowercase().startsWith("beta") -> BETA
                else -> STABLE
            }
        }

        fun getTagPrefix(type: VersionType): String {
            return when (type) {
                VersionType.NIGHTLY -> "nightly"
                VersionType.BETA -> "beta"
                VersionType.STABLE -> "v"
            }
        }
    }
}

/**
 * 版本检查结果
 */
data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val latestRelease: GitHubRelease?,
    val currentVersion: String,
    val latestVersion: String?,
    val versionType: VersionType,
    val error: String? = null
)

/**
 * 解析版本信息 - 支持多种版本格式
 */
sealed class ParsedVersion {
    abstract val fullString: String
    abstract val versionType: VersionType

    /**
     * 比较版本，判断当前版本是否比另一个版本新
     */
    abstract fun isNewerThan(other: ParsedVersion): Boolean

    /**
     * Nightly版本格式: nightly-1.0-20260216a (11)
     */
    data class NightlyVersion(
        override val fullString: String,
        val prefix: String,              // nightly
        val versionCode: String,         // 1.0
        val dateCode: String,            // 20260216a
        val buildNumber: String?         // 11
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.NIGHTLY

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is NightlyVersion) return true // 不同类型，当前版本更新

            // 提取日期数字部分
            val thisDate = dateCode.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            val otherDate = other.dateCode.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

            return when {
                thisDate > otherDate -> true
                thisDate < otherDate -> false
                else -> {
                    // 日期相同，比较字母后缀
                    val thisSuffix = dateCode.dropWhile { it.isDigit() }
                    val otherSuffix = other.dateCode.dropWhile { it.isDigit() }
                    thisSuffix > otherSuffix
                }
            }
        }
    }

    /**
     * Beta版本格式: beta-1.0-20260216
     */
    data class BetaVersion(
        override val fullString: String,
        val prefix: String,              // beta
        val versionCode: String,         // 1.0
        val dateCode: String             // 20260216
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.BETA

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is BetaVersion) return true

            val thisDate = dateCode.toIntOrNull() ?: 0
            val otherDate = other.dateCode.toIntOrNull() ?: 0
            return thisDate > otherDate
        }
    }

    /**
     * 稳定版本格式: v1.2.3 或 1.2.3
     */
    data class StableVersion(
        override val fullString: String,
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.STABLE

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is StableVersion) return true

            return when {
                major > other.major -> true
                major < other.major -> false
                minor > other.minor -> true
                minor < other.minor -> false
                else -> patch > other.patch
            }
        }
    }
}

/**
 * 版本解析工具
 */
object VersionParser {

    fun parse(versionString: String): ParsedVersion? {
        // 移除可能的 "Version " 前缀
        val cleanVersion = versionString.removePrefix("Version ").trim()

        // 尝试匹配 Nightly 版本: nightly-1.0-20260216a (11)
        val nightlyRegex = """^([nN]ightly)-(\d+\.\d+)-(\d+[a-zA-Z]?)(?:\s*\((\d+)\))?$""".toRegex()
        nightlyRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, dateCode, buildNumber) = it.destructured
            return ParsedVersion.NightlyVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                dateCode = dateCode,
                buildNumber = buildNumber.ifBlank { null }
            )
        }

        // 尝试匹配 Beta 版本: beta-1.0-20260216
        val betaRegex = """^([bB]eta)-(\d+\.\d+)-(\d+)$""".toRegex()
        betaRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, dateCode) = it.destructured
            return ParsedVersion.BetaVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                dateCode = dateCode
            )
        }

        // 尝试匹配稳定版本: v1.2.3 或 1.2.3
        val stableRegex = """^v?(\d+)\.(\d+)\.(\d+)$""".toRegex()
        stableRegex.find(cleanVersion)?.let {
            val (major, minor, patch) = it.destructured
            return ParsedVersion.StableVersion(
                fullString = versionString,
                major = major.toInt(),
                minor = minor.toInt(),
                patch = patch.toInt()
            )
        }

        return null
    }

    /**
     * 获取版本类型
     */
    fun getVersionType(versionString: String): VersionType {
        return VersionType.fromVersion(versionString)
    }

    /**
     * 检查版本是否匹配指定类型
     */
    fun isVersionType(versionString: String, type: VersionType): Boolean {
        return getVersionType(versionString) == type
    }
}