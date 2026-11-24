package com.qust.schedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.model.TermConfig
import com.qust.schedule.ui.theme.QustPrimary

@Composable
fun SettingsDialog(
    initialConfig: TermConfig,
    timeSlotsCount: Int,
    initialUsername: String = "",
    initialPassword: String = "",
    initialAutoLogin: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (TermConfig, String, String, Boolean) -> Unit,
    onOpenCustomTimeSlots: () -> Unit
) {
    var firstMonday by remember { mutableStateOf(initialConfig.firstMonday) }
    var isDateValid by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableIntStateOf(initialConfig.reminderMinutes) }
    var calendarName by remember { mutableStateOf(initialConfig.calendarName) }
    var enableSummerTime by remember { mutableStateOf(initialConfig.enableSummerTime) }

    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var autoLogin by remember { mutableStateOf(initialAutoLogin) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "系统与自动化设置",
                fontWeight = FontWeight.Bold,
                color = QustPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 自动化凭据配置卡片
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = QustPrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = QustPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "教务与 WebVPN 自动登录",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = QustPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "解决认证 Cookie 过期快的问题。保存后将在更新课表时全自动代填学号密码、登录并自动提取课表。",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { autoLogin = !autoLogin }
                        ) {
                            Checkbox(
                                checked = autoLogin,
                                onCheckedChange = { autoLogin = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "开启全流程自动化代填与提取",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("学号 / 工号") },
                            placeholder = { Text("例如：2108000000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("登录密码") },
                            placeholder = { Text("WebVPN / 统一身份认证密码") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (username.isNotBlank() || password.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        username = ""
                                        password = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("清空凭据", fontSize = 11.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "开学第一天上课日期 (第一周)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateInputFields(
                    initialDate = remember(initialConfig.firstMonday) { initialConfig.getFirstMondayLocalDate() },
                    onDateChanged = { valid, date, _ ->
                        isDateValid = valid
                        if (valid && date != null) {
                            val targetMonday = if (date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
                                date
                            } else {
                                date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                            }
                            firstMonday = targetMonday.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "日历日程分类名称",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = calendarName,
                    onValueChange = { calendarName = it },
                    placeholder = { Text("青科大课表") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 自定义作息时间入口卡片
                Text(
                    text = "课程作息时间设置",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenCustomTimeSlots() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = QustPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "自定义上课时间",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "当前已配置 $timeSlotsCount 个小节 (点击编辑)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 夏时制自动推迟开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enableSummerTime = !enableSummerTime }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableSummerTime,
                        onCheckedChange = { enableSummerTime = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "夏时制自动调整",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "每年5月1日~9月30日下午及晚上日程推迟30分钟，10月1日恢复",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "上课前系统提醒通知",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val reminderOptions = listOf(
                    15 to "提前 15 分钟提醒",
                    30 to "提前 30 分钟提醒",
                    10 to "提前 10 分钟提醒",
                    0 to "不提醒"
                )

                reminderOptions.forEach { (mins, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reminderMinutes = mins }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = (reminderMinutes == mins),
                            onClick = { reminderMinutes = mins }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = QustPrimary),
                enabled = isDateValid,
                onClick = {
                    val updated = initialConfig.copy(
                        firstMonday = firstMonday.trim(),
                        reminderMinutes = reminderMinutes,
                        calendarName = if (calendarName.isBlank()) "青科大课表" else calendarName.trim(),
                        enableSummerTime = enableSummerTime
                    )
                    onSave(updated, username.trim(), password.trim(), autoLogin)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
