package com.thelastecho.reminder.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.model.SubTask
import com.thelastecho.reminder.domain.model.RepeatInterval
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderItem(
    reminder: Reminder,
    category: Category? = null,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleSubTask: (SubTask) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var checklistExpanded by remember(reminder.id) { mutableStateOf(false) }
    val completionScale = remember(reminder.id) { Animatable(1f) }
    val animationScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)), ReminderShapes.Card),
        shape = ReminderShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Completion Toggle Checkbox
            IconButton(
                onClick = {
                    animationScope.launch {
                        completionScale.snapTo(0.82f)
                        completionScale.animateTo(1.16f, tween(110))
                        completionScale.animateTo(1f, tween(130))
                    }
                    onToggleComplete()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = stringResource(if (reminder.isCompleted) com.thelastecho.reminder.R.string.completed else com.thelastecho.reminder.R.string.mark_complete),
                    tint = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = completionScale.value
                        scaleY = completionScale.value
                    }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Body Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Badges (Date, Repeat, Subtasks, Priority)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (category != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier = Modifier.size(8.dp).background(
                                    androidx.compose.ui.graphics.Color(category.colorArgb),
                                    CircleShape
                                )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Due Date / Time
                    if (reminder.dueDateTimeEpochMillis != null) {
                        val dateFormatted = formatDueDateTime(reminder.dueDateTimeEpochMillis, LocalConfiguration.current.locales[0])
                        val isOverdue = reminder.isOverdue
                        val dateColor = if (isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = dateColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                color = dateColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Repeating badge
                    if (reminder.repeatInterval != RepeatInterval.ONCE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val repeatText = if (reminder.repeatEvery > 1 && reminder.repeatInterval != RepeatInterval.ONCE) {
                                val unitRes = when (reminder.repeatInterval) {
                                    RepeatInterval.MINUTELY -> com.thelastecho.reminder.R.string.repeat_unit_minute
                                    RepeatInterval.HOURLY -> com.thelastecho.reminder.R.string.repeat_unit_hour
                                    RepeatInterval.DAILY, RepeatInterval.WEEKDAYS -> com.thelastecho.reminder.R.string.repeat_unit_day
                                    RepeatInterval.WEEKLY -> com.thelastecho.reminder.R.string.repeat_unit_week
                                    RepeatInterval.MONTHLY -> com.thelastecho.reminder.R.string.repeat_unit_month
                                    RepeatInterval.YEARLY -> com.thelastecho.reminder.R.string.repeat_unit_year
                                    RepeatInterval.ONCE -> com.thelastecho.reminder.R.string.repeat_unit_day
                                }
                                stringResource(com.thelastecho.reminder.R.string.repeat_frequency, reminder.repeatEvery, stringResource(unitRes))
                            } else {
                                stringResource(repeatLabelResource(reminder.repeatInterval))
                            }
                            Text(
                                text = repeatText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (reminder.repeatDuration == com.thelastecho.reminder.domain.model.RepeatDuration.COUNT) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        com.thelastecho.reminder.R.string.repeat_progress,
                                        reminder.repeatCompletedCount.coerceAtMost(reminder.repeatCount),
                                        reminder.repeatCount
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    // Subtasks progress badge
                    if (reminder.subTasks.isNotEmpty()) {
                        val completedCount = reminder.subTasks.count { it.isCompleted }
                        TextButton(
                            onClick = { checklistExpanded = !checklistExpanded },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = stringResource(com.thelastecho.reminder.R.string.subtasks_progress, completedCount, reminder.subTasks.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    // Priority
                    PriorityBadge(priority = reminder.priority)
                }

                if (reminder.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = checklistExpanded && reminder.subTasks.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(150)) + expandVertically(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(100)) + shrinkVertically(animationSpec = tween(160))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        reminder.subTasks.sortedBy { it.orderIndex }.forEach { subTask ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onToggleSubTask(subTask)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = subTask.isCompleted,
                                    onCheckedChange = { onToggleSubTask(subTask) }
                                )
                                Text(
                                    text = subTask.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Delete Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (reminder.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(if (reminder.isFavorite) com.thelastecho.reminder.R.string.remove_favorite else com.thelastecho.reminder.R.string.add_favorite),
                    tint = if (reminder.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(com.thelastecho.reminder.R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDueDateTime(epochMillis: Long, locale: java.util.Locale): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM).withLocale(locale))
}
