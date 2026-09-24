package com.thelastecho.reminder.presentation.components

import androidx.annotation.StringRes
import com.thelastecho.reminder.R
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.RepeatInterval

@StringRes
fun priorityLabelResource(priority: Priority): Int = when (priority) {
    Priority.HIGH -> R.string.priority_high
    Priority.MEDIUM -> R.string.priority_medium
    Priority.LOW -> R.string.priority_low
    Priority.NONE -> R.string.priority_none
}

@StringRes
fun repeatLabelResource(interval: RepeatInterval): Int = when (interval) {
    RepeatInterval.ONCE -> R.string.repeat_once
    RepeatInterval.HOURLY -> R.string.repeat_hourly
    RepeatInterval.DAILY -> R.string.repeat_daily
    RepeatInterval.WEEKDAYS -> R.string.repeat_weekdays
    RepeatInterval.WEEKLY -> R.string.repeat_weekly
    RepeatInterval.MONTHLY -> R.string.repeat_monthly
    RepeatInterval.YEARLY -> R.string.repeat_yearly
}
