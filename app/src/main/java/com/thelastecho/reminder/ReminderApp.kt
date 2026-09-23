package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels at application startup
        val preferencesRepository = UserPreferencesRepository(this)
        ReminderNotificationManager(this, preferencesRepository)
    }
}
