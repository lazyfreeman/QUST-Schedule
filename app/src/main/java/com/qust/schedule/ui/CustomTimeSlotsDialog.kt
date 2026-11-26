package com.qust.schedule.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qust.schedule.model.DefaultTimeSlots
import com.qust.schedule.model.TimeSlot
import com.qust.schedule.ui.theme.QustPrimary
import com.qust.schedule.ui.theme.QustSecondary
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CustomTimeSlotsDialog(
    initialSlots: List<TimeSlot>,
    onDismiss: () -> Unit,
    onSave: (List<TimeSlot>) -> Unit
) {
    val context = LocalContext.current
    var slots by remember { mutableStateOf(initialSlots.sortedBy { it.section }) }
    var previewSummerTime by remember { mutableStateOf(false) }

    fun showPicker(currentTimeStr: String, onTimePicked: (String) -> Unit) {
        val parts = currentTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                onTimePicked(formatted)
            },
            hour,
            minute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自定义上课时间",
                    fontWeight = FontWeight.Bold,
                    color = QustPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 提示与说明卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(QustPrimary.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = QustPrimary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "默认时间依据学校作息时间。每年5月1日之后下午及晚上的日程将自动推迟30分钟（下午14:00、晚上18:30开课），10月1日恢复。",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 预览模式切换：冬时制 vs 夏时制（标题与切换按钮分行，避免按钮被挤压换行）
                Text(
                    text = "各节次时间表 (${slots.size}节)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !previewSummerTime,
                        onClick = { previewSummerTime = false },
                        label = { Text("冬时制/标准", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = previewSummerTime,
                        onClick = { previewSummerTime = true },
                        label = { Text("夏时制(+30m)", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 节次列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(slots) { index, slot ->
                        val effectiveSlot = slot.getEffectiveSlot(previewSummerTime)
                        TimeSlotItemRow(
                            slot = slot,
                            effectiveSlot = effectiveSlot,
                            isSummerPreview = previewSummerTime,
                            canDelete = (slots.size > 1 && index == slots.size - 1),
                            onPickStartTime = {
                                showPicker(slot.startTime) { newTime ->
                                    slots = slots.toMutableList().also { list ->
                                        list[index] = list[index].copy(startTime = newTime)
                                    }
                                }
                            },
                            onPickEndTime = {
                                showPicker(slot.endTime) { newTime ->
                                    slots = slots.toMutableList().also { list ->
                                        list[index] = list[index].copy(endTime = newTime)
                                    }
                                }
                            },
                            onDelete = {
                                slots = slots.filterIndexed { i, _ -> i != index }
                                    .mapIndexed { idx, s -> s.copy(section = idx + 1) }
                            }
                        )
                    }

                    // 添加节次按钮
                    item {
                        OutlinedButton(
                            onClick = {
                                val nextSection = slots.size + 1
                                val lastSlot = slots.lastOrNull()
                                val (newStart, newEnd) = if (lastSlot != null) {
                                    val lastEnd = lastSlot.getEndLocalTime()
                                    val start = lastEnd.plusMinutes(10)
                                    val end = start.plusMinutes(45)
                                    val fmt = DateTimeFormatter.ofPattern("HH:mm")
                                    Pair(start.format(fmt), end.format(fmt))
                                } else {
                                    Pair("08:00", "08:45")
                                }
                                slots = slots + TimeSlot(nextSection, newStart, newEnd)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加第 ${slots.size + 1} 节")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 恢复默认按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    OutlinedButton(
                        onClick = {
                            slots = DefaultTimeSlots.getDefault()
                            Toast.makeText(context, "已重置为官方默认作息时间", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复官方默认", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = QustPrimary),
                onClick = {
                    // 校验所有节次
                    for (slot in slots) {
                        try {
                            val start = slot.getStartLocalTime()
                            val end = slot.getEndLocalTime()
                            if (!end.isAfter(start)) {
                                Toast.makeText(context, "第 ${slot.section} 节结束时间必须晚于开始时间", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "第 ${slot.section} 节时间格式不正确", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                    }
                    onSave(slots)
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

@Composable
private fun TimeSlotItemRow(
    slot: TimeSlot,
    effectiveSlot: TimeSlot,
    isSummerPreview: Boolean,
    canDelete: Boolean,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onDelete: () -> Unit
) {
    val periodLabel = slot.getPeriodLabel()
    val periodBgColor = when (periodLabel) {
        "上午" -> Color(0xFFE8F5E9)
        "下午" -> Color(0xFFFFF3E0)
        else -> Color(0xFFEDE7F6)
    }
    val periodTextColor = when (periodLabel) {
        "上午" -> Color(0xFF2E7D32)
        "下午" -> Color(0xFFE65100)
        else -> Color(0xFF512DA8)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 节次序号与时段标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "第${slot.section}节",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.width(46.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(periodBgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = periodLabel,
                    fontSize = 10.sp,
                    color = periodTextColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 时间点击编辑区
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBadge(
                timeText = if (isSummerPreview) effectiveSlot.startTime else slot.startTime,
                isModifiedBySummer = isSummerPreview && slot.isShiftedInSummer(),
                onClick = onPickStartTime
            )

            Text(
                text = "-",
                modifier = Modifier.padding(horizontal = 4.dp),
                fontSize = 13.sp,
                color = Color.Gray
            )

            TimeBadge(
                timeText = if (isSummerPreview) effectiveSlot.endTime else slot.endTime,
                isModifiedBySummer = isSummerPreview && slot.isShiftedInSummer(),
                onClick = onPickEndTime
            )
        }

        // 删除按钮
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "删除节次",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }
    }
}

@Composable
private fun TimeBadge(
    timeText: String,
    isModifiedBySummer: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.dp,
                color = if (isModifiedBySummer) QustSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (isModifiedBySummer) QustSecondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isModifiedBySummer) QustSecondary else MaterialTheme.colorScheme.onSurface
        )
    }
}
