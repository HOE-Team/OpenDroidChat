package com.hoeteam.opendroidchat.network

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GitHubRelease(
    val tag_name: String,           // 版本标签，如 "Beta-1.0" 或 "Stable-1.0" 或 "Stable-1.0Fix"
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
    NIGHTLY,    //  nightly-前缀，日期版本（格式：nightly-yymmdd）
    BETA,       //  Beta-前缀，主版本号
    STABLE;     //  Stable-前缀，主版本号

    companion object {
        fun fromVersion(version: String): VersionType {
            val lowerVersion = version.lowercase()
            return when {
                lowerVersion.startsWith("nightly") -> NIGHTLY
                lowerVersion.startsWith("beta") -> BETA
                lowerVersion.startsWith("stable") -> STABLE
                else -> STABLE // 默认当作稳定版
            }
        }

        fun fromTag(tag: String): VersionType {
            val lowerTag = tag.lowercase()
            return when {
                lowerTag.startsWith("nightly") -> NIGHTLY
                lowerTag.startsWith("beta") -> BETA
                lowerTag.startsWith("stable") -> STABLE
                else -> STABLE
            }
        }

        fun getDisplayName(type: VersionType): String {
            return when (type) {
                VersionType.NIGHTLY -> "Nightly 每夜构建版"
                VersionType.BETA -> "Beta 公测版"
                VersionType.STABLE -> "稳定版"
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
    abstract val baseVersion: String // 基础版本号（用于比较）

    /**
     * 比较版本，判断当前版本是否比另一个版本新
     */
    abstract fun isNewerThan(other: ParsedVersion): Boolean

    /**
     * 判断是否为 Hotfix 版本
     */
    abstract val isHotfix: Boolean

    /**
     * Nightly版本格式: nightly-yymmdd
     * 例如: nightly-260222 (（20）26年2月22日)
     */
    data class NightlyVersion(
        override val fullString: String,
        val prefix: String,              // nightly
        val dateCode: String             // yymmdd
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.NIGHTLY
        override val baseVersion: String = "$prefix-$dateCode"
        override val isHotfix: Boolean = false

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is NightlyVersion) return true

            // 将yymmdd格式转换为整数进行比较
            val thisDate = dateCode.toIntOrNull() ?: 0
            val otherDate = other.dateCode.toIntOrNull() ?: 0

            return thisDate > otherDate
        }
    }

    /**
     * 遵循下列Beta版本格式:
     * - 应用versionName: Beta-x.y-Catalog 或 Beta-x.yFix-Catalog
     * - GitHub标签: Beta-x.y 或 Beta-x.yFix
     */
    data class BetaVersion(
        override val fullString: String,
        val prefix: String,              // Beta
        val versionCode: String,          // 1.0
        override val isHotfix: Boolean,            // 是否为 Hotfix 版本
        val hotfixSuffix: String? = null, // Fix 后缀（如果有）
        val catalog: String?              // Catalog (可能为 null)
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.BETA
        override val baseVersion: String = if (isHotfix) {
            "$prefix-$versionCode$hotfixSuffix"
        } else {
            "$prefix-$versionCode"
        }

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is BetaVersion) return true

            // 先比较基础版本号
            val thisBase = versionCode
            val otherBase = other.versionCode

            if (thisBase != otherBase) {
                // 比较版本号数字
                val thisVersion = thisBase.split(".").map { it.toIntOrNull() ?: 0 }
                val otherVersion = otherBase.split(".").map { it.toIntOrNull() ?: 0 }

                for (i in 0 until minOf(thisVersion.size, otherVersion.size)) {
                    if (thisVersion[i] > otherVersion[i]) return true
                    if (thisVersion[i] < otherVersion[i]) return false
                }
            }

            // 版本号相同，比较 Hotfix
            if (isHotfix && !other.isHotfix) return true
            if (!isHotfix && other.isHotfix) return false

            // 都有或都没有 Hotfix，比较 Catalog
            return (catalog ?: "") > (other.catalog ?: "")
        }
    }

    /**
     * 稳定版本格式:
     * - 应用内: Stable-x.y-Catalog 或 Stable-x.yFix-Catalog
     * - GitHub标签: Stable-x.y 或 Stable-x.yFix
     */
    data class StableVersion(
        override val fullString: String,
        val prefix: String,              // Stable
        val versionCode: String,          // 1.0
        override val isHotfix: Boolean,            // 是否为 Hotfix 版本
        val hotfixSuffix: String? = null, // Fix 后缀（如果有）
        val catalog: String?              // Catalog (可能为 null)
    ) : ParsedVersion() {
        override val versionType: VersionType = VersionType.STABLE
        override val baseVersion: String = if (isHotfix) {
            "$prefix-$versionCode$hotfixSuffix"
        } else {
            "$prefix-$versionCode"
        }

        override fun isNewerThan(other: ParsedVersion): Boolean {
            if (other !is StableVersion) return true

            // 先比较基础版本号
            val thisBase = versionCode
            val otherBase = other.versionCode

            if (thisBase != otherBase) {
                // 比较版本号数字
                val thisVersion = thisBase.split(".").map { it.toIntOrNull() ?: 0 }
                val otherVersion = otherBase.split(".").map { it.toIntOrNull() ?: 0 }

                for (i in 0 until minOf(thisVersion.size, otherVersion.size)) {
                    if (thisVersion[i] > otherVersion[i]) return true
                    if (thisVersion[i] < otherVersion[i]) return false
                }
            }

            // 版本号相同，比较 Hotfix
            if (isHotfix && !other.isHotfix) return true
            if (!isHotfix && other.isHotfix) return false

            // 都有或都没有 Hotfix，比较 Catalog
            return (catalog ?: "") > (other.catalog ?: "")
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

        // 尝试匹配 Nightly 版本: nightly-yymmdd (大小写不敏感)
        val nightlyRegex = """^(?i)(nightly)-(\d{6})$""".toRegex()
        nightlyRegex.find(cleanVersion)?.let {
            val (prefix, dateCode) = it.destructured
            return ParsedVersion.NightlyVersion(
                fullString = versionString,
                prefix = prefix,
                dateCode = dateCode
            )
        }

        // 尝试匹配 Beta 版本（带 Catalog）- 应用内版本
        // 格式: Beta-x.y-Catalog 或 Beta-x.yFix-Catalog
        val betaWithCatalogRegex = """^(?i)(beta)-(\d+\.\d+)(Fix)?-([a-zA-Z0-9]+)$""".toRegex()
        betaWithCatalogRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, fixSuffix, catalog) = it.destructured
            val isHotfix = fixSuffix.equals("Fix", ignoreCase = true)
            return ParsedVersion.BetaVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                isHotfix = isHotfix,
                hotfixSuffix = if (isHotfix) fixSuffix else null,
                catalog = catalog
            )
        }

        // 尝试匹配 Beta 版本（不带 Catalog）- GitHub 标签
        // 格式: Beta-x.y 或 Beta-x.yFix
        val betaRegex = """^(?i)(beta)-(\d+\.\d+)(Fix)?$""".toRegex()
        betaRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, fixSuffix) = it.destructured
            val isHotfix = fixSuffix.equals("Fix", ignoreCase = true)
            return ParsedVersion.BetaVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                isHotfix = isHotfix,
                hotfixSuffix = if (isHotfix) fixSuffix else null,
                catalog = null
            )
        }

        // 尝试匹配 Stable 版本（带 Catalog）- 应用内版本
        // 格式: Stable-x.y-Catalog 或 Stable-x.yFix-Catalog
        val stableWithCatalogRegex = """^(?i)(stable)-(\d+\.\d+)(Fix)?-([a-zA-Z0-9]+)$""".toRegex()
        stableWithCatalogRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, fixSuffix, catalog) = it.destructured
            val isHotfix = fixSuffix.equals("Fix", ignoreCase = true)
            return ParsedVersion.StableVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                isHotfix = isHotfix,
                hotfixSuffix = if (isHotfix) fixSuffix else null,
                catalog = catalog
            )
        }

        // 尝试匹配 Stable 版本（不带 Catalog）- GitHub 标签
        // 格式: Stable-x.y 或 Stable-x.yFix
        val stableRegex = """^(?i)(stable)-(\d+\.\d+)(Fix)?$""".toRegex()
        stableRegex.find(cleanVersion)?.let {
            val (prefix, versionCode, fixSuffix) = it.destructured
            val isHotfix = fixSuffix.equals("Fix", ignoreCase = true)
            return ParsedVersion.StableVersion(
                fullString = versionString,
                prefix = prefix,
                versionCode = versionCode,
                isHotfix = isHotfix,
                hotfixSuffix = if (isHotfix) fixSuffix else null,
                catalog = null
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