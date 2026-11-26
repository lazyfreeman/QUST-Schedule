package com.qust.schedule.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.ui.theme.QustPrimary
import com.qust.schedule.ui.theme.QustScheduleTheme
import com.qust.schedule.ui.theme.SuccessGreen
import com.qust.schedule.util.ApkDownloadManager
import com.qust.schedule.util.UpdateCheckResult
import com.qust.schedule.util.UpdateChecker
import com.qust.schedule.util.UpdateSource

/**
 * 检查更新对话框：
 * - 支持「GitHub 直连」与「镜像加速」两条检查线路，可随时切换
 * - 发现新版本时展示更新日志，提供 GitHub / 镜像两种应用内直接下载方式，
 *   下载完成后一键调起系统安装器；也可兜底跳转浏览器下载
 */
@Composable
fun UpdateDialog(
    currentVersion: String,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onInstall: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    var source by remember { mutableStateOf(UpdateSource.GITHUB) }
    var retryTick by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }

    LaunchedEffect(source, retryTick) {
        result = null
        result = UpdateChecker.check(context, source)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdateAlt,
                contentDescription = null,
                tint = QustPrimary
            )
        },
        title = {
            Text(
                text = "检查更新",
                fontWeight = FontWeight.Bold,
                color = QustPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 检查线路切换
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = source == UpdateSource.GITHUB,
                        onClick = { source = UpdateSource.GITHUB },
                        label = { Text("GitHub 直连", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = source == UpdateSource.MIRROR,
                        onClick = { source = UpdateSource.MIRROR },
                        label = { Text("镜像加速", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "当前版本：v$currentVersion",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                when (val state = result) {
                    null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = QustPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "正在通过${source.label}检查更新…",
                                fontSize = 13.sp
                            )
                        }
                    }

                    is UpdateCheckResult.UpToDate -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已是最新版本 ${state.latestVersion}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is UpdateCheckResult.Error -> {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { retryTick++ }) {
                            Text("重试", color = QustPrimary)
                        }
                    }

                    is UpdateCheckResult.UpdateAvailable -> {
                        val info = state.info
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = QustPrimary.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "发现新版本 ${info.tagName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = QustPrimary
                                )
                                if (info.apkSizeBytes > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "安装包大小：%.1f MB".format(info.apkSizeBytes / 1024f / 1024f),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (info.releaseNotes.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = info.releaseNotes,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "镜像通道暂无法获取更新日志，可点击右上角「GitHub 下载」查看发布页详情。",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "选择下载方式（应用内直接下载，完成后可一键安装）：",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        UpdateDownloadButtons(
                            apkDownloadUrl = info.apkDownloadUrl,
                            mirrorPrefix = info.mirrorPrefix,
                            onOpenUrl = onOpenUrl,
                            onInstall = onInstall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * GitHub / 镜像双通道应用内下载：
 * 空闲时展示两个下载按钮；下载中展示进度条与取消；完成后展示安装按钮。
 * 另提供「改用浏览器下载」兜底入口。
 */
@Composable
internal fun UpdateDownloadButtons(
    apkDownloadUrl: String,
    mirrorPrefix: String?,
    onOpenUrl: (String) -> Unit,
    onInstall: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    val downloadState by ApkDownloadManager.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        when (val state = downloadState) {
            is ApkDownloadManager.State.Downloading -> {
                if (state.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        color = QustPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        color = QustPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.totalBytes > 0) {
                            "下载中 %d%%（%.1f / %.1f MB）".format(
                                state.percent,
                                state.downloadedBytes / 1024f / 1024f,
                                state.totalBytes / 1024f / 1024f
                            )
                        } else {
                            "下载中 %.1f MB".format(state.downloadedBytes / 1024f / 1024f)
                        },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { ApkDownloadManager.cancel() }) {
                        Text("取消", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            is ApkDownloadManager.State.Ready -> {
                Button(
                    onClick = { onInstall(state.file) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("下载完成，点击安装新版本", fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { ApkDownloadManager.reset() }) {
                    Text("重新下载", fontSize = 12.sp, color = Color.Gray)
                }
            }

            else -> {
                if (state is ApkDownloadManager.State.Failed) {
                    Text(
                        text = "下载失败：${state.message}，可重试或改用浏览器下载",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { ApkDownloadManager.start(context, apkDownloadUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = QustPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GitHub 下载", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            ApkDownloadManager.start(
                                context,
                                UpdateChecker.mirrorUrl(apkDownloadUrl, mirrorPrefix)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = QustPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("镜像加速下载", fontSize = 12.sp, color = QustPrimary, maxLines = 1, softWrap = false)
                    }
                }
                TextButton(
                    onClick = { onOpenUrl(apkDownloadUrl) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("改用浏览器下载", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Preview(name = "下载按钮-窄屏", widthDp = 266)
@Composable
private fun UpdateDownloadButtonsPreview() {
    QustScheduleTheme {
        UpdateDownloadButtons(
            apkDownloadUrl = "https://github.com/lazyfreeman/QUST-Schedule/releases/download/v1.2.0/QUST-Schedule-v1.2.0.apk",
            mirrorPrefix = null,
            onOpenUrl = {},
            onInstall = {}
        )
    }
}
