package com.qust.schedule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.ui.theme.QustPrimary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 课表导入成功后的开学第一天上课日期引导对话框。
 * 引导用户填写/选择本学期第一天上课的日期。
 * 日期通过三个独立的文本框（年、月、日）填写，数字之间的分隔符无需用户输入。
 */
@Composable
fun FirstDayGuideDialog(
    initialDate: LocalDate,
    courseCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (confirmedFirstMonday: LocalDate) -> Unit
) {
    var isDateValid by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = QustPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设置第一天上课日期",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = QustPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎉 成功导入 $courseCount 门课程！",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = QustPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "为保证周次计算准确并顺利同步日程，请设置本学期第一天上课的日期（第一周）。",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "第一天上课日期 (年月日)：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 三个文本框日期输入组件（分隔符无需用户填写）
                DateInputFields(
                    initialDate = initialDate,
                    onDateChanged = { valid, date, _ ->
                        isDateValid = valid
                        selectedDate = date
                    }
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = QustPrimary),
                enabled = isDateValid && selectedDate != null,
                onClick = {
                    val date = selectedDate ?: initialDate
                    // 若选择的不是周一，智能换算为该周周一作为开学起始基准
                    val targetMonday = if (date.dayOfWeek == DayOfWeek.MONDAY) {
                        date
                    } else {
                        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    }
                    onConfirm(targetMonday)
                }
            ) {
                Text("保存并完成")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("稍后设置")
            }
        }
    )
}
