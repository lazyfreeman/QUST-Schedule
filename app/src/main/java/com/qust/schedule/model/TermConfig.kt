package com.qust.schedule.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 学期与日历同步配置项
 */
data class TermConfig(
    val firstMonday: String = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ISO_LOCAL_DATE),
    val reminderMinutes: Int = 15,
    val calendarName: String = "青科大课表",
    val accountName: String = "qust_schedule_account",
    val accountType: String = "com.qust.schedule",
    val enableSummerTime: Boolean = true // 每年5月1日至9月30日下午及晚上作息自动推迟30分钟，10月1日恢复
) {
    fun getFirstMondayLocalDate(): LocalDate {
        return try {
            LocalDate.parse(firstMonday, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        }
    }
}
