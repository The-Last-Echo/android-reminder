package com.thelastecho.reminder.presentation

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.designsystem.ReminderTheme
import com.thelastecho.reminder.core.notification.AlarmSoundService
import com.thelastecho.reminder.core.notification.NotificationActionReceiver
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date

class ReminderFullScreenActivity : ComponentActivity() {
    private lateinit var alarmViewModel: ReminderAlarmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the existing lockscreen and screen-on behavior unchanged.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val reminderId = intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE).orEmpty()
        val notes = intent.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES).orEmpty()
        val photoUri = intent.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI)

        if (savedInstanceState == null && reminderId >= 0L && intent.getBooleanExtra(AlarmSoundService.EXTRA_START_FROM_VISIBLE_ACTIVITY, false)) {
            runCatching { AlarmSoundService.start(this, reminderId, title, notes, photoUri) }
        }

        val image = photoUri?.let(::decodeReminderPhoto)
        val database = ReminderDatabase.getInstance(applicationContext)
        val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
        val alarmScheduler = AndroidAlarmScheduler(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (!modelClass.isAssignableFrom(ReminderAlarmViewModel::class.java)) {
                    error("Unknown ViewModel class: ${modelClass.name}")
                }
                return ReminderAlarmViewModel(
                    initialState = ReminderAlarmUiState(reminderId, title, notes, photoUri),
                    repository = repository,
                    alarmScheduler = alarmScheduler
                ) as T
            }
        }
        alarmViewModel = ViewModelProvider(this, factory)[ReminderAlarmViewModel::class.java]

        lifecycleScope.launch {
            alarmViewModel.effect.collect { effect ->
                when (effect) {
                    is ReminderAlarmEffect.Finish -> finishAlarm(reminderId, effect.openReminder)
                    ReminderAlarmEffect.DispatchDeleteAction -> {
                        sendBroadcast(Intent(this@ReminderFullScreenActivity, NotificationActionReceiver::class.java).apply {
                            action = NotificationActionReceiver.ACTION_DELETE
                            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
                        })
                        finish()
                    }
                }
            }
        }

        setContent {
            val state by alarmViewModel.uiState.collectAsState()
            val preferences = remember { UserPreferencesRepository(applicationContext) }
            val themeSettings by preferences.themeSettings.collectAsState(initial = AppThemeSettings())
            val darkTheme = when (themeSettings.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }

            ReminderTheme(
                darkTheme = darkTheme,
                isAmoledMode = themeSettings.isAmoledMode,
                dynamicColor = themeSettings.useDynamicColors
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ReminderAlarmContent(
                        state = state,
                        image = image,
                        onIntent = alarmViewModel::onIntent
                    )
                }
            }
        }
    }

    private fun finishAlarm(reminderId: Long, openReminder: Boolean) {
        AlarmSoundService.stopIfPlaying(this, reminderId)
        if (reminderId >= 0L) getSystemService(NotificationManager::class.java)?.cancel(reminderId.toInt())
        if (openReminder) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
        finish()
    }

    private fun decodeReminderPhoto(rawUri: String): Bitmap? = runCatching {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        val sample = if (maxDimension > 1024) (maxDimension / 1024).coerceAtLeast(1) else 1
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

@Composable
private fun ReminderAlarmContent(
    state: ReminderAlarmUiState,
    image: Bitmap?,
    onIntent: (ReminderAlarmIntent) -> Unit
) {
    val context = LocalContext.current
    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val currentTime = remember(currentTimeMillis) {
        android.text.format.DateFormat.getTimeFormat(context).format(Date(currentTimeMillis))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 560.dp
        val narrowWidth = maxWidth < 360.dp
        val horizontalPadding = if (narrowWidth) 20.dp else 32.dp
        val clockSize = if (narrowWidth) 64.sp else 78.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .padding(top = if (compactHeight) 12.dp else 24.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 180))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.alarm_active),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(Modifier.height(if (compactHeight) 12.dp else 22.dp))
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = clockSize,
                                lineHeight = clockSize * 1.08f,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-2).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(if (compactHeight) 8.dp else 14.dp))
                        Text(
                            text = state.title.ifBlank { stringResource(R.string.app_name) },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (image != null || state.notes.isNotBlank()) {
                            Spacer(Modifier.height(if (compactHeight) 10.dp else 16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                image?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = stringResource(R.string.photo_attached),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .padding(end = if (state.notes.isNotBlank()) 14.dp else 0.dp)
                                            .size(if (compactHeight) 64.dp else 84.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                                if (state.notes.isNotBlank()) {
                                    Text(
                                        text = state.notes,
                                        modifier = Modifier
                                            .then(if (image != null) Modifier.weight(1f, fill = false) else Modifier.fillMaxWidth())
                                            .padding(horizontal = if (image == null) 8.dp else 0.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (image != null) TextAlign.Start else TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isProcessing || state.errorMessageRes != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    state.errorMessageRes?.let { message ->
                        Text(
                            text = stringResource(message),
                            modifier = Modifier.padding(start = if (state.isProcessing) 10.dp else 0.dp),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val actionSpacing = if (narrowWidth) 10.dp else 14.dp
            if (narrowWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(actionSpacing)) {
                    SnoozeAlarmButton(!state.isProcessing, onIntent, Modifier.fillMaxWidth())
                    StopAlarmButton(!state.isProcessing, onIntent, Modifier.fillMaxWidth())
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(actionSpacing)) {
                    SnoozeAlarmButton(!state.isProcessing, onIntent, Modifier.weight(1f))
                    StopAlarmButton(!state.isProcessing, onIntent, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = !state.isProcessing,
                    onClick = { onIntent(ReminderAlarmIntent.Complete) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.action_complete), textAlign = TextAlign.Center, maxLines = 2)
                }
                TextButton(
                    enabled = !state.isProcessing,
                    onClick = { onIntent(ReminderAlarmIntent.Delete) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete), textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            TextButton(
                enabled = !state.isProcessing,
                onClick = { onIntent(ReminderAlarmIntent.OpenReminder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            ) {
                Text(stringResource(R.string.open_reminder), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SnoozeAlarmButton(
    enabled: Boolean,
    onIntent: (ReminderAlarmIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        enabled = enabled,
        onClick = { onIntent(ReminderAlarmIntent.Snooze) },
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = stringResource(R.string.action_snooze),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StopAlarmButton(
    enabled: Boolean,
    onIntent: (ReminderAlarmIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        enabled = enabled,
        onClick = { onIntent(ReminderAlarmIntent.Dismiss) },
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Text(
            text = stringResource(R.string.stop_alarm),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
