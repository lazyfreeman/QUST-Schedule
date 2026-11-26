package com.qust.schedule.util

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 应用内 APK 下载管理器。
 * 下载到应用私有缓存目录（cacheDir/updates），无需任何存储权限；
 * 使用进程级作用域，关闭对话框不会中断下载，重新打开可继续查看进度。
 */
object ApkDownloadManager {

    sealed class State {
        object Idle : State()
        data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : State()
        data class Ready(val file: File) : State()
        data class Failed(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    // 下载大文件不设 callTimeout，避免慢速网络下整体超时
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun start(context: Context, url: String) {
        if (job?.isActive == true) return
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, url.substringAfterLast('/').ifBlank { "update.apk" })

        job = scope.launch {
            try {
                _state.value = State.Downloading(0, 0L, 0L)
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("响应内容为空")
                    val total = body.contentLength()
                    var downloaded = 0L
                    var lastPercent = -1

                    body.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val percent = if (total > 0) (downloaded * 100 / total).toInt() else 0
                                if (percent != lastPercent || total <= 0) {
                                    lastPercent = percent
                                    _state.value = State.Downloading(percent, downloaded, total)
                                }
                            }
                        }
                    }
                }
                _state.value = State.Ready(file)
            } catch (e: CancellationException) {
                file.delete()
                _state.value = State.Idle
                throw e
            } catch (e: Exception) {
                file.delete()
                _state.value = State.Failed(e.message ?: "下载失败")
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    /** 重置回初始状态（用于「重新下载」），不影响进行中的任务 */
    fun reset() {
        if (job?.isActive != true) {
            _state.value = State.Idle
        }
    }
}
