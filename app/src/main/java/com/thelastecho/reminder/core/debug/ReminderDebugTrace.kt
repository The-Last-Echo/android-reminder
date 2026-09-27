package com.thelastecho.reminder.core.debug

import android.util.Log
import com.thelastecho.reminder.BuildConfig

data class ReminderDebugTrace(
    val step: String,
    val reminderId: Long? = null,
    val state: String? = null,
    val extra: Map<String, String> = emptyMap()
) {
    fun toLogMessage(): String {
        val parts = buildList {
            add("step=$step")
            reminderId?.let { add("reminderId=$it") }
            state?.let { add("state=$state") }
            extra.forEach { (key, value) -> add("$key=$value") }
        }
        return parts.joinToString(" | ")
    }

    companion object {
        fun log(step: String, reminderId: Long? = null, state: String? = null, extra: Map<String, String> = emptyMap()) {
            if (!BuildConfig.DEBUG) return
            ReminderDebugLogger.log(ReminderDebugTrace(step, reminderId, state, extra))
        }
    }
}

internal object ReminderDebugLogger {
    private const val TAG = "ReminderDebug"

    fun log(trace: ReminderDebugTrace) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, trace.toLogMessage())
    }
}