package com.qust.schedule.calendar

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.qust.schedule.model.Course
import com.qust.schedule.model.DefaultTimeSlots
import com.qust.schedule.model.TermConfig
import com.qust.schedule.model.TimeSlot
import java.time.ZoneId
import java.util.TimeZone

data class SyncResult(
    val success: Boolean,
    val eventCount: Int = 0,
    val message: String
)

object CalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private fun getSafeAccountName(config: TermConfig): String {
        return if (config.accountName.isBlank()) "qust_schedule_account" else config.accountName
    }

    /**
     * 获取或创建专属的“青科大课表”本地日历本。
     *
     * 注意：ACCOUNT_TYPE 必须严格使用 [CalendarContract.ACCOUNT_TYPE_LOCAL] ("LOCAL")。
     * 若使用自定义类型（如 "com.qust.schedule"），由于该类型未在系统 AccountManager 中注册为 Authenticator，
     * Android CalendarProvider 会在系统定期同步、重启或账户扫描（removeStaleAccounts）时，
     * 将其判定为已失效/孤儿账户，从而自动级联删除该日历及名下的所有日程事件。
     */
    fun getOrCreateCalendarId(context: Context, config: TermConfig): Long {
        val contentResolver = context.contentResolver
        val accountName = getSafeAccountName(config)
        val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

        // 0. 清理旧版本可能残留的非 LOCAL 日历（例如旧版本使用过 "com.qust.schedule"），
        // 避免系统日历中出现残留的同名日历本或随时会被系统清理的失效日历
        try {
            val oldSelection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? OR (${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} != ?)"
            val oldArgs = arrayOf("com.qust.schedule", accountName, accountType)
            val deleteOldUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build()
            contentResolver.delete(deleteOldUri, oldSelection, oldArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. 查询是否已存在专属本地日历
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(accountName, accountType)

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIndex != -1) {
                    return cursor.getLong(idIndex)
                }
            }
        }

        // 2. 若不存在，创建专属本地日历
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            put(CalendarContract.Calendars.NAME, config.calendarName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, config.calendarName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor("#1565C0"))
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        // 以 SyncAdapter 模式插入日历
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build()

        val createdUri = contentResolver.insert(uri, values) ?: return -1L
        return ContentUris.parseId(createdUri)
    }

    /**
     * 将解析出的所有课程全量同步至系统日历
     */
    fun syncCoursesToCalendar(
        context: Context,
        courses: List<Course>,
        config: TermConfig,
        timeSlots: List<TimeSlot>
    ): SyncResult {
        if (!hasCalendarPermission(context)) {
            return SyncResult(false, 0, "缺少日历读写权限，请先授予权限")
        }

        if (courses.isEmpty()) {
            return SyncResult(false, 0, "课程列表为空，无需同步")
        }

        val calendarId = getOrCreateCalendarId(context, config)
        if (calendarId <= 0) {
            return SyncResult(false, 0, "创建或获取专属日历失败")
        }

        val contentResolver = context.contentResolver
        val accountName = getSafeAccountName(config)
        val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

        // 1. 以 SyncAdapter 身份清空当前日历本下的所有旧课程，保证物理彻底删除，不留墓碑数据
        try {
            val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build()
            contentResolver.delete(
                deleteUri,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val firstMonday = config.getFirstMondayLocalDate()
        val zoneId = ZoneId.systemDefault()
        val timeZoneId = TimeZone.getDefault().id
        var insertedCount = 0

        val eventsUri = CalendarContract.Events.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build()

        val remindersUri = CalendarContract.Reminders.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build()

        // 2. 遍历每门课程的每一个周次，生成具体公历日程并分批写入
        val operations = ArrayList<ContentProviderOperation>()
        val batchLimit = 100 // 每批提交的日程数，防止超出 Binder 事务大小限制

        for (course in courses) {
            for (week in course.weeks) {
                // 计算当前周对应星期几的公历日期
                val courseDate = firstMonday
                    .plusWeeks((week - 1).toLong())
                    .plusDays((course.dayOfWeek - 1).toLong())

                val isSummer = config.enableSummerTime && DefaultTimeSlots.isSummerTime(courseDate)
                val (startLocalTime, endLocalTime) = DefaultTimeSlots.getStartEndTime(
                    timeSlots,
                    course.startSection,
                    course.endSection,
                    isSummer = isSummer
                )

                val startZonedDateTime = courseDate.atTime(startLocalTime).atZone(zoneId)
                val endZonedDateTime = courseDate.atTime(endLocalTime).atZone(zoneId)

                val startMillis = startZonedDateTime.toInstant().toEpochMilli()
                val endMillis = endZonedDateTime.toInstant().toEpochMilli()

                val summerNotice = if (isSummer && (course.startSection >= 5 || course.endSection >= 5)) {
                    "\n【已应用夏时制：下午及晚上日程推迟30分钟】"
                } else ""

                val description = "教师：${course.teacher}\n周次：第${week}周 (原${course.rawWeekStr})\n节次：第${course.startSection}-${course.endSection}节 ($startLocalTime-$endLocalTime)$summerNotice"

                val eventOpIndex = operations.size
                operations.add(
                    ContentProviderOperation.newInsert(eventsUri)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .withValue(CalendarContract.Events.TITLE, course.name)
                        .withValue(CalendarContract.Events.EVENT_LOCATION, course.location)
                        .withValue(CalendarContract.Events.DESCRIPTION, description)
                        .withValue(CalendarContract.Events.DTSTART, startMillis)
                        .withValue(CalendarContract.Events.DTEND, endMillis)
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, timeZoneId)
                        .withValue(CalendarContract.Events.EVENT_END_TIMEZONE, timeZoneId)
                        .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                        .withValue(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                        .withValue(CalendarContract.Events.HAS_ALARM, if (config.reminderMinutes > 0) 1 else 0)
                        .build()
                )

                // 添加提前提醒
                if (config.reminderMinutes > 0) {
                    operations.add(
                        ContentProviderOperation.newInsert(remindersUri)
                            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventOpIndex)
                            .withValue(CalendarContract.Reminders.MINUTES, config.reminderMinutes)
                            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            .build()
                    )
                }

                insertedCount++

                // 分批提交，保证内存友好与极高执行性能
                if (operations.size >= batchLimit * 2) {
                    try {
                        contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    operations.clear()
                }
            }
        }

        // 提交剩余批次
        if (operations.isNotEmpty()) {
            try {
                contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            operations.clear()
        }

        return SyncResult(true, insertedCount, "成功同步 $insertedCount 节课程到系统日历！")
    }

    /**
     * 清空专属日历下的所有日程
     */
    fun clearCalendar(context: Context, config: TermConfig): SyncResult {
        if (!hasCalendarPermission(context)) {
            return SyncResult(false, 0, "缺少日历权限")
        }

        val calendarId = getOrCreateCalendarId(context, config)
        if (calendarId <= 0) {
            return SyncResult(true, 0, "日历为空或已被清除")
        }

        val accountName = getSafeAccountName(config)
        val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

        val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build()

        val count = context.contentResolver.delete(
            deleteUri,
            "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString())
        )
        return SyncResult(true, count, "已清除系统日历中青科大课表的全部日程 ($count 条)")
    }
}
