package com.qust.schedule.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeSlotTest {

    @Test
    fun testDefaultScheduleTimesMatchWinterSchedule() {
        val defaults = DefaultTimeSlots.getDefault()
        assertEquals(11, defaults.size)

        // 上午
        assertEquals("08:00", defaults[0].startTime)
        assertEquals("08:45", defaults[0].endTime)
        assertEquals("08:55", defaults[1].startTime)
        assertEquals("09:40", defaults[1].endTime)
        assertEquals("10:00", defaults[2].startTime)
        assertEquals("10:45", defaults[2].endTime)
        assertEquals("10:55", defaults[3].startTime)
        assertEquals("11:40", defaults[3].endTime)

        // 下午 (冬季时间)
        assertEquals("13:30", defaults[4].startTime)
        assertEquals("14:15", defaults[4].endTime)
        assertEquals("14:25", defaults[5].startTime)
        assertEquals("15:10", defaults[5].endTime)
        assertEquals("15:30", defaults[6].startTime)
        assertEquals("16:15", defaults[6].endTime)
        assertEquals("16:25", defaults[7].startTime)
        assertEquals("17:10", defaults[7].endTime)

        // 晚上
        assertEquals("18:00", defaults[8].startTime)
        assertEquals("18:45", defaults[8].endTime)
        assertEquals("18:55", defaults[9].startTime)
        assertEquals("19:40", defaults[9].endTime)
        assertEquals("19:50", defaults[10].startTime)
        assertEquals("20:35", defaults[10].endTime)
    }

    @Test
    fun testIsSummerTimeRule() {
        // 规则：每年 5月1日之后实行夏时制，10月1日恢复 (5月1日~9月30日)
        assertFalse(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 4, 30)))
        assertTrue(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 5, 1)))
        assertTrue(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 6, 15)))
        assertTrue(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 9, 30)))
        assertFalse(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 10, 1)))
        assertFalse(DefaultTimeSlots.isSummerTime(LocalDate.of(2026, 11, 18)))
        assertFalse(DefaultTimeSlots.isSummerTime(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun testSummerTimeShiftAfternoonAndEveningSlots() {
        val defaults = DefaultTimeSlots.getDefault()

        // 上午第1节保持不变
        val slot1Summer = defaults[0].getEffectiveSlot(isSummer = true)
        assertEquals("08:00", slot1Summer.startTime)
        assertEquals("08:45", slot1Summer.endTime)

        // 下午第5节推迟30分钟 (13:30 -> 14:00, 14:15 -> 14:45)
        val slot5Summer = defaults[4].getEffectiveSlot(isSummer = true)
        assertEquals("14:00", slot5Summer.startTime)
        assertEquals("14:45", slot5Summer.endTime)

        // 下午第6节推迟30分钟 (14:25 -> 14:55, 15:10 -> 15:40)
        val slot6Summer = defaults[5].getEffectiveSlot(isSummer = true)
        assertEquals("14:55", slot6Summer.startTime)
        assertEquals("15:40", slot6Summer.endTime)

        // 下午第7节推迟30分钟 (15:30 -> 16:00, 16:15 -> 16:45)
        val slot7Summer = defaults[6].getEffectiveSlot(isSummer = true)
        assertEquals("16:00", slot7Summer.startTime)
        assertEquals("16:45", slot7Summer.endTime)

        // 下午第8节推迟30分钟 (16:25 -> 16:55, 17:10 -> 17:40)
        val slot8Summer = defaults[7].getEffectiveSlot(isSummer = true)
        assertEquals("16:55", slot8Summer.startTime)
        assertEquals("17:40", slot8Summer.endTime)

        // 晚上第9节在夏天也推迟30分钟 (18:00 -> 18:30, 18:45 -> 19:15)
        val slot9Summer = defaults[8].getEffectiveSlot(isSummer = true)
        assertEquals("18:30", slot9Summer.startTime)
        assertEquals("19:15", slot9Summer.endTime)

        // 晚上第10节在夏天也推迟30分钟 (18:55 -> 19:25, 19:40 -> 20:10)
        val slot10Summer = defaults[9].getEffectiveSlot(isSummer = true)
        assertEquals("19:25", slot10Summer.startTime)
        assertEquals("20:10", slot10Summer.endTime)

        // 晚上第11节在夏天也推迟30分钟 (19:50 -> 20:20, 20:35 -> 21:05)
        val slot11Summer = defaults[10].getEffectiveSlot(isSummer = true)
        assertEquals("20:20", slot11Summer.startTime)
        assertEquals("21:05", slot11Summer.endTime)
    }

    @Test
    fun testGetStartEndTimeWinterAndSummer() {
        val defaults = DefaultTimeSlots.getDefault()

        // 冬季：第5-6节 (13:30 - 15:10)
        val (wStart, wEnd) = DefaultTimeSlots.getStartEndTime(defaults, 5, 6, isSummer = false)
        assertEquals(LocalTime.of(13, 30), wStart)
        assertEquals(LocalTime.of(15, 10), wEnd)

        // 夏季：第5-6节 (14:00 - 15:40)
        val (sStart, sEnd) = DefaultTimeSlots.getStartEndTime(defaults, 5, 6, isSummer = true)
        assertEquals(LocalTime.of(14, 0), sStart)
        assertEquals(LocalTime.of(15, 40), sEnd)

        // 冬季：第7-8节 (15:30 - 17:10)
        val (w78Start, w78End) = DefaultTimeSlots.getStartEndTime(defaults, 7, 8, isSummer = false)
        assertEquals(LocalTime.of(15, 30), w78Start)
        assertEquals(LocalTime.of(17, 10), w78End)

        // 夏季：第7-8节 (16:00 - 17:40)
        val (s78Start, s78End) = DefaultTimeSlots.getStartEndTime(defaults, 7, 8, isSummer = true)
        assertEquals(LocalTime.of(16, 0), s78Start)
        assertEquals(LocalTime.of(17, 40), s78End)

        // 冬季：第9-11节 (18:00 - 20:35)
        val (w911Start, w911End) = DefaultTimeSlots.getStartEndTime(defaults, 9, 11, isSummer = false)
        assertEquals(LocalTime.of(18, 0), w911Start)
        assertEquals(LocalTime.of(20, 35), w911End)

        // 夏季：第9-11节 (18:30 - 21:05)
        val (s911Start, s911End) = DefaultTimeSlots.getStartEndTime(defaults, 9, 11, isSummer = true)
        assertEquals(LocalTime.of(18, 30), s911Start)
        assertEquals(LocalTime.of(21, 5), s911End)
    }

    @Test
    fun testCustomTimeSlots() {
        // 用户自定义作息时间
        val customSlots = listOf(
            TimeSlot(1, "08:10", "08:55"),
            TimeSlot(2, "09:05", "09:50"),
            TimeSlot(5, "13:00", "13:45"),
            TimeSlot(9, "18:10", "18:55")
        )

        // 冬季
        val (start, end) = DefaultTimeSlots.getStartEndTime(customSlots, 1, 2, isSummer = false)
        assertEquals(LocalTime.of(8, 10), start)
        assertEquals(LocalTime.of(9, 50), end)

        // 夏季下午推迟30分钟 (13:00 -> 13:30, 13:45 -> 14:15)
        val (sStart, sEnd) = DefaultTimeSlots.getStartEndTime(customSlots, 5, 5, isSummer = true)
        assertEquals(LocalTime.of(13, 30), sStart)
        assertEquals(LocalTime.of(14, 15), sEnd)

        // 夏季晚上推迟30分钟 (18:10 -> 18:40, 18:55 -> 19:25)
        val (eStart, eEnd) = DefaultTimeSlots.getStartEndTime(customSlots, 9, 9, isSummer = true)
        assertEquals(LocalTime.of(18, 40), eStart)
        assertEquals(LocalTime.of(19, 25), eEnd)
    }
}
