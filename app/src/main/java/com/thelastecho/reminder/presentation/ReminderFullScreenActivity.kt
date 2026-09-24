package com.thelastecho.reminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.core.designsystem.ReminderTheme

class ReminderFullScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra("reminder_id", -1L)
        val title = intent.getStringExtra("reminder_title").orEmpty()
        val notes = intent.getStringExtra("reminder_notes").orEmpty()
        val photoUri = intent.getStringExtra(com.thelastecho.reminder.core.notification.ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI)
        if (savedInstanceState == null && id >= 0L && intent.getBooleanExtra(com.thelastecho.reminder.core.notification.AlarmSoundService.EXTRA_START_FROM_VISIBLE_ACTIVITY, false)) {
            runCatching {
                com.thelastecho.reminder.core.notification.AlarmSoundService.start(this, id, title, notes, photoUri)
            }
        }
        val image = photoUri?.let { raw ->
            runCatching {
                val uri = android.net.Uri.parse(raw)
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
                val sample = if (maxDimension > 1024) (maxDimension / 1024).coerceAtLeast(1) else 1
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
        setContent {
            ReminderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                        image?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.padding(top = 16.dp).size(220.dp)) }
                        if (notes.isNotBlank()) Text(notes, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = {
                            stopService(android.content.Intent(this@ReminderFullScreenActivity, com.thelastecho.reminder.core.notification.AlarmSoundService::class.java))
                            startActivity(android.content.Intent(this@ReminderFullScreenActivity, MainActivity::class.java).apply {
                                putExtra("reminder_id", id)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            })
                            finish()
                        }, modifier = Modifier.padding(top = 24.dp)) { Text(stringResource(com.thelastecho.reminder.R.string.open_reminder)) }
                        Button(onClick = {
                            stopService(android.content.Intent(this@ReminderFullScreenActivity, com.thelastecho.reminder.core.notification.AlarmSoundService::class.java))
                            getSystemService(android.app.NotificationManager::class.java).cancel(id.toInt())
                            finish()
                        }) { Text(stringResource(com.thelastecho.reminder.R.string.dismiss)) }
                    }
                }
            }
        }
    }
}
