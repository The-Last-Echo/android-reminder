package com.thelastecho.reminder.data.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupPasswordOrCorruptException : IOException("The password is incorrect or the backup is damaged.")
class UnsupportedBackupEnvelopeException : IOException("This encrypted backup version is not supported.")

object BackupEnvelope {
    private val magic = byteArrayOf(0x52, 0x4D, 0x42, 0x4B)
    private const val VERSION = 1
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + NONCE_BYTES

    fun hasMagic(prefix: ByteArray): Boolean = prefix.size >= magic.size && prefix.copyOf(magic.size).contentEquals(magic)

    fun isEncrypted(input: InputStream): Boolean {
        val prefix = ByteArray(magic.size)
        var offset = 0
        while (offset < prefix.size) {
            val count = input.read(prefix, offset, prefix.size - offset)
            if (count < 0) return false
            offset += count
        }
        return hasMagic(prefix)
    }

    fun encrypt(archive: File, output: OutputStream, password: CharArray) {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, ITERATIONS)
        val headerBytes = ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { header ->
                header.write(magic)
                header.writeByte(VERSION)
                header.writeInt(ITERATIONS)
                header.write(salt)
                header.write(nonce)
            }
        }.toByteArray()
        cipher.updateAAD(headerBytes)
        output.write(headerBytes)
        FileInputStream(archive).use { input ->
            val buffer = ByteArray(8192)
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                cipher.update(buffer, 0, count)?.let(output::write)
            }
        }
        cipher.doFinal()?.let(output::write)
        output.flush()
    }

    fun decrypt(input: InputStream, cacheDirectory: File, maximumBytes: Long, password: CharArray): File {
        val temp = File.createTempFile("reminder-restore-", ".zip", cacheDirectory)
        try {
            val header = DataInputStream(input)
            val foundMagic = ByteArray(magic.size)
            header.readFully(foundMagic)
            if (!foundMagic.contentEquals(magic)) throw UnsupportedBackupEnvelopeException()
            if (header.readUnsignedByte() != VERSION) throw UnsupportedBackupEnvelopeException()
            val iterations = header.readInt()
            if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw UnsupportedBackupEnvelopeException()
            val salt = ByteArray(SALT_BYTES).also(header::readFully)
            val nonce = ByteArray(NONCE_BYTES).also(header::readFully)
            val cipher = cipher(Cipher.DECRYPT_MODE, password, salt, nonce, iterations)
            val aad = ByteArrayOutputStream().also { buffer ->
                DataOutputStream(buffer).use { data ->
                    data.write(magic)
                    data.writeByte(VERSION)
                    data.writeInt(iterations)
                    data.write(salt)
                    data.write(nonce)
                }
            }.toByteArray()
            cipher.updateAAD(aad)
            var encryptedBytes = HEADER_BYTES.toLong()
            var clearBytes = 0L
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(8192)
                var count: Int
                while (header.read(buffer).also { count = it } != -1) {
                    encryptedBytes += count
                    if (encryptedBytes > maximumBytes + HEADER_BYTES) throw IOException("The backup exceeds the supported size limit.")
                    val clear = cipher.update(buffer, 0, count)
                    if (clear != null) {
                        clearBytes += clear.size
                        if (clearBytes > maximumBytes) throw IOException("The backup exceeds the supported size limit.")
                        output.write(clear)
                    }
                }
                try {
                    val finalBytes = cipher.doFinal()
                    clearBytes += finalBytes.size
                    if (clearBytes > maximumBytes) throw IOException("The backup exceeds the supported size limit.")
                    output.write(finalBytes)
                } catch (_: AEADBadTagException) {
                    throw BackupPasswordOrCorruptException()
                }
            }
            return temp
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, nonce: ByteArray, iterations: Int): Cipher {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    private const val MIN_ITERATIONS = 200_000
    private const val MAX_ITERATIONS = 2_000_000
}
