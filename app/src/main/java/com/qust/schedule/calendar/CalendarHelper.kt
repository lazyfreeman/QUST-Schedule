package com.qust.schedule.calendar

import android.Manifest
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

    /**
     * 获取或创建专属的“青科大课表”日历本
     */
    fun getOrCreateCalendarId(context: Context, config: TermConfig): Long {
        val contentResolver = context.contentResolver

        // 1. 查询是否已存在该日历
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(config.accountName, config.accountType)

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
            put(CalendarContract.Calendars.ACCOUNT_NAME, config.accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, config.accountType)
            put(CalendarContract.Calendars.NAME, config.calendarName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, config.calendarName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor("#00838F"))
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, config.accountName)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        // 以 SyncAdapter 模式插入日历
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, config.accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, config.accountType)
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

        // 1. 清空当前日历本下的所有旧课程，保证全量更新不会产生冲突与脏数据
        try {
            contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val firstMonday = config.getFirstMondayLocalDate()
        val zoneId = ZoneId.systemDefault()
        var insertedCount = 0

        // 2. 遍历每门课程的每一个周次，生成具体公历日程
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

                val eventValues = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, course.name)
                    put(CalendarContract.Events.EVENT_LOCATION, course.location)
                    put(
                        CalendarContract.Events.DESCRIPTION,
                        "教师：${course.teacher}\n周次：第${week}周 (原${course.rawWeekStr})\n节次：第${course.startSection}-${course.endSection}节 ($startLocalTime-$endLocalTime)$summerNotice"
                    )
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

                val eventUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
                if (eventUri != null) {
                    insertedCount++
                    val eventId = ContentUris.parseId(eventUri)

                    // 添加提前提醒 (默认提前15分钟通知)
                    if (config.reminderMinutes > 0) {
                        val reminderValues = ContentValues().apply {
                            put(CalendarContract.Reminders.EVENT_ID, eventId)
                            put(CalendarContract.Reminders.MINUTES, config.reminderMinutes)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }
                        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                    }
                }
            }
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

        val count = context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString())
        )
        return SyncResult(true, count, "已清除系统日历中青科大课表的全部日程 ($count 条)")
    }
}
