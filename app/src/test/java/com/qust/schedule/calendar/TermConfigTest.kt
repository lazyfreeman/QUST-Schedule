package com.qust.schedule.calendar

import android.provider.CalendarContract
import com.google.gson.Gson
import com.qust.schedule.model.TermConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class TermConfigTest {

    @Test
    fun testDefaultAccountTypeIsLocal() {
        val config = TermConfig()
        assertEquals(CalendarContract.ACCOUNT_TYPE_LOCAL, config.accountType)
        assertEquals(CalendarContract.ACCOUNT_TYPE_LOCAL, config.effectiveAccountType)
        assertEquals("LOCAL", config.accountType)
    }

    @Test
    fun testLegacyAccountTypeMigration() {
        // 模拟从 SharedPreferences 中读取的旧版 JSON 配置（使用了自定义包名类型的账户）
        val legacyJson = """
            {
                "firstMonday": "2026-03-02",
                "reminderMinutes": 15,
                "calendarName": "青科大课表",
                "accountName": "qust_schedule_account",
                "accountType": "com.qust.schedule",
                "enableSummerTime": true
            }
        """.trimIndent()

        val gson = Gson()
        val parsed = gson.fromJson(legacyJson, TermConfig::class.java)

        // 旧版字段存在旧值
        assertEquals("com.qust.schedule", parsed.accountType)
        // 但通过 effectiveAccountType 或更新逻辑能强制得到 LOCAL
        assertEquals(CalendarContract.ACCOUNT_TYPE_LOCAL, parsed.effectiveAccountType)

        val fixed = if (parsed.accountType != CalendarContract.ACCOUNT_TYPE_LOCAL) {
            parsed.copy(accountType = CalendarContract.ACCOUNT_TYPE_LOCAL)
        } else {
            parsed
        }

        assertEquals(CalendarContract.ACCOUNT_TYPE_LOCAL, fixed.accountType)
    }
}
