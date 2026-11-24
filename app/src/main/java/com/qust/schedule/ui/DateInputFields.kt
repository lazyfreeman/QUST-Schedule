package com.qust.schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.ui.theme.QustPrimary
import com.qust.schedule.ui.theme.SuccessGreen
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 年月日三文本框日期输入组件。
 * 包含三个独立文本框（年、月、日），数字之间的分隔符由组件自动呈现，禁止用户手动填写。
 * 支持输入校验、自动跳焦、实时周次/星期推算以及日历快速选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputFields(
    initialDate: LocalDate,
    modifier: Modifier = Modifier,
    showCalendarPickerButton: Boolean = true,
    onDateChanged: (isValid: Boolean, localDate: LocalDate?, formattedString: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val yearFocusRequester = remember { FocusRequester() }
    val monthFocusRequester = remember { FocusRequester() }
    val dayFocusRequester = remember { FocusRequester() }

    var yearText by remember { mutableStateOf(initialDate.year.toString()) }
    var monthText by remember { mutableStateOf(initialDate.monthValue.toString().padStart(2, '0')) }
    var dayText by remember { mutableStateOf(initialDate.dayOfMonth.toString().padStart(2, '0')) }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    // 当 initialDate 外部变化时同步
    LaunchedEffect(initialDate) {
        yearText = initialDate.year.toString()
        monthText = initialDate.monthValue.toString().padStart(2, '0')
        dayText = initialDate.dayOfMonth.toString().padStart(2, '0')
    }

    // 解析当前填写的日期
    val parseResult by remember(yearText, monthText, dayText) {
        derivedStateOf {
            val y = yearText.toIntOrNull()
            val m = monthText.toIntOrNull()
            val d = dayText.toIntOrNull()

            if (y == null || m == null || d == null) {
                DateParseResult.Incomplete
            } else if (y !in 1970..2100 || m !in 1..12 || d !in 1..31) {
                DateParseResult.Invalid
            } else {
                try {
                    val date = LocalDate.of(y, m, d)
                    DateParseResult.Valid(date)
                } catch (_: Exception) {
                    DateParseResult.Invalid
                }
            }
        }
    }

    // 回调通知外部状态
    LaunchedEffect(parseResult) {
        when (val res = parseResult) {
            is DateParseResult.Valid -> {
                val formatted = res.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                onDateChanged(true, res.date, formatted)
            }
            DateParseResult.Incomplete -> {
                val formatted = "$yearText-$monthText-$dayText"
                onDateChanged(false, null, formatted)
            }
            DateParseResult.Invalid -> {
                val formatted = "$yearText-$monthText-$dayText"
                onDateChanged(false, null, formatted)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 年份输入框 (4位)
            OutlinedTextField(
                value = yearText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(4)
                    yearText = filtered
                    if (filtered.length == 4) {
                        monthFocusRequester.requestFocus()
                    }
                },
                label = { Text("年", fontSize = 11.sp) },
                placeholder = { Text("2026", color = Color.LightGray) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { monthFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QustPrimary,
                    focusedLabelColor = QustPrimary
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .focusRequester(yearFocusRequester)
            )

            // 分隔符：连字符 - （用户无需填写）
            Text(
                text = "-",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // 月份输入框 (2位)
            OutlinedTextField(
                value = monthText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(2)
                    monthText = filtered
                    // 输满2位或输入的是2~9的单个数字（因月份最大12）时自动换焦到日期框
                    if (filtered.length == 2 || (filtered.length == 1 && filtered.toIntOrNull()?.let { it in 2..9 } == true)) {
                        dayFocusRequester.requestFocus()
                    }
                },
                label = { Text("月", fontSize = 11.sp) },
                placeholder = { Text("03", color = Color.LightGray) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { dayFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QustPrimary,
                    focusedLabelColor = QustPrimary
                ),
                modifier = Modifier
                    .weight(1.0f)
                    .focusRequester(monthFocusRequester)
            )

            // 分隔符：连字符 - （用户无需填写）
            Text(
                text = "-",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // 日期输入框 (2位)
            OutlinedTextField(
                value = dayText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(2)
                    dayText = filtered
                    if (filtered.length == 2) {
                        focusManager.clearFocus()
                    }
                },
                label = { Text("日", fontSize = 11.sp) },
                placeholder = { Text("02", color = Color.LightGray) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QustPrimary,
                    focusedLabelColor = QustPrimary
                ),
                modifier = Modifier
                    .weight(1.0f)
                    .focusRequester(dayFocusRequester)
            )

            // 可选：日历选择器快捷入口图标
            if (showCalendarPickerButton) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "在日历中选择日期",
                        tint = QustPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 提示与推算结果展示
        when (val res = parseResult) {
            is DateParseResult.Valid -> {
                val date = res.date
                val dayOfWeekStr = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "星期一"
                    DayOfWeek.TUESDAY -> "星期二"
                    DayOfWeek.WEDNESDAY -> "星期三"
                    DayOfWeek.THURSDAY -> "星期四"
                    DayOfWeek.FRIDAY -> "星期五"
                    DayOfWeek.SATURDAY -> "星期六"
                    DayOfWeek.SUNDAY -> "星期日"
                }

                val isMonday = date.dayOfWeek == DayOfWeek.MONDAY
                val mondayOfThatWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isMonday) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isMonday) SuccessGreen else QustPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMonday) {
                            "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ($dayOfWeekStr · 开学第一周)"
                        } else {
                            "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ($dayOfWeekStr)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMonday) SuccessGreen else QustPrimary
                    )
                }

                if (!isMonday) {
                    Text(
                        text = "💡 系统将以此周周一 (${mondayOfThatWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)}) 为第一周基准推算课程日程",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                    )
                }
            }
            DateParseResult.Invalid -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "请输入有效的公历日期 (如: 2026-03-02)",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }
            DateParseResult.Incomplete -> {
                Text(
                    text = "提示：请完整填写年、月、日数字（无需输入分隔符）",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }

    // Material 3 DatePicker 对话框
    if (showDatePickerDialog) {
        val selectedMillis = remember(parseResult) {
            val date = (parseResult as? DateParseResult.Valid)?.date ?: initialDate
            date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            yearText = pickedDate.year.toString()
                            monthText = pickedDate.monthValue.toString().padStart(2, '0')
                            dayText = pickedDate.dayOfMonth.toString().padStart(2, '0')
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("确定", color = QustPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private sealed interface DateParseResult {
    data class Valid(val date: LocalDate) : DateParseResult
    data object Incomplete : DateParseResult
    data object Invalid : DateParseResult
}
