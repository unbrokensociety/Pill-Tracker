package com.example.util

import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "PillTrackerMasterKey_v150"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey
            }

            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val keySpec = android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback key generation if AndroidKeyStore fails in test/emulator environment
            val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            "ENC:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        if (!cipherText.startsWith("ENC:")) return cipherText // Not encrypted or raw text
        val rawData = cipherText.removePrefix("ENC:")
        return try {
            val combined = Base64.decode(rawData, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return cipherText
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }
}
