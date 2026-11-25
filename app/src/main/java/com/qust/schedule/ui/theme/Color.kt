package com.qust.schedule.ui.theme

import androidx.compose.ui.graphics.Color

val QustPrimary = Color(0xFF1565C0)
val QustPrimaryLight = Color(0xFF5E92F3)
val QustPrimaryDark = Color(0xFF003C8F)

val QustSecondary = Color(0xFF1E88E5)
val QustSecondaryLight = Color(0xFF6AB7FF)
val QustSecondaryDark = Color(0xFF005CB2)

val BackgroundLight = Color(0xFFF7F9FA)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

val SuccessGreen = Color(0xFF2E7D32)
val WarningOrange = Color(0xFFE65100)

data class CourseColorTheme(
    val bg: Color,
    val text: Color,
    val border: Color = bg
)

val CourseColorPalettes = listOf(
    CourseColorTheme(bg = Color(0xFFE8F5E9), text = Color(0xFF1B5E20), border = Color(0xFFA5D6A7)), // 薄荷绿
    CourseColorTheme(bg = Color(0xFFE1F5FE), text = Color(0xFF01579B), border = Color(0xFF81D4FA)), // 晴空蓝
    CourseColorTheme(bg = Color(0xFFFFF3E0), text = Color(0xFFE65100), border = Color(0xFFFFCC80)), // 暖杏橙
    CourseColorTheme(bg = Color(0xFFF3E5F5), text = Color(0xFF4A148C), border = Color(0xFFCE93D8)), // 丁香紫
    CourseColorTheme(bg = Color(0xFFFCE4EC), text = Color(0xFF880E4F), border = Color(0xFFF48FB1)), // 珊瑚粉
    CourseColorTheme(bg = Color(0xFFE0F7FA), text = Color(0xFF006064), border = Color(0xFF80DEEA)), // 青蓝
    CourseColorTheme(bg = Color(0xFFFFF8E1), text = Color(0xFFF57F17), border = Color(0xFFFFE082)), // 琥珀黄
    CourseColorTheme(bg = Color(0xFFEDE7F6), text = Color(0xFF311B92), border = Color(0xFFB39DDB)), // 烟云紫
    CourseColorTheme(bg = Color(0xFFE3F2FD), text = Color(0xFF0D47A1), border = Color(0xFF90CAF9)), // 海洋蓝
    CourseColorTheme(bg = Color(0xFFFBE9E7), text = Color(0xFFBF360C), border = Color(0xFFFFAB91)), // 阳光橙
    CourseColorTheme(bg = Color(0xFFEFEBE9), text = Color(0xFF3E2723), border = Color(0xFFBCAAA4)), // 浅咖
    CourseColorTheme(bg = Color(0xFFE0F2F1), text = Color(0xFF004D40), border = Color(0xFF80CBC4))  // 翡翠青
)

val InactiveCourseColor = CourseColorTheme(
    bg = Color(0xFFF1F5F9),
    text = Color(0xFF94A3B8),
    border = Color(0xFFE2E8F0)
)

