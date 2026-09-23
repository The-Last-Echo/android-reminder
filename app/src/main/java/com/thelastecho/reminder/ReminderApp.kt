package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels at application startup
        ReminderNotificationManager(this)
    }
}
