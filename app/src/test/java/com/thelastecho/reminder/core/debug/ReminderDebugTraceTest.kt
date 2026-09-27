package com.thelastecho.reminder.core.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDebugTraceTest {

    @Test
    fun traceFormatsReminderIdAndStateWithoutUserContent() {
        val trace = ReminderDebugTrace(
            step = "alarm.received",
            reminderId = 42L,
            state = "pending",
            extra = mapOf(
                "method" to "setExactAndAllowWhileIdle",
                "permission" to "granted"
            )
        )

        assertEquals(
            "step=alarm.received | reminderId=42 | state=pending | method=setExactAndAllowWhileIdle | permission=granted",
            trace.toLogMessage()
        )
    }
}