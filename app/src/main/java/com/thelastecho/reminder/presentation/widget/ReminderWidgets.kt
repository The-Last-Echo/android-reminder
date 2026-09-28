package com.thelastecho.reminder.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.reminderColorScheme
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.presentation.MainActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal val transparentWidgetBackgroundKey = booleanPreferencesKey("transparent_background")

private val reminderIdKey = ActionParameters.Key<Long>("reminder_id")
private val openNewReminderKey = ActionParameters.Key<Boolean>("open_new_reminder")

private class TodayRemindersWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val remindersFlow = activeRemindersFlow(context)
        val initialReminders = remindersFlow.first()
        val themeSettingsFlow = UserPreferencesRepository(context).themeSettings
        val initialThemeSettings = themeSettingsFlow.first()

        provideContent {
            TodayRemindersContent(
                context = context,
                remindersFlow = remindersFlow,
                initialReminders = initialReminders,
                themeSettingsFlow = themeSettingsFlow,
                initialThemeSettings = initialThemeSettings
            )
        }
    }
}

@Composable
private fun TodayRemindersContent(
    context: Context,
    remindersFlow: Flow<List<Reminder>>,
    initialReminders: List<Reminder>,
    themeSettingsFlow: Flow<AppThemeSettings>,
    initialThemeSettings: AppThemeSettings
) {
    val reminders by remindersFlow.collectAsState(initial = initialReminders)
    val themeSettings by themeSettingsFlow.collectAsState(initial = initialThemeSettings)
    val preferences = currentState<Preferences>()
    val isTransparent = preferences[transparentWidgetBackgroundKey] ?: false
    val darkTheme = when (themeSettings.themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    val colors = reminderColorScheme(
        context = context,
        darkTheme = darkTheme,
        isAmoledMode = themeSettings.themeMode == ThemeMode.AMOLED,
        dynamicColor = themeSettings.useDynamicColors,
        accentColor = themeSettings.accentColor,
        customAccentColor = themeSettings.customAccentColor,
        useCustomAccent = themeSettings.useCustomAccent
    )
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todaysReminders = reminders
        .filter { reminder ->
            reminder.dueDateTimeEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
            } == true
        }
        .sortedBy { it.dueDateTimeEpochMillis }
    val maxRows = ((androidx.glance.LocalSize.current.height - 42.dp).value / 42f).toInt().coerceIn(1, 5)
    val widgetBackground = if (isTransparent) Color.Transparent else colors.surfaceContainer

    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(widgetBackground))
            .padding(12.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.today),
                style = TextStyle(color = ColorProvider(colors.onSurface), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = todaysReminders.size.toString(),
                style = TextStyle(color = ColorProvider(colors.primary), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        if (todaysReminders.isEmpty()) {
            Text(
                text = context.getString(R.string.no_reminders_here),
                style = TextStyle(color = ColorProvider(colors.onSurfaceVariant), fontSize = 13.sp)
            )
        } else {
            todaysReminders.take(maxRows).forEach { reminder ->
                Column(
                    GlanceModifier.fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(reminderIdKey to reminder.id)))
                        .padding(vertical = 5.dp)
                ) {
                    Text(
                        text = reminder.title,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(colors.onSurface), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                    reminder.dueDateTimeEpochMillis?.let { dueTime ->
                        Text(
                            text = DateFormat.getTimeInstance(DateFormat.SHORT, context.resources.configuration.locales[0]).format(Date(dueTime)),
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(colors.onSurfaceVariant), fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

private class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val themeSettings = UserPreferencesRepository(context).themeSettings.first()
        val darkTheme = when (themeSettings.themeMode) {
            ThemeMode.DARK, ThemeMode.AMOLED -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val colors = reminderColorScheme(
            context = context,
            darkTheme = darkTheme,
            isAmoledMode = themeSettings.themeMode == ThemeMode.AMOLED,
            dynamicColor = themeSettings.useDynamicColors,
            accentColor = themeSettings.accentColor,
            customAccentColor = themeSettings.customAccentColor,
            useCustomAccent = themeSettings.useCustomAccent
        )
        provideContent {
            Column(
                GlanceModifier.fillMaxSize()
                    .background(ColorProvider(colors.primary))
                    .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(openNewReminderKey to true)))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", style = TextStyle(color = ColorProvider(colors.onPrimary), fontSize = 24.sp))
                Text(
                    text = context.getString(R.string.create_reminder),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(colors.onPrimary), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

class TodayRemindersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayRemindersWidget()
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

suspend fun updateReminderWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(TodayRemindersWidget::class.java).forEach { glanceId ->
        TodayRemindersWidget().update(context, glanceId)
    }
    manager.getGlanceIds(QuickAddWidget::class.java).forEach { glanceId ->
        QuickAddWidget().update(context, glanceId)
    }
}

internal suspend fun readTodayWidgetTransparency(context: Context, appWidgetId: Int): Boolean {
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    return TodayRemindersWidget().getAppWidgetState<Preferences>(context, glanceId)[transparentWidgetBackgroundKey] ?: false
}

internal suspend fun saveTodayWidgetTransparency(context: Context, appWidgetId: Int, isTransparent: Boolean) {
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    updateAppWidgetState(context, glanceId) { preferences ->
        preferences[transparentWidgetBackgroundKey] = isTransparent
    }
    TodayRemindersWidget().update(context, glanceId)
}

private fun activeRemindersFlow(context: Context): Flow<List<Reminder>> {
    val database = ReminderDatabase.getInstance(context)
    return ReminderRepositoryImpl(database.reminderDao(), database.categoryDao()).getActiveReminders()
}