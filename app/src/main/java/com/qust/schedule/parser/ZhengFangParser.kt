package com.qust.schedule.parser

import com.google.gson.Gson
import com.qust.schedule.model.Course
import com.qust.schedule.model.RawKbItem
import com.qust.schedule.model.RawZhengFangResponse

object ZhengFangParser {

    private val gson = Gson()

    /**
     * 将从正方接口获取到的 JSON 字符串解析为 Course 列表
     */
    fun parseJson(jsonStr: String): List<Course> {
        val response = try {
            gson.fromJson(jsonStr, RawZhengFangResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val items = response?.kbList ?: emptyList()
        return items.mapNotNull { convertToCourse(it) }
    }

    private fun convertToCourse(item: RawKbItem): Course? {
        val name = item.kcmc?.trim() ?: return null
        if (name.isEmpty()) return null

        val location = item.cdmc?.trim() ?: "待定"
        val teacher = item.xm?.trim() ?: "待定"

        val dayOfWeek = parseDayOfWeek(item.xqj)
        val (startSec, endSec) = parseSection(item.jcor)
        val weeks = parseWeeks(item.zcd)

        if (weeks.isEmpty()) return null

        return Course(
            name = name,
            location = location,
            teacher = teacher,
            dayOfWeek = dayOfWeek,
            startSection = startSec,
            endSection = endSec,
            weeks = weeks,
            rawWeekStr = item.zcd ?: ""
        )
    }

    /**
     * 解析星期几 (1~7)
     */
    fun parseDayOfWeek(xqjStr: String?): Int {
        if (xqjStr == null) return 1
        return when {
            xqjStr.contains("1") || xqjStr.contains("一") -> 1
            xqjStr.contains("2") || xqjStr.contains("二") -> 2
            xqjStr.contains("3") || xqjStr.contains("三") -> 3
            xqjStr.contains("4") || xqjStr.contains("四") -> 4
            xqjStr.contains("5") || xqjStr.contains("五") -> 5
            xqjStr.contains("6") || xqjStr.contains("六") -> 6
            xqjStr.contains("7") || xqjStr.contains("日") || xqjStr.contains("天") -> 7
            else -> xqjStr.toIntOrNull() ?: 1
        }
    }

    /**
     * 解析节次，如 "1-2" -> Pair(1, 2)
     */
    fun parseSection(jcorStr: String?): Pair<Int, Int> {
        if (jcorStr.isNullOrBlank()) return Pair(1, 2)
        val clean = jcorStr.replace("节", "").trim()
        val parts = clean.split("-")
        return if (parts.size >= 2) {
            val start = parts[0].toIntOrNull() ?: 1
            val end = parts[1].toIntOrNull() ?: start
            Pair(start, end)
        } else {
            val single = parts[0].toIntOrNull() ?: 1
            Pair(single, single)
        }
    }

    /**
     * 解析周次，如 "1-16周", "1-8周(单)", "2-16周(双)", "1-8周,10-17周(单)", "3,5,7周"
     */
    fun parseWeeks(zcdStr: String?): List<Int> {
        if (zcdStr.isNullOrBlank()) return emptyList()
        val result = mutableSetOf<Int>()

        // 统一全角半角逗号，按段拆分
        val segments = zcdStr.replace("，", ",").split(",")
        for (seg in segments) {
            val s = seg.trim()
            if (s.isEmpty()) continue

            val isOdd = s.contains("(单)") || s.contains("单周")
            val isEven = s.contains("(双)") || s.contains("双周")

            // 过滤中文括号与说明文字
            val numPart = s.replace(Regex("[\u4e00-\u9fa5()（）]"), "").trim()
            if (numPart.contains("-")) {
                val rangeParts = numPart.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].toIntOrNull() ?: continue
                    val end = rangeParts[1].toIntOrNull() ?: continue
                    for (w in start..end) {
                        if (isOdd && w % 2 == 0) continue
                        if (isEven && w % 2 != 0) continue
                        result.add(w)
                    }
                }
            } else {
                val singleWeek = numPart.toIntOrNull()
                if (singleWeek != null) {
                    result.add(singleWeek)
                }
            }
        }
        return result.sorted()
    }
}
