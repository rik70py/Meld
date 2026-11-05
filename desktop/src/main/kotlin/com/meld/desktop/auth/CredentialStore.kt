package com.meld.desktop.auth

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted credential storage for sensitive session tokens (sp_dc).
 * Uses AES-256 GCM with a machine-bound key derived from the user environment.
 */
object CredentialStore {
    private val dir = File(System.getProperty("user.home"), ".meld")
    private val encFile = File(dir, "spotify_session.enc")
    private val legacyPlainFile = File(dir, "spotify_session.txt")

    private val keySpec by lazy {
        val seed = "${System.getProperty("user.name")}:${System.getProperty("os.name")}:MeldDesktopSecureTokenKey"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        SecretKeySpec(digest, "AES")
    }

    fun saveSpDc(spDc: String) {
        try {
            dir.mkdirs()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(spDc.toByteArray(Charsets.UTF_8))

            // File format: 12 bytes IV + ciphertext
            encFile.writeBytes(iv + ciphertext)

            // Remove legacy plain text if present
            if (legacyPlainFile.exists()) {
                legacyPlainFile.delete()
            }
        } catch (e: Exception) {
            println("[CredentialStore] Failed to save encrypted sp_dc: ${e.message}")
        }
    }

    fun loadSpDc(): String? {
        // Automatic migration of old plain text cookie file
        if (legacyPlainFile.exists()) {
            val plain = legacyPlainFile.readText().trim()
            if (plain.isNotBlank()) {
                saveSpDc(plain)
                return plain
            }
        }

        if (!encFile.exists()) return null

        return try {
            val bytes = encFile.readBytes()
            if (bytes.size <= 12) return null
            val iv = bytes.copyOfRange(0, 12)
            val ciphertext = bytes.copyOfRange(12, bytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            println("[CredentialStore] Failed to decrypt sp_dc: ${e.message}")
            null
        }
    }

    fun clearSpDc() {
        if (encFile.exists()) encFile.delete()
        if (legacyPlainFile.exists()) legacyPlainFile.delete()
    }
}
