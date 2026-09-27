package com.thelastecho.reminder.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalBackupSecretUnavailableException : Exception("The local backup password is unavailable. Enter it again in backup settings.")

class EncryptedBackupSecretStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun savePassword(password: CharArray) {
        require(password.isNotEmpty()) { "The backup password must not be empty." }
        val bytes = String(password).toByteArray(StandardCharsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(bytes)
            val saved = preferences.edit()
                .putString(KEY_IV, android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP))
                .putString(KEY_CIPHERTEXT, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .commit()
            if (!saved) throw LocalBackupSecretUnavailableException()
        } finally {
            bytes.fill(0)
        }
    }

    fun loadPassword(): CharArray? {
        val ivString = preferences.getString(KEY_IV, null) ?: return null
        val ciphertextString = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val clear = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_BITS, android.util.Base64.decode(ivString, android.util.Base64.NO_WRAP))
            )
            cipher.doFinal(android.util.Base64.decode(ciphertextString, android.util.Base64.NO_WRAP))
        } catch (_: Exception) {
            throw LocalBackupSecretUnavailableException()
        }
        return try {
            String(clear, StandardCharsets.UTF_8).toCharArray()
        } finally {
            clear.fill(0)
        }
    }

    fun clear() {
        if (!preferences.edit().clear().commit()) throw LocalBackupSecretUnavailableException()
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = keyStore()
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val PREFERENCES_NAME = "backup_secret_store"
        private const val KEY_ALIAS = "reminder_backup_secret_wrap_v1"
        private const val KEY_IV = "iv"
        private const val KEY_CIPHERTEXT = "ciphertext"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
    }
}
