package com.qust.schedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.model.Course
import com.qust.schedule.model.DefaultTimeSlots
import com.qust.schedule.model.TermConfig
import com.qust.schedule.model.TimeSlot
import com.qust.schedule.ui.theme.CourseColorPalettes
import com.qust.schedule.ui.theme.InactiveCourseColor
import com.qust.schedule.ui.theme.QustPrimary
import com.qust.schedule.ui.theme.QustSecondary
import com.qust.schedule.ui.theme.SuccessGreen
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    courses: List<Course>,
    termConfig: TermConfig,
    timeSlots: List<TimeSlot>,
    onLaunchLogin: () -> Unit,
    onSyncCalendar: () -> Unit,
    onClearCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCustomTimeSlots: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val firstMonday = remember(termConfig.firstMonday) { termConfig.getFirstMondayLocalDate() }

    // 计算当前真实周次
    val calculatedCurrentWeek = remember(firstMonday, today) {
        val days = ChronoUnit.DAYS.between(firstMonday, today)
        if (days >= 0) (days / 7).toInt() + 1 else 1
    }

    // 计算最大周次（课表中的最大周或默认25周）
    val maxWeek = remember(courses, calculatedCurrentWeek) {
        val maxInCourses = courses.flatMap { it.weeks }.maxOrNull() ?: 20
        maxOf(20, maxInCourses, calculatedCurrentWeek).coerceAtMost(30)
    }

    val initialPage = remember(calculatedCurrentWeek, maxWeek) {
        (calculatedCurrentWeek - 1).coerceIn(0, maxWeek - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { maxWeek }
    )

    val selectedWeek = pagerState.currentPage + 1
    val verticalScrollState = rememberScrollState()

    var isWeekSelectorExpanded by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var detailCoursesForDialog by remember { mutableStateOf<List<Course>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isWeekSelectorExpanded = !isWeekSelectorExpanded }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "第 $selectedWeek 周",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (isWeekSelectorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "切换周次",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (selectedWeek == calculatedCurrentWeek) "本周 (点击切换周次)" else "非当前周 (点击切换)",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        if (selectedWeek != calculatedCurrentWeek) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage((calculatedCurrentWeek - 1).coerceIn(0, maxWeek - 1))
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "回本周",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 更多选项（同步到日历入口在菜单内，避免顶部图标被误认为刷新课表）
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("获取/更新课表") },
                                onClick = {
                                    showMoreMenu = false
                                    onLaunchLogin()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.School, contentDescription = null, tint = QustPrimary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("同步到系统日历") },
                                onClick = {
                                    showMoreMenu = false
                                    onSyncCalendar()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = QustSecondary)
                                },
                                enabled = courses.isNotEmpty()
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("作息时间设置") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenCustomTimeSlots()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("学期与偏好设置") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("从系统日历清除课表", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showClearConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
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
            // 可展开的周次选择器
            AnimatedVisibility(
                visible = isWeekSelectorExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                WeekSelectorPanel(
                    currentWeek = calculatedCurrentWeek,
                    selectedWeek = selectedWeek,
                    maxWeek = maxWeek,
                    onSelectWeek = { week ->
                        coroutineScope.launch {
                            pagerState.scrollToPage(week - 1)
                        }
                        isWeekSelectorExpanded = false
                    }
                )
            }

            if (courses.isEmpty()) {
                // 空数据状态
                EmptyCourseState(onLaunchLogin = onLaunchLogin)
            } else {
                // 核心：支持左右滑动切换周次的课程表网格视图
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageWeek = page + 1
                    val pageMonday = remember(firstMonday, pageWeek) {
                        firstMonday.plusWeeks((pageWeek - 1).toLong())
                    }
                    TimetableGrid(
                        courses = courses,
                        selectedWeek = pageWeek,
                        selectedMonday = pageMonday,
                        today = today,
                        timeSlots = timeSlots,
                        enableSummerTime = termConfig.enableSummerTime,
                        verticalScrollState = verticalScrollState,
                        onCourseClick = { clickedCourses ->
                            detailCoursesForDialog = clickedCourses
                        }
                    )
                }
            }
        }
    }

    // 课程详情底部弹窗
    if (detailCoursesForDialog != null) {
        CourseDetailSheet(
            courses = detailCoursesForDialog!!,
            selectedWeek = selectedWeek,
            timeSlots = timeSlots,
            enableSummerTime = termConfig.enableSummerTime,
            onDismiss = { detailCoursesForDialog = null }
        )
    }

    // 清空日历确认弹窗
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认从系统日历中移除？") },
            text = { Text("将彻底删除系统本地日历中同步的青科大全部课程日程。本地已保存的课表数据不受影响。") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearCalendar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 周次展开切换面板
 */
@Composable
private fun WeekSelectorPanel(
    currentWeek: Int,
    selectedWeek: Int,
    maxWeek: Int,
    onSelectWeek: (Int) -> Unit
) {
    val targetIndex = (selectedWeek - 1).coerceIn(0, (maxWeek - 1).coerceAtLeast(0))
    val initialIndex = remember(selectedWeek, maxWeek) { (targetIndex - 2).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(selectedWeek, maxWeek) {
        listState.animateScrollToItem((selectedWeek - 3).coerceAtLeast(0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "切换周次",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "当前是第 $currentWeek 周",
                    fontSize = 12.sp,
                    color = QustSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 横向滑动行展示周次
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = maxWeek) { index ->
                    val week = index + 1
                    val isSelected = week == selectedWeek
                    val isCurrent = week == currentWeek

                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> QustPrimary
                                    isCurrent -> QustPrimary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                            .border(
                                width = if (isCurrent && !isSelected) 1.5.dp else 0.dp,
                                color = if (isCurrent && !isSelected) QustPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectWeek(week) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = week.toString(),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> Color.White
                                    isCurrent -> QustPrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = if (isCurrent) "本周" else "周",
                                fontSize = 10.sp,
                                color = when {
                                    isSelected -> Color.White.copy(alpha = 0.85f)
                                    isCurrent -> QustPrimary
                                    else -> Color.Gray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 核心课程表网格组件
 */
@Composable
private fun TimetableGrid(
    courses: List<Course>,
    selectedWeek: Int,
    selectedMonday: LocalDate,
    today: LocalDate,
    timeSlots: List<TimeSlot>,
    enableSummerTime: Boolean,
    verticalScrollState: ScrollState,
    onCourseClick: (List<Course>) -> Unit
) {
    val daysOfWeek = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val totalSections = maxOf(11, timeSlots.maxOfOrNull { it.section } ?: 11, courses.maxOfOrNull { it.endSection } ?: 11)
    val slotHeight = 62.dp
    val timeColWidth = 42.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 表头：月份与周一至周日日期
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左上角月份标识
            Box(
                modifier = Modifier
                    .width(timeColWidth)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${selectedMonday.monthValue}\n月",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QustPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }

            // 7天表头
            daysOfWeek.forEachIndexed { index, dayName ->
                val dayDate = selectedMonday.plusDays(index.toLong())
                val isToday = dayDate == today

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isToday) QustPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) QustPrimary else Color.DarkGray
                    )
                    Text(
                        text = "${dayDate.dayOfMonth}日",
                        fontSize = 10.sp,
                        color = if (isToday) QustPrimary else Color.Gray,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

        // 2. 课程表主体网格（可上下平滑滑动）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // 左侧时间与节次纵轴
            Column(
                modifier = Modifier
                    .width(timeColWidth)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                for (section in 1..totalSections) {
                    val isSummer = enableSummerTime && DefaultTimeSlots.isSummerTime(selectedMonday)
                    val effectiveSlots = if (isSummer) timeSlots.map { it.getEffectiveSlot(true) } else timeSlots
                    val slot = effectiveSlots.find { it.section == section }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(slotHeight)
                            .border(
                                width = 0.3.dp,
                                color = Color.LightGray.copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$section",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (slot != null) {
                                Text(
                                    text = slot.startTime,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = slot.endTime,
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // 右侧 7 天课程列
            Row(modifier = Modifier.weight(1f)) {
                for (dayIndex in 1..7) {
                    val dayCourses = courses.filter { it.dayOfWeek == dayIndex }
                    val dayDate = selectedMonday.plusDays((dayIndex - 1).toLong())
                    val isToday = dayDate == today

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(slotHeight * totalSections)
                            .background(if (isToday) QustPrimary.copy(alpha = 0.03f) else Color.Transparent)
                            .border(
                                width = 0.3.dp,
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                    ) {
                        // 背景底线
                        Column {
                            repeat(totalSections) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(slotHeight)
                                        .border(
                                            width = 0.2.dp,
                                            color = Color.LightGray.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }

                        // 按起止节次对课程聚类
                        val sectionGroups = dayCourses.groupBy { it.startSection to it.endSection }

                        sectionGroups.forEach { (_, coursesInSlot) ->
                            // 确定当前展示的代表课程（若本周有课优先展示本周课程，否则展示第一门非本周课）
                            val activeCourse = coursesInSlot.firstOrNull { it.weeks.contains(selectedWeek) }
                            val displayCourse = activeCourse ?: coursesInSlot.first()
                            val isThisWeek = activeCourse != null

                            CourseGridCard(
                                course = displayCourse,
                                isThisWeek = isThisWeek,
                                hasConflict = coursesInSlot.size > 1,
                                slotHeight = slotHeight,
                                onClick = { onCourseClick(coursesInSlot) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课表单个课程块
 */
@Composable
private fun CourseGridCard(
    course: Course,
    isThisWeek: Boolean,
    hasConflict: Boolean,
    slotHeight: Dp,
    onClick: () -> Unit
) {
    val topOffset = slotHeight * (course.startSection - 1).coerceAtLeast(0)
    val spanSections = (course.endSection - course.startSection + 1).coerceAtLeast(1)
    val cardHeight = slotHeight * spanSections - 3.dp

    // 分配配色方案
    val colorIndex = (course.name.hashCode().absoluteValue) % CourseColorPalettes.size
    val colorTheme = if (isThisWeek) CourseColorPalettes[colorIndex] else InactiveCourseColor

    Box(
        modifier = Modifier
            .offset(y = topOffset)
            .fillMaxWidth()
            .height(cardHeight)
            .padding(horizontal = 1.5.dp, vertical = 1.5.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colorTheme.bg)
            .border(
                width = 0.5.dp,
                color = colorTheme.border,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // 课程名
            Text(
                text = (if (!isThisWeek) "[非本周] " else "") + course.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colorTheme.text,
                lineHeight = 12.sp,
                maxLines = if (spanSections >= 2) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (spanSections >= 2 && course.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${course.location}",
                        fontSize = 9.sp,
                        color = colorTheme.text.copy(alpha = 0.85f),
                        lineHeight = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (spanSections >= 3 && course.teacher.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = course.teacher,
                    fontSize = 8.5.sp,
                    color = colorTheme.text.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 冲突角标提示（多门课程存在于同时段）
        if (hasConflict) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colorTheme.text.copy(alpha = 0.7f))
            )
        }
    }
}

/**
 * 课程详情底部抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailSheet(
    courses: List<Course>,
    selectedWeek: Int,
    timeSlots: List<TimeSlot>,
    enableSummerTime: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialIndex = remember(courses, selectedWeek) {
        val idx = courses.indexOfFirst { it.weeks.contains(selectedWeek) }
        if (idx >= 0) idx else 0
    }
    var selectedIndex by remember(courses, selectedWeek) { mutableIntStateOf(initialIndex) }
    val currentCourse = courses.getOrNull(selectedIndex) ?: courses.first()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 如果同时段有多门课（如单双周），提供切换选项卡
            if (courses.size > 1) {
                Text(
                    text = "该时段共有 ${courses.size} 门课程：",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = courses.size) { index ->
                        val c = courses[index]
                        val isCurrentSlot = index == selectedIndex
                        FilterChip(
                            selected = isCurrentSlot,
                            onClick = { selectedIndex = index },
                            label = {
                                Text(
                                    text = c.name + if (c.weeks.contains(selectedWeek)) " (本周)" else "",
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QustPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = QustPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 课程名称
            Text(
                text = currentCourse.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 标签行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isThisWeek = currentCourse.weeks.contains(selectedWeek)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isThisWeek) SuccessGreen.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isThisWeek) "本周有课" else "非本周",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isThisWeek) SuccessGreen else Color.DarkGray
                    )
                }

                val today = LocalDate.now()
                val isSummer = enableSummerTime && DefaultTimeSlots.isSummerTime(today)
                if (isSummer && (currentCourse.startSection >= 5 || currentCourse.endSection >= 5)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(QustSecondary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "夏时制作息 (下午推迟30分)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = QustSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // 详细信息条目
            val today = LocalDate.now()
            val isSummer = enableSummerTime && DefaultTimeSlots.isSummerTime(today)
            val (startLocalTime, endLocalTime) = DefaultTimeSlots.getStartEndTime(
                timeSlots,
                currentCourse.startSection,
                currentCourse.endSection,
                isSummer = isSummer
            )

            DetailRow(
                icon = Icons.Default.Schedule,
                label = "上课节次",
                value = "第 ${currentCourse.startSection} - ${currentCourse.endSection} 节 ($startLocalTime - $endLocalTime)"
            )

            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "上课教室",
                value = currentCourse.location.ifBlank { "未指定教室" }
            )

            DetailRow(
                icon = Icons.Default.Person,
                label = "授课教师",
                value = currentCourse.teacher.ifBlank { "未指定教师" }
            )

            DetailRow(
                icon = Icons.Default.CalendarMonth,
                label = "开课周次",
                value = currentCourse.rawWeekStr.ifBlank { "${currentCourse.weeks.firstOrNull() ?: 1}-${currentCourse.weeks.lastOrNull() ?: 16}周" }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = QustPrimary)
            ) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = QustPrimary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 空数据欢迎引导界面
 */
@Composable
private fun EmptyCourseState(onLaunchLogin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(QustPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = QustPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "欢迎使用青科大课表",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "尚未导入课表数据，请点击下方按钮登录青科教务系统，一键自动获取本学期课程表并生成直观网格视图。",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onLaunchLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = QustPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("登录教务网获取课表", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
