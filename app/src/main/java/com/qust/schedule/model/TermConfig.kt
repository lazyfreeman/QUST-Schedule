package com.qust.schedule.model

import android.provider.CalendarContract
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
    val accountType: String = CalendarContract.ACCOUNT_TYPE_LOCAL,
    val enableSummerTime: Boolean = true // 每年5月1日至9月30日下午及晚上作息自动推迟30分钟，10月1日恢复
) {
    /**
     * 获取有效的账户类型。
     * 本地日历必须使用 CalendarContract.ACCOUNT_TYPE_LOCAL ("LOCAL")，
     * 否则 Android CalendarProvider 会在账户同步检查或后台维护时，因系统 AccountManager 中不存在该账户而将该日历及其所有日程判定为过期并自动清除。
     */
    val effectiveAccountType: String
        get() = CalendarContract.ACCOUNT_TYPE_LOCAL
    fun getFirstMondayLocalDate(): LocalDate {
        return try {
            LocalDate.parse(firstMonday, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        }
    }
}
