package com.qust.schedule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.qust.schedule.calendar.CalendarHelper
import com.qust.schedule.model.Course
import com.qust.schedule.model.TermConfig
import com.qust.schedule.model.TimeSlot
import com.qust.schedule.parser.ZhengFangParser
import com.qust.schedule.ui.CustomTimeSlotsDialog
import com.qust.schedule.ui.LoginWebViewActivity
import com.qust.schedule.ui.MainScreen
import com.qust.schedule.ui.SettingsDialog
import com.qust.schedule.ui.theme.QustScheduleTheme
import com.qust.schedule.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var prefManager: PreferenceManager

    private var courses by mutableStateOf<List<Course>>(emptyList())
    private var termConfig by mutableStateOf(TermConfig())
    private var timeSlots by mutableStateOf<List<TimeSlot>>(emptyList())
    private var lastSyncTime by mutableStateOf<String?>(null)
    private var showSettingsDialog by mutableStateOf(false)
    private var showCustomTimeSlotsDialog by mutableStateOf(false)

    // 登录与课表抓取回调
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val rawJson = result.data?.getStringExtra(LoginWebViewActivity.EXTRA_RAW_JSON)
            if (!rawJson.isNullOrBlank()) {
                val parsed = ZhengFangParser.parseJson(rawJson)
                if (parsed.isNotEmpty()) {
                    courses = parsed
                    prefManager.saveCourses(parsed)
                    Toast.makeText(this, "成功获取 ${parsed.size} 门课程！点击【同步到日历】即可写入日程", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "解析课表数据为空，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 日历权限申请回调
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
        if (readGranted && writeGranted) {
            performSyncToCalendar()
        } else {
            Toast.makeText(this, "需要日历权限才能将课程写入系统日历", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefManager = PreferenceManager(this)
        courses = prefManager.getCourses()
        termConfig = prefManager.getTermConfig()
        timeSlots = prefManager.getTimeSlots()
        lastSyncTime = prefManager.getLastSyncTime()

        setContent {
            QustScheduleTheme {
                MainScreen(
                    courses = courses,
                    termConfig = termConfig,
                    timeSlots = timeSlots,
                    lastSyncTime = lastSyncTime,
                    onLaunchLogin = {
                        val intent = Intent(this, LoginWebViewActivity::class.java)
                        loginLauncher.launch(intent)
                    },
                    onSyncCalendar = {
                        if (CalendarHelper.hasCalendarPermission(this)) {
                            performSyncToCalendar()
                        } else {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    },
                    onClearCalendar = {
                        if (CalendarHelper.hasCalendarPermission(this)) {
                            val res = CalendarHelper.clearCalendar(this, termConfig)
                            lastSyncTime = null
                            prefManager.saveLastSyncTime("")
                            Toast.makeText(this, res.message, Toast.LENGTH_SHORT).show()
                        } else {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    },
                    onOpenCustomTimeSlots = {
                        showCustomTimeSlotsDialog = true
                    }
                )

                if (showSettingsDialog) {
                    SettingsDialog(
                        initialConfig = termConfig,
                        timeSlotsCount = timeSlots.size,
                        initialUsername = prefManager.getSavedUsername(),
                        initialPassword = prefManager.getSavedPassword(),
                        initialAutoLogin = prefManager.isAutoLoginEnabled(),
                        onDismiss = { showSettingsDialog = false },
                        onSave = { updatedConfig, newUsername, newPassword, autoLogin ->
                            termConfig = updatedConfig
                            prefManager.saveTermConfig(updatedConfig)
                            if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                                prefManager.saveUserCredentials(newUsername, newPassword, autoLogin)
                            } else {
                                prefManager.clearUserCredentials()
                            }
                            showSettingsDialog = false
                            Toast.makeText(this, "设置已更新，建议重新点击【同步到日历】生效", Toast.LENGTH_SHORT).show()
                        },
                        onOpenCustomTimeSlots = {
                            showSettingsDialog = false
                            showCustomTimeSlotsDialog = true
                        }
                    )
                }

                if (showCustomTimeSlotsDialog) {
                    CustomTimeSlotsDialog(
                        initialSlots = timeSlots,
                        onDismiss = { showCustomTimeSlotsDialog = false },
                        onSave = { updatedSlots ->
                            timeSlots = updatedSlots
                            prefManager.saveTimeSlots(updatedSlots, isCustomized = true)
                            showCustomTimeSlotsDialog = false
                            Toast.makeText(this, "作息时间已更新，建议重新点击【同步到日历】生效", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun performSyncToCalendar() {
        if (courses.isEmpty()) {
            Toast.makeText(this, "暂无课程数据，请先登录获取课表", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val result = CalendarHelper.syncCoursesToCalendar(
                context = this@MainActivity,
                courses = courses,
                config = termConfig,
                timeSlots = timeSlots
            )

            withContext(Dispatchers.Main) {
                if (result.success) {
                    val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    lastSyncTime = nowStr
                    prefManager.saveLastSyncTime(nowStr)
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "同步失败: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
