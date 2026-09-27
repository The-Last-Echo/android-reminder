package com.thelastecho.reminder.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafBackupDestinationTest {
    @Test
    fun retentionIncludesOnlyAutomaticBackupNames() {
        assertTrue(SafBackupDestination.isAutomaticBackupName("Reminder-Auto-100-abc.reminderbackup"))
        assertFalse(SafBackupDestination.isAutomaticBackupName("Reminder-Manual-100-abc.reminderbackup"))
        assertFalse(SafBackupDestination.isAutomaticBackupName("Reminder-100-abc.reminderbackup"))
        assertFalse(SafBackupDestination.isAutomaticBackupName("other-Auto-100-abc.reminderbackup"))
    }
}
