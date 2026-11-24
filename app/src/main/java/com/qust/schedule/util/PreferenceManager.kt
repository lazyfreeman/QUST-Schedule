package com.qust.schedule.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qust.schedule.model.Course
import com.qust.schedule.model.DefaultTimeSlots
import com.qust.schedule.model.TermConfig
import com.qust.schedule.model.TimeSlot

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("qust_schedule_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCourses(courses: List<Course>) {
        val json = gson.toJson(courses)
        prefs.edit().putString(KEY_COURSES, json).apply()
    }

    fun getCourses(): List<Course> {
        val json = prefs.getString(KEY_COURSES, null) ?: return emptyList()
        val type = object : TypeToken<List<Course>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTermConfig(config: TermConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString(KEY_TERM_CONFIG, json).apply()
    }

    fun getTermConfig(): TermConfig {
        val json = prefs.getString(KEY_TERM_CONFIG, null) ?: return TermConfig()
        return try {
            val config = gson.fromJson(json, TermConfig::class.java) ?: TermConfig()
            // 修复历史旧配置：如果本地保存过非 LOCAL 账户类型，自动迁移纠正为 LOCAL，避免被系统清理
            if (config.accountType != android.provider.CalendarContract.ACCOUNT_TYPE_LOCAL) {
                val fixedConfig = config.copy(accountType = android.provider.CalendarContract.ACCOUNT_TYPE_LOCAL)
                saveTermConfig(fixedConfig)
                fixedConfig
            } else {
                config
            }
        } catch (e: Exception) {
            TermConfig()
        }
    }

    fun saveTimeSlots(slots: List<TimeSlot>, isCustomized: Boolean = true) {
        val json = gson.toJson(slots)
        prefs.edit()
            .putString(KEY_TIME_SLOTS, json)
            .putBoolean(KEY_CUSTOMIZED_SLOTS, isCustomized)
            .putInt(KEY_SLOTS_VERSION, CURRENT_SLOTS_VERSION)
            .apply()
    }

    fun hasUserCustomizedSlots(): Boolean {
        return prefs.getBoolean(KEY_CUSTOMIZED_SLOTS, false)
    }

    fun resetTimeSlotsToDefault(): List<TimeSlot> {
        val defaultSlots = DefaultTimeSlots.getDefault()
        saveTimeSlots(defaultSlots, isCustomized = false)
        return defaultSlots
    }

    fun getTimeSlots(): List<TimeSlot> {
        val json = prefs.getString(KEY_TIME_SLOTS, null) ?: return DefaultTimeSlots.getDefault()
        val type = object : TypeToken<List<TimeSlot>>() {}.type
        return try {
            val list: List<TimeSlot>? = gson.fromJson(json, type)
            val version = prefs.getInt(KEY_SLOTS_VERSION, 1)
            val isOld0830 = list?.firstOrNull()?.startTime == "08:30"
            val isOld0850 = list?.getOrNull(1)?.startTime == "08:50"
            if (list.isNullOrEmpty() || isOld0830 || isOld0850 || (version < CURRENT_SLOTS_VERSION && !hasUserCustomizedSlots())) {
                val newDefaults = DefaultTimeSlots.getDefault()
                saveTimeSlots(newDefaults, isCustomized = false)
                newDefaults
            } else {
                list
            }
        } catch (e: Exception) {
            DefaultTimeSlots.getDefault()
        }
    }

    fun saveLastSyncTime(timeStr: String) {
        prefs.edit().putString(KEY_LAST_SYNC, timeStr).apply()
    }

    fun getLastSyncTime(): String? {
        return prefs.getString(KEY_LAST_SYNC, null)
    }

    fun saveUserCredentials(username: String, password: String, autoLogin: Boolean = true) {
        val trimmedPwd = password.trim()
        if (trimmedPwd.startsWith("_RSA", ignoreCase = true)) {
            // 严禁将 _RSA 密文当作密码原文保存
            return
        }
        val encodedPassword = android.util.Base64.encodeToString(trimmedPwd.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_SAVED_USERNAME, username.trim())
            .putString(KEY_SAVED_PASSWORD, encodedPassword)
            .putBoolean(KEY_AUTO_LOGIN, autoLogin)
            .apply()
    }

    fun getSavedUsername(): String {
        return prefs.getString(KEY_SAVED_USERNAME, "") ?: ""
    }

    fun getSavedPassword(): String {
        val encoded = prefs.getString(KEY_SAVED_PASSWORD, "") ?: ""
        if (encoded.isBlank()) return ""
        val decoded = try {
            String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
        // 如果历史误存了 _RSA 开头的密文，过滤为空字符串，方便用户查看和重新录入正确的密码原文
        return if (decoded.startsWith("_RSA", ignoreCase = true)) "" else decoded
    }

    fun isAutoLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOGIN, true)
    }

    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    fun hasUserCredentials(): Boolean {
        return getSavedUsername().isNotBlank() && getSavedPassword().isNotBlank()
    }

    fun clearUserCredentials() {
        prefs.edit()
            .remove(KEY_SAVED_USERNAME)
            .remove(KEY_SAVED_PASSWORD)
            .remove(KEY_AUTO_LOGIN)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val CURRENT_SLOTS_VERSION = 3
        private const val KEY_SLOTS_VERSION = "key_slots_version"
        private const val KEY_COURSES = "key_courses"
        private const val KEY_TERM_CONFIG = "key_term_config"
        private const val KEY_TIME_SLOTS = "key_time_slots"
        private const val KEY_CUSTOMIZED_SLOTS = "key_customized_slots"
        private const val KEY_LAST_SYNC = "key_last_sync"
        private const val KEY_SAVED_USERNAME = "key_saved_username"
        private const val KEY_SAVED_PASSWORD = "key_saved_password"
        private const val KEY_AUTO_LOGIN = "key_auto_login"
    }
}
