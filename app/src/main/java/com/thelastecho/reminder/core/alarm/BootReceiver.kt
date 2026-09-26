package com.thelastecho.reminder.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.util.concurrent.atomic.AtomicBoolean

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()
        val resultFinished = AtomicBoolean(false)
        fun finishPendingResult() {
            if (resultFinished.compareAndSet(false, true)) pendingResult.finish()
        }
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    val database = ReminderDatabase.getInstance(appContext)
                    val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
                    val alarmScheduler = AndroidAlarmScheduler(appContext)
                    val activeReminders = repository.getActiveScheduledReminders()
                    activeReminders.forEach(alarmScheduler::schedule)
                } finally {
                    finishPendingResult()
                }
            }
        } catch (failure: Throwable) {
            finishPendingResult()
            throw failure
        }
    }
}
