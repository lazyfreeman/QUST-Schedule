package com.qust.schedule.model

import java.util.UUID

/**
 * App 内部处理与渲染的课程标准模型
 */
data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int, // 1: 星期一, 7: 星期日
    val startSection: Int, // 起始节次 (1~11)
    val endSection: Int, // 结束节次 (1~11)
    val weeks: List<Int>, // 包含的具体周次，如 [1, 2, 3, 4, 5...]
    val rawWeekStr: String = "" // 原始周次文本，如 "1-16周"
)
