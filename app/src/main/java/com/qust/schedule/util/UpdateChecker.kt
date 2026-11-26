package com.qust.schedule.util

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 检查更新的网络通道 */
enum class UpdateSource(val label: String) {
    GITHUB("GitHub 直连"),
    MIRROR("镜像加速")
}

/** 一次成功检查得到的最新 Release 信息 */
data class UpdateInfo(
    val tagName: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    /** GitHub 官方 APK 直链（镜像下载时在其前面拼接镜像前缀） */
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    /** 若通过镜像检查成功，记录可用的镜像前缀 */
    val mirrorPrefix: String?
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo, val currentVersion: String) : UpdateCheckResult()
    data class UpToDate(val latestVersion: String, val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {

    const val REPO = "lazyfreeman/QUST-Schedule"

    /** 常用 GitHub 加速镜像，按顺序探测，首个可用的即被采用 */
    val MIRROR_PREFIXES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.net/"
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** 镜像检查时不自动跟随重定向，直接从 302 的 Location 中提取最新 tag */
    private val noRedirectClient: OkHttpClient by lazy {
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    suspend fun check(context: Context, source: UpdateSource): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val currentVersion = getCurrentVersion(context)
            try {
                val info = when (source) {
                    UpdateSource.GITHUB -> fetchFromGitHub()
                    UpdateSource.MIRROR -> fetchFromMirror()
                }
                if (compareVersions(info.tagName, currentVersion) > 0) {
                    UpdateCheckResult.UpdateAvailable(info, currentVersion)
                } else {
                    UpdateCheckResult.UpToDate(info.tagName, currentVersion)
                }
            } catch (e: Exception) {
                UpdateCheckResult.Error(
                    when (source) {
                        UpdateSource.GITHUB -> "GitHub 直连检查失败：${e.message ?: "网络异常"}\n可切换到「镜像加速」后重试。"
                        UpdateSource.MIRROR -> "镜像检查失败：${e.message ?: "网络异常"}\n可切换到「GitHub 直连」后重试。"
                    }
                )
            }
        }

    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    /** 拼接镜像下载直链 */
    fun mirrorUrl(githubUrl: String, mirrorPrefix: String? = null): String {
        return (mirrorPrefix ?: MIRROR_PREFIXES.first()) + githubUrl
    }

    // ---------------- GitHub 直连：官方 API ----------------

    private fun fetchFromGitHub(): UpdateInfo {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val json = JsonParser.parseString(response.body?.string() ?: "").asJsonObject
            val tag = json.get("tag_name")?.asString
                ?: throw IOException("未找到任何 Release 版本")

            val apkAsset = json.getAsJsonArray("assets")
                ?.firstOrNull { it.asJsonObject.get("name")?.asString?.endsWith(".apk") == true }
                ?.asJsonObject

            val apkUrl = apkAsset?.get("browser_download_url")?.asString ?: buildApkUrl(tag)
            return UpdateInfo(
                tagName = tag,
                releaseNotes = json.get("body")?.takeIf { !it.isJsonNull }?.asString ?: "",
                releasePageUrl = json.get("html_url")?.takeIf { !it.isJsonNull }?.asString
                    ?: "https://github.com/$REPO/releases/tag/$tag",
                apkDownloadUrl = apkUrl,
                apkSizeBytes = apkAsset?.get("size")?.takeIf { !it.isJsonNull }?.asLong ?: 0L,
                mirrorPrefix = null
            )
        }
    }

    // ---------------- 镜像加速：代理 releases/latest 重定向 ----------------

    private fun fetchFromMirror(): UpdateInfo {
        var lastError: Exception? = null
        for (prefix in MIRROR_PREFIXES) {
            try {
                val request = Request.Builder()
                    .url("${prefix}https://github.com/$REPO/releases/latest")
                    .build()

                noRedirectClient.newCall(request).execute().use { response ->
                    // GitHub 对 /releases/latest 返回 302，Location 中带最新 tag
                    val tag = when {
                        response.isRedirect ->
                            extractTagFromUrl(response.header("Location") ?: "")
                        response.isSuccessful ->
                            // 部分镜像会代跟随重定向直接返回最终页面 HTML
                            extractTagFromUrl(response.request.url.toString())
                                ?: extractTagFromUrl(response.body?.string() ?: "")
                        else -> throw IOException("HTTP ${response.code}")
                    } ?: throw IOException("未能从镜像响应中解析版本号")

                    return UpdateInfo(
                        tagName = tag,
                        releaseNotes = "",
                        releasePageUrl = "https://github.com/$REPO/releases/tag/$tag",
                        apkDownloadUrl = buildApkUrl(tag),
                        apkSizeBytes = 0L,
                        mirrorPrefix = prefix
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("所有镜像均不可用")
    }

    /** 与 CI 产物命名规则一致：QUST-Schedule-{tag}.apk */
    private fun buildApkUrl(tag: String): String =
        "https://github.com/$REPO/releases/download/$tag/QUST-Schedule-$tag.apk"

    // ---------------- 纯函数工具（可单元测试） ----------------

    /** 从 URL / Location / HTML 片段中提取 releases/tag/ 后的版本号 */
    fun extractTagFromUrl(text: String): String? {
        val regex = Regex("/releases/tag/([^/?#\\s\"']+)")
        return regex.find(text)?.groupValues?.get(1)
    }

    /**
     * 语义化版本比较：忽略前缀 v/V，按点分段逐段数值比较，
     * 非数字后缀（如 1.2.0-beta）按 0 处理。缺失段视为 0。
     * 返回正数表示 v1 更新，负数表示 v2 更新，0 表示相同。
     */
    fun compareVersions(v1: String, v2: String): Int {
        fun parts(v: String): List<Int> =
            v.trim().removePrefix("v").removePrefix("V")
                .split('.', '-')
                .map { seg -> seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

        val a = parts(v1)
        val b = parts(v2)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = (a.getOrElse(i) { 0 }) - (b.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
