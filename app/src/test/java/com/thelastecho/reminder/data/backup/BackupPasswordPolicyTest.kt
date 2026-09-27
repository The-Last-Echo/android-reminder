package com.thelastecho.reminder.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupPasswordPolicyTest {
    @Test
    fun manualClearBackupDoesNotUseOrStoreAnyPassword() {
        assertNull(BackupPasswordPolicy.forManual(null))
    }

    @Test
    fun manualEncryptedBackupUsesOnlyThePasswordForThisInvocation() {
        val password = "one-time manual password".toCharArray()
        assertArrayEquals(password, BackupPasswordPolicy.forManual(password))
    }

    @Test
    fun automaticClearBackupIgnoresTheRememberedPassword() {
        assertNull(BackupPasswordPolicy.forAutomatic(false, "automatic secret".toCharArray()))
    }

    @Test
    fun automaticEncryptedBackupUsesTheDeviceStoredPassword() {
        val password = "device stored secret".toCharArray()
        assertArrayEquals(password, BackupPasswordPolicy.forAutomatic(true, password))
    }

    @Test
    fun manualBackupDoesNotReadAnAvailableAutomaticPassword() {
        val storedSecret = "automatic secret".toCharArray()
        var secretReads = 0
        val resolver = BackupPasswordResolver {
            secretReads++
            storedSecret.copyOf()
        }

        assertNull(resolver.forManual(null))
        assertEquals(0, secretReads)
    }
}
