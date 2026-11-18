package com.qust.schedule.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 单小节课程起止时间模型
 */
data class TimeSlot(
    val section: Int,
    val startTime: String, // "08:00"
    val endTime: String    // "08:45"
) {
    fun getStartLocalTime(): LocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
    fun getEndLocalTime(): LocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))

    /**
     * 判断是否属于下午时段
     * 规则：第5~8节，或者开始时间在12:00~17:59之间
     */
    fun isAfternoonSection(): Boolean {
        return section in 5..8 || (getStartLocalTime() >= LocalTime.of(12, 0) && getStartLocalTime() < LocalTime.of(18, 0))
    }

    /**
     * 判断是否在夏天推迟半小时（下午及晚上的节次）
     * 规则：第5节及以后，或者开始时间在12:00及之后
     */
    fun isShiftedInSummer(): Boolean {
        return section >= 5 || getStartLocalTime() >= LocalTime.of(12, 0)
    }

    /**
     * 判断属于上午、下午还是晚上
     */
    fun getPeriodLabel(): String {
        return when {
            section in 1..4 || getStartLocalTime() < LocalTime.of(12, 0) -> "上午"
            section in 5..8 || (getStartLocalTime() >= LocalTime.of(12, 0) && getStartLocalTime() < LocalTime.of(18, 0)) -> "下午"
            else -> "晚上"
        }
    }

    /**
     * 根据是否处于夏令时（5月1日~9月30日）获取实际生效的时间段
     * 下午及晚上各节次推迟 30 分钟
     */
    fun getEffectiveSlot(isSummer: Boolean): TimeSlot {
        if (!isSummer) return this
        if (!isShiftedInSummer()) return this

        val newStart = getStartLocalTime().plusMinutes(30)
        val newEnd = getEndLocalTime().plusMinutes(30)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return TimeSlot(
            section = section,
            startTime = newStart.format(formatter),
            endTime = newEnd.format(formatter)
        )
    }
}

object DefaultTimeSlots {
    /**
     * 青岛科技大学默认冬季作息时间表（共11节）
     */
    fun getDefault(): List<TimeSlot> = listOf(
        TimeSlot(1, "08:00", "08:45"),
        TimeSlot(2, "08:55", "09:40"),
        TimeSlot(3, "10:00", "10:45"),
        TimeSlot(4, "10:55", "11:40"),
        TimeSlot(5, "13:30", "14:15"),
        TimeSlot(6, "14:25", "15:10"),
        TimeSlot(7, "15:30", "16:15"),
        TimeSlot(8, "16:25", "17:10"),
        TimeSlot(9, "18:00", "18:45"),
        TimeSlot(10, "18:55", "19:40"),
        TimeSlot(11, "19:50", "20:35")
    )

    /**
     * 判断指定日期是否处于夏时制（夏令时）期间
     * 规则：每年 5 月 1 日至 9 月 30 日（在5月1日之后将下午及晚上日程推迟半小时，在10月1日恢复）
     */
    fun isSummerTime(date: LocalDate): Boolean {
        return date.monthValue in 5..9
    }

    /**
     * 根据起始节次和结束节次获取对应的起止时间
     */
    fun getStartEndTime(
        slots: List<TimeSlot>,
        startSection: Int,
        endSection: Int,
        isSummer: Boolean = false
    ): Pair<LocalTime, LocalTime> {
        val effectiveSlots = if (isSummer) slots.map { it.getEffectiveSlot(true) } else slots
        val startSlot = effectiveSlots.find { it.section == startSection }
            ?: effectiveSlots.minByOrNull { it.section }
            ?: TimeSlot(startSection, "08:00", "08:45")
        val endSlot = effectiveSlots.find { it.section == endSection }
            ?: effectiveSlots.maxByOrNull { it.section }
            ?: TimeSlot(endSection, "19:50", "20:35")
        return Pair(startSlot.getStartLocalTime(), endSlot.getEndLocalTime())
    }
}
