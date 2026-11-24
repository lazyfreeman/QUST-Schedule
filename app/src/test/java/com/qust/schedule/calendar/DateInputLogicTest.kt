package com.qust.schedule.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class DateInputLogicTest {

    private fun parseDateParts(year: String, month: String, day: String): LocalDate? {
        val y = year.toIntOrNull()
        val m = month.toIntOrNull()
        val d = day.toIntOrNull()

        if (y == null || m == null || d == null) return null
        if (y !in 1970..2100 || m !in 1..12 || d !in 1..31) return null

        return try {
            LocalDate.of(y, m, d)
        } catch (_: Exception) {
            null
        }
    }

    private fun alignToFirstMonday(date: LocalDate): LocalDate {
        return if (date.dayOfWeek == DayOfWeek.MONDAY) {
            date
        } else {
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }

    @Test
    fun testValidDateParsing() {
        val date = parseDateParts("2026", "03", "02")
        assertNotNull(date)
        assertEquals(2026, date!!.year)
        assertEquals(3, date.monthValue)
        assertEquals(2, date.dayOfMonth)
        assertEquals(DayOfWeek.MONDAY, date.dayOfWeek)
        assertEquals("2026-03-02", date.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    @Test
    fun testInvalidDateParsing() {
        // 2月30日不存在
        assertNull(parseDateParts("2026", "02", "30"))
        // 平年2月29日不存在
        assertNull(parseDateParts("2025", "02", "29"))
        // 闰年2月29日存在
        assertNotNull(parseDateParts("2024", "02", "29"))
        // 月份超出范围
        assertNull(parseDateParts("2026", "13", "01"))
        // 日期超出范围
        assertNull(parseDateParts("2026", "05", "32"))
        // 含有非数字
        assertNull(parseDateParts("2026", "", "01"))
    }

    @Test
    fun testAlignToFirstMonday() {
        // 2026-03-02 是周一
        val monday = LocalDate.of(2026, 3, 2)
        assertEquals(monday, alignToFirstMonday(monday))

        // 2026-03-03 是周二，应当推算出所在周周一 2026-03-02
        val tuesday = LocalDate.of(2026, 3, 3)
        assertEquals(DayOfWeek.TUESDAY, tuesday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 3, 2), alignToFirstMonday(tuesday))

        // 2026-03-08 是周日，仍属于该周（青科大及中国周历通常以周一为一周起始）
        val sunday = LocalDate.of(2026, 3, 8)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 3, 2), alignToFirstMonday(sunday))
    }
}
