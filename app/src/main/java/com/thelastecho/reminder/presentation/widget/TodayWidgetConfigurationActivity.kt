package com.thelastecho.reminder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderTheme
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TodayWidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultIntent)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val initialTransparency = readTodayWidgetTransparency(this@TodayWidgetConfigurationActivity, appWidgetId)
            val themeSettings = UserPreferencesRepository(this@TodayWidgetConfigurationActivity).themeSettings.first()

            setContent {
                ReminderTheme(
                    darkTheme = when (themeSettings.themeMode) {
                        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK, ThemeMode.AMOLED -> true
                    },
                    isAmoledMode = themeSettings.themeMode == ThemeMode.AMOLED,
                    dynamicColor = themeSettings.useDynamicColors,
                    accentColor = themeSettings.accentColor,
                    customAccentColor = themeSettings.customAccentColor,
                    useCustomAccent = themeSettings.useCustomAccent
                ) {
                    var isTransparent by remember { mutableStateOf(initialTransparency) }
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(stringResource(R.string.today), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                stringResource(R.string.widget_transparency_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.widget_transparent_background))
                                Switch(checked = isTransparent, onCheckedChange = { isTransparent = it })
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { finish() }) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Button(onClick = {
                                    lifecycleScope.launch {
                                        saveTodayWidgetTransparency(this@TodayWidgetConfigurationActivity, appWidgetId, isTransparent)
                                        setResult(RESULT_OK, resultIntent)
                                        finish()
                                    }
                                }) {
                                    Text(stringResource(R.string.save))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}