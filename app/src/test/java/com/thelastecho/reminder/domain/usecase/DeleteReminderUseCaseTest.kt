package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.data.local.dao.ReminderDao
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.repository.ReminderRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteReminderUseCaseTest {

    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)

    @Test
    fun cancelsAlarmAndDelegatesDeletionToRepository() = runTest {
        val reminderId = 51L

        DeleteReminderUseCase(repository, scheduler)(reminderId)

        verify(exactly = 1) { scheduler.cancel(reminderId) }
        coVerify(exactly = 1) { repository.deleteReminder(reminderId) }
    }

    @Test
    fun softDeleteSetsExpirationNinetyDaysAfterDeletion() = runTest {
        val reminderId = 52L
        val reminderDao = mockk<ReminderDao>(relaxed = true)
        val repository = ReminderRepositoryImpl(reminderDao, mockk(relaxed = true))
        val scheduler = mockk<AlarmScheduler>(relaxed = true)
        val deletedAt = slot<Long>()
        val expiresAt = slot<Long>()
        val beforeDelete = System.currentTimeMillis()

        DeleteReminderUseCase(repository, scheduler)(reminderId)

        val afterDelete = System.currentTimeMillis()
        coVerify(exactly = 1) {
            reminderDao.softDeleteReminder(reminderId, capture(deletedAt), capture(expiresAt))
        }
        assertTrue(deletedAt.captured in beforeDelete..afterDelete)
        assertEquals(NINETY_DAYS_MILLIS, expiresAt.captured - deletedAt.captured)
        coVerify(exactly = 0) { reminderDao.deleteReminderById(reminderId) }
        verify(exactly = 1) { scheduler.cancel(reminderId) }
    }

    private companion object {
        const val NINETY_DAYS_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}