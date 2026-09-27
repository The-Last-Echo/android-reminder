package com.thelastecho.reminder.core.alarm

import com.thelastecho.reminder.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedReminderAlarmSchedulerTest {
    @Test
    fun appliesOnlyActiveFutureRemindersAndCancelsReplacedIds() {
        val fake = RecordingAlarmScheduler()
        val importer = ImportedReminderAlarmScheduler(fake) { 100L }
        val reminders = listOf(
            Reminder(id = 1, title = "Future", dueDateTimeEpochMillis = 200L),
            Reminder(id = 2, title = "Completed", dueDateTimeEpochMillis = 300L, isCompleted = true),
            Reminder(id = 3, title = "Past", dueDateTimeEpochMillis = 50L),
            Reminder(id = 4, title = "No date")
        )

        val report = importer.apply(reminders, replacedReminderIds = setOf(9L))

        assertEquals(listOf(1L), fake.scheduled)
        assertEquals(setOf(2L, 3L, 4L, 9L), fake.cancelled.toSet())
        assertEquals(1, report.scheduledCount)
        assertTrue(report.failedReminderIds.isEmpty())
    }

    @Test
    fun reportsScheduleAndCancelFailuresWithoutThrowing() {
        val fake = RecordingAlarmScheduler(failingScheduleId = 10L, failingCancelId = 20L)
        val importer = ImportedReminderAlarmScheduler(fake) { 100L }

        val report = importer.apply(
            listOf(
                Reminder(id = 10, title = "Cannot schedule", dueDateTimeEpochMillis = 200L),
                Reminder(id = 20, title = "Cannot cancel", isCompleted = true)
            ),
            replacedReminderIds = setOf(30L)
        )

        assertEquals(0, report.scheduledCount)
        assertEquals(setOf(10L, 20L), report.failedReminderIds)
        assertEquals(listOf(30L), fake.cancelled)
    }

    @Test
    fun successfulReplacementScheduleClearsStaleCancelFailure() {
        val fake = RecordingAlarmScheduler(failingCancelId = 40L)
        val importer = ImportedReminderAlarmScheduler(fake) { 100L }

        val report = importer.apply(
            listOf(Reminder(id = 40, title = "Replacement", dueDateTimeEpochMillis = 200L)),
            replacedReminderIds = setOf(40L)
        )

        assertEquals(listOf(40L), fake.scheduled)
        assertTrue(report.failedReminderIds.isEmpty())
    }

    private class RecordingAlarmScheduler(
        private val failingScheduleId: Long? = null,
        private val failingCancelId: Long? = null
    ) : AlarmScheduler {
        val scheduled = mutableListOf<Long>()
        val cancelled = mutableListOf<Long>()

        override fun schedule(reminder: Reminder) {
            if (reminder.id == failingScheduleId) error("schedule failed")
            scheduled += reminder.id
        }

        override fun cancel(reminderId: Long) {
            if (reminderId == failingCancelId) error("cancel failed")
            cancelled += reminderId
        }

        override fun canScheduleExactAlarms(): Boolean = true
    }
}
