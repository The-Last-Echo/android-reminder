package com.thelastecho.reminder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.LocalSize
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.reminderColorScheme
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.presentation.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class ReminderWidgetKind { TODAY, UPCOMING, COMPACT }

private class ReminderListWidget(private val kind: ReminderWidgetKind) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val remindersFlow = activeRemindersFlow(context)
        val initialReminders = remindersFlow.first()
        val themeSettingsFlow = UserPreferencesRepository(context).themeSettings
        val initialThemeSettings = themeSettingsFlow.first()
        provideContent {
            ReminderWidgetContent(
                context = context,
                kind = kind,
                remindersFlow = remindersFlow,
                initialReminders = initialReminders,
                themeSettingsFlow = themeSettingsFlow,
                initialThemeSettings = initialThemeSettings
            )
        }
    }
}

@Composable
private fun ReminderWidgetContent(
    context: Context,
    kind: ReminderWidgetKind,
    remindersFlow: Flow<List<Reminder>>,
    initialReminders: List<Reminder>,
    themeSettingsFlow: Flow<AppThemeSettings>,
    initialThemeSettings: AppThemeSettings
) {
    val reminders by remindersFlow.collectAsState(initial = initialReminders)
    val themeSettings by themeSettingsFlow.collectAsState(initial = initialThemeSettings)
    val dark = when (themeSettings.themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    val colors = reminderColorScheme(
        context = context,
        darkTheme = dark,
        isAmoledMode = themeSettings.themeMode == ThemeMode.AMOLED,
        dynamicColor = themeSettings.useDynamicColors,
        accentColor = themeSettings.accentColor,
        customAccentColor = themeSettings.customAccentColor,
        useCustomAccent = themeSettings.useCustomAccent
    )
    val backgroundColor = colors.surface.copy(alpha = themeSettings.widgetBackgroundOpacity / 100f)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val now = System.currentTimeMillis()
    val eligibleReminders = when (kind) {
        ReminderWidgetKind.TODAY -> reminders.filter { reminder ->
            reminder.dueDateTimeEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
            } == true
        }
        ReminderWidgetKind.UPCOMING -> reminders.filter {
            (it.dueDateTimeEpochMillis ?: Long.MIN_VALUE) > now
        }
        ReminderWidgetKind.COMPACT -> reminders
    }

    if (kind == ReminderWidgetKind.COMPACT) {
        CompactWidgetContent(context, eligibleReminders.firstOrNull(), backgroundColor, colors)
        return
    }

    val size = LocalSize.current
    val maxRows = if (size.height >= 160.dp) 5 else 2
    Column(GlanceModifier.fillMaxSize().background(ColorProvider(backgroundColor)).padding(8.dp)) {
        Text(
            when (kind) {
                ReminderWidgetKind.TODAY -> context.getString(R.string.today)
                ReminderWidgetKind.UPCOMING -> context.getString(R.string.scheduled)
                ReminderWidgetKind.COMPACT -> context.getString(R.string.reminders)
            },
            style = TextStyle(fontSize = 16.sp, color = ColorProvider(colors.onSurface))
        )
        Spacer(GlanceModifier.height(2.dp))
        if (eligibleReminders.isEmpty()) {
            Text(context.getString(R.string.no_reminders_here), style = TextStyle(fontSize = 14.sp, color = ColorProvider(colors.onSurfaceVariant)))
        } else {
            eligibleReminders.take(maxRows).forEach { reminder ->
                val reminderIdKey = ActionParameters.Key<Long>("reminder_id")
                Row(
                    GlanceModifier.fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(reminderIdKey to reminder.id)))
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(GlanceModifier.fillMaxWidth()) {
                        Text(reminder.title, maxLines = 1, style = TextStyle(fontSize = 12.sp, color = ColorProvider(colors.onSurface)))
                        Text(
                            reminder.dueDateTimeEpochMillis?.let { formatDueDateTime(context, it, today, zone) }.orEmpty(),
                            maxLines = 1,
                            style = TextStyle(fontSize = 10.sp, color = ColorProvider(colors.onSurfaceVariant))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWidgetContent(
    context: Context,
    reminder: Reminder?,
    backgroundColor: androidx.compose.ui.graphics.Color,
    colors: androidx.compose.material3.ColorScheme
) {
    Row(
        GlanceModifier.fillMaxSize().background(ColorProvider(backgroundColor)).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(GlanceModifier.defaultWeight()) {
            if (reminder == null) {
                Text(context.getString(R.string.no_reminders_here), maxLines = 2, style = TextStyle(fontSize = 12.sp, color = ColorProvider(colors.onSurfaceVariant)))
            } else {
                val reminderIdKey = ActionParameters.Key<Long>("reminder_id")
                Column(
                    GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(reminderIdKey to reminder.id)))
                ) {
                    Text(reminder.title, maxLines = 1, style = TextStyle(fontSize = 12.sp, color = ColorProvider(colors.onSurface)))
                    Text(
                        reminder.dueDateTimeEpochMillis?.let {
                            formatDueDateTime(context, it, LocalDate.now(ZoneId.systemDefault()), ZoneId.systemDefault())
                        }.orEmpty(),
                        maxLines = 1,
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(colors.onSurfaceVariant))
                    )
                }
            }
        }
        Spacer(GlanceModifier.width(6.dp))
        val addReminderKey = ActionParameters.Key<Boolean>("open_new_reminder")
        Row(
            GlanceModifier.background(ColorProvider(colors.primaryContainer))
                .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(addReminderKey to true)))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+", style = TextStyle(fontSize = 15.sp, color = ColorProvider(colors.onPrimaryContainer)))
            Spacer(GlanceModifier.width(3.dp))
            Text(context.getString(R.string.widget_add_reminder), maxLines = 1, style = TextStyle(fontSize = 10.sp, color = ColorProvider(colors.onPrimaryContainer)))
        }
    }
}

private fun activeRemindersFlow(context: Context): Flow<List<Reminder>> {
    val database = ReminderDatabase.getInstance(context)
    return ReminderRepositoryImpl(database.reminderDao(), database.categoryDao()).getActiveReminders()
}

private fun formatDueDateTime(context: Context, dueTimeMillis: Long, today: LocalDate, zone: ZoneId): String {
    val dateTime = Instant.ofEpochMilli(dueTimeMillis).atZone(zone)
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val formatter = if (dateTime.toLocalDate() == today) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    return formatter.withLocale(locale).format(dateTime)
}

private val todayWidget = ReminderListWidget(ReminderWidgetKind.TODAY)
private val upcomingWidget = ReminderListWidget(ReminderWidgetKind.UPCOMING)
private val compactWidget = ReminderListWidget(ReminderWidgetKind.COMPACT)

class TodayRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = todayWidget }
class UpcomingRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = upcomingWidget }
class CompactRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = compactWidget }

suspend fun updateReminderWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    listOf(
        TodayRemindersWidgetReceiver::class.java to todayWidget,
        UpcomingRemindersWidgetReceiver::class.java to upcomingWidget,
        CompactRemindersWidgetReceiver::class.java to compactWidget
    ).forEach { (receiverClass, widget) ->
        manager.getAppWidgetIds(ComponentName(context, receiverClass)).forEach { appWidgetId ->
            widget.update(context, AppWidgetId(appWidgetId))
        }
    }
}
