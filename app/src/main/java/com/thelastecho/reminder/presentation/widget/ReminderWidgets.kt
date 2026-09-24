package com.thelastecho.reminder.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.presentation.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class ReminderWidgetKind { TODAY, UPCOMING, COMPACT }

private class ReminderListWidget(private val kind: ReminderWidgetKind) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rows = loadRows(context, kind)
        val preferences = UserPreferencesRepository(context).themeSettings.first()
        val dark = when (preferences.darkThemeConfig) {
            DarkThemeConfig.DARK -> true
            DarkThemeConfig.LIGHT -> false
            DarkThemeConfig.FOLLOW_SYSTEM -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val backgroundColor = if (dark && preferences.isAmoledMode) androidx.compose.ui.graphics.Color.Black else if (dark) androidx.compose.ui.graphics.Color(0xFF202124) else androidx.compose.ui.graphics.Color(0xFFF5F5FA)
        val foregroundColor = if (dark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF15151B)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ColorProvider(backgroundColor)).padding(16.dp)) {
                Text(when (kind) {
                    ReminderWidgetKind.TODAY -> context.getString(R.string.today)
                    ReminderWidgetKind.UPCOMING -> context.getString(R.string.scheduled)
                    ReminderWidgetKind.COMPACT -> context.getString(R.string.reminders)
                }, style = TextStyle(fontSize = 18.sp, color = ColorProvider(foregroundColor)))
                Spacer(GlanceModifier.height(8.dp))
                if (rows.isEmpty()) Text(context.getString(R.string.no_reminders_here), style = TextStyle(fontSize = 14.sp, color = ColorProvider(foregroundColor)))
                else rows.forEach { reminder ->
                    val reminderIdKey = ActionParameters.Key<Long>("reminder_id")
                    Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(reminderIdKey to reminder.id))).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(reminder.title, maxLines = 1, style = TextStyle(fontSize = 14.sp, color = ColorProvider(foregroundColor)))
                    }
                }
            }
        }
    }

    private suspend fun loadRows(context: Context, kind: ReminderWidgetKind): List<Reminder> {
        val database = ReminderDatabase.getInstance(context)
        val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
        val now = System.currentTimeMillis()
        val active = repository.getAllReminders().first().filter { !it.isCompleted }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return when (kind) {
            ReminderWidgetKind.TODAY -> active.filter { r -> r.dueDateTimeEpochMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today } == true }.take(6)
            ReminderWidgetKind.UPCOMING -> active.filter { (it.dueDateTimeEpochMillis ?: Long.MIN_VALUE) > now }.take(6)
            ReminderWidgetKind.COMPACT -> active.take(4)
        }
    }
}

class TodayRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ReminderListWidget(ReminderWidgetKind.TODAY) }
class UpcomingRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ReminderListWidget(ReminderWidgetKind.UPCOMING) }
class CompactRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ReminderListWidget(ReminderWidgetKind.COMPACT) }
