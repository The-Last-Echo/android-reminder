package com.thelastecho.reminder.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSettingsDestinationTest {
    @Test
    fun manualAndAutomaticBackupsUseIndependentDestinations() {
        val settings = BackupSettings(
            destinationTreeUri = "content://automatic-folder",
            manualDestinationTreeUri = "content://manual-folder"
        )

        assertEquals("content://manual-folder", settings.destinationFor(SafBackupDestination.Origin.MANUAL))
        assertEquals("content://automatic-folder", settings.destinationFor(SafBackupDestination.Origin.AUTOMATIC))
    }

    @Test
    fun manualBackupFallsBackToExistingDestinationUntilOneIsChosen() {
        val settings = BackupSettings(destinationTreeUri = "content://existing-folder")

        assertEquals("content://existing-folder", settings.destinationFor(SafBackupDestination.Origin.MANUAL))
    }
}