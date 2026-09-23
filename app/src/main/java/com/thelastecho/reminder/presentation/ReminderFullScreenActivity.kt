package com.thelastecho.reminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
        setContent {
            ReminderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                        if (notes.isNotBlank()) Text(notes, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = {
                            startActivity(android.content.Intent(this@ReminderFullScreenActivity, MainActivity::class.java).apply {
                                putExtra("reminder_id", id)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            })
                            finish()
                        }, modifier = Modifier.padding(top = 24.dp)) { Text(stringResource(com.thelastecho.reminder.R.string.open_reminder)) }
                        Button(onClick = {
                            getSystemService(android.app.NotificationManager::class.java).cancel(id.toInt())
                            finish()
                        }) { Text(stringResource(com.thelastecho.reminder.R.string.dismiss)) }
                    }
                }
            }
        }
    }
}
