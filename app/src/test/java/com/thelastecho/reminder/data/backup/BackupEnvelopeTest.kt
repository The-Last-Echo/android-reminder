package com.thelastecho.reminder.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class BackupEnvelopeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun encryptThenDecryptPreservesEntirePayload() {
        val payload = ByteArray(256_000) { (it * 31).toByte() }
        val archive = File(temporaryFolder.root, "archive.zip").apply { writeBytes(payload) }
        val encrypted = ByteArrayOutputStream()
        BackupEnvelope.encrypt(archive, encrypted, "correct horse".toCharArray())

        val decrypted = BackupEnvelope.decrypt(
            encrypted.toByteArray().inputStream(),
            temporaryFolder.root,
            300_000,
            "correct horse".toCharArray()
        )

        assertArrayEquals(payload, decrypted.readBytes())
        assertTrue(BackupEnvelope.hasMagic(encrypted.toByteArray()))
    }

    @Test
    fun wrongPasswordDoesNotReturnPlaintext() {
        val archive = File(temporaryFolder.root, "archive.zip").apply { writeBytes("private payload".toByteArray()) }
        val encrypted = ByteArrayOutputStream()
        BackupEnvelope.encrypt(archive, encrypted, "correct".toCharArray())

        val result = runCatching {
            BackupEnvelope.decrypt(encrypted.toByteArray().inputStream(), temporaryFolder.root, 1024, "incorrect".toCharArray())
        }

        assertTrue(result.exceptionOrNull() is BackupPasswordOrCorruptException)
    }

    @Test
    fun modifiedCiphertextIsRejected() {
        val archive = File(temporaryFolder.root, "archive.zip").apply { writeBytes(ByteArray(2048) { it.toByte() }) }
        val encrypted = ByteArrayOutputStream()
        BackupEnvelope.encrypt(archive, encrypted, "password".toCharArray())
        val bytes = encrypted.toByteArray().also { it[it.lastIndex - 3] = (it[it.lastIndex - 3].toInt() xor 0x40).toByte() }

        val result = runCatching {
            BackupEnvelope.decrypt(bytes.inputStream(), temporaryFolder.root, 4096, "password".toCharArray())
        }

        assertTrue(result.exceptionOrNull() is BackupPasswordOrCorruptException)
    }

    @Test
    fun encryptedDetectionDistinguishesClearAndProtectedArchives() {
        val archive = File(temporaryFolder.root, "archive.zip").apply { writeBytes("zip payload".toByteArray()) }
        val clearBytes = archive.readBytes()
        val encrypted = ByteArrayOutputStream()
        BackupEnvelope.encrypt(archive, encrypted, "password".toCharArray())

        assertTrue(!BackupEnvelope.isEncrypted(clearBytes.inputStream()))
        assertTrue(BackupEnvelope.isEncrypted(encrypted.toByteArray().inputStream()))
    }
}
