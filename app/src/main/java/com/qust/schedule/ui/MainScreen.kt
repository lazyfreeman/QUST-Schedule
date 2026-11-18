package com.qust.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.model.Course
import com.qust.schedule.model.DefaultTimeSlots
import com.qust.schedule.model.TermConfig
import com.qust.schedule.model.TimeSlot
import com.qust.schedule.ui.theme.QustPrimary
import com.qust.schedule.ui.theme.QustSecondary
import com.qust.schedule.ui.theme.SuccessGreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    courses: List<Course>,
    termConfig: TermConfig,
    timeSlots: List<TimeSlot>,
    lastSyncTime: String?,
    onLaunchLogin: () -> Unit,
    onSyncCalendar: () -> Unit,
    onClearCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCustomTimeSlots: () -> Unit
) {
    // 默认定位到今天所在的星期
    val todayIndex = remember {
        val day = LocalDate.now().dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        (day - 1).coerceIn(0, 6)
    }
    var selectedDayTab by remember { mutableIntStateOf(todayIndex) }
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "青科日历课表",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCustomTimeSlots) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "作息时间",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QustPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 状态卡片
            StatusCard(
                courseCount = courses.size,
                termConfig = termConfig,
                timeSlots = timeSlots,
                lastSyncTime = lastSyncTime,
                onOpenCustomTimeSlots = onOpenCustomTimeSlots
            )

            // 操作按钮区
            ActionButtons(
                hasCourses = courses.isNotEmpty(),
                onLaunchLogin = onLaunchLogin,
                onSyncCalendar = onSyncCalendar,
                onClearCalendar = onClearCalendar
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 课表预览星期导航条
            ScrollableTabRow(
                selectedTabIndex = selectedDayTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = QustPrimary
            ) {
                days.forEachIndexed { index, dayName ->
                    val dayCourseCount = courses.count { it.dayOfWeek == (index + 1) }
                    Tab(
                        selected = selectedDayTab == index,
                        onClick = { selectedDayTab = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayName,
                                    fontWeight = if (selectedDayTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dayCourseCount > 0) {
                                    Text(
                                        text = "$dayCourseCount 节",
                                        fontSize = 11.sp,
                                        color = if (selectedDayTab == index) QustSecondary else Color.Gray
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // 当日课程列表
            val currentDayCourses = courses.filter { it.dayOfWeek == (selectedDayTab + 1) }
                .sortedBy { it.startSection }

            if (currentDayCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (courses.isEmpty()) "点击上方按钮登录教务网获取课表" else "${days[selectedDayTab]} 没有课，好好享受吧~",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(currentDayCourses) { course ->
                        CourseCard(
                            course = course,
                            timeSlots = timeSlots,
                            enableSummerTime = termConfig.enableSummerTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    courseCount: Int,
    termConfig: TermConfig,
    timeSlots: List<TimeSlot>,
    lastSyncTime: String?,
    onOpenCustomTimeSlots: () -> Unit
) {
    val today = LocalDate.now()
    val isSummerToday = termConfig.enableSummerTime && DefaultTimeSlots.isSummerTime(today)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = QustPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (courseCount > 0) "已加载 $courseCount 门课程" else "尚未导入课表",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (courseCount > 0) SuccessGreen else Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "开学周一：${termConfig.firstMonday} (提前 ${termConfig.reminderMinutes} 分钟提醒)",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Text(
                text = "上次同步日历：${lastSyncTime ?: "尚未同步"}",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "作息状态：${if (isSummerToday) "夏时制 (下午及晚上推迟30分)" else "冬时制/标准"} · 共${timeSlots.size}节",
                    fontSize = 13.sp,
                    color = if (isSummerToday) QustSecondary else Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )

                OutlinedButton(
                    onClick = onOpenCustomTimeSlots,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自定义时间", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    hasCourses: Boolean,
    onLaunchLogin: () -> Unit,
    onSyncCalendar: () -> Unit,
    onClearCalendar: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 登录教务处
            Button(
                onClick = onLaunchLogin,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = QustPrimary)
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("获取/更新课表")
            }

            // 一键同步到日历
            Button(
                onClick = onSyncCalendar,
                enabled = hasCourses,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = QustSecondary)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("同步到日历")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 清除日历日程
        OutlinedButton(
            onClick = onClearCalendar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("从系统日历中移除青科大课表")
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    timeSlots: List<TimeSlot>,
    enableSummerTime: Boolean
) {
    val today = LocalDate.now()
    val isSummer = enableSummerTime && DefaultTimeSlots.isSummerTime(today)
    val (startLocalTime, endLocalTime) = DefaultTimeSlots.getStartEndTime(
        timeSlots,
        course.startSection,
        course.endSection,
        isSummer = isSummer
    )
    val isShiftedBySummer = isSummer && (course.startSection >= 5 || course.endSection >= 5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            // 装饰竖线
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QustSecondary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = course.location,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = course.teacher,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "第${course.startSection}-${course.endSection}节 ($startLocalTime-$endLocalTime)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (isShiftedBySummer) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(QustSecondary.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "夏时制+30m",
                                fontSize = 9.sp,
                                color = QustSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = " | ${course.rawWeekStr}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
