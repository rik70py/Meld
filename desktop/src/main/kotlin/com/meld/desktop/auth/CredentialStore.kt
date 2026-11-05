package com.meld.desktop.auth

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure credential storage for sensitive session tokens (sp_dc).
 * Uses Windows DPAPI (CryptProtectData via JNA) on Windows, binding encryption
 * directly to the OS user account.
 * Uses AES-256 GCM as a fallback on other platforms.
 */
object CredentialStore {
    private val dir = File(System.getProperty("user.home"), ".meld")
    private val dpapiFile = File(dir, "spotify_session.dpapi")
    private val encFile = File(dir, "spotify_session.enc")
    private val legacyPlainFile = File(dir, "spotify_session.txt")

    fun saveSpDc(spDc: String) {
        try {
            dir.mkdirs()
            val plainBytes = spDc.toByteArray(Charsets.UTF_8)

            if (Platform.isWindows()) {
                // Windows DPAPI: Encrypted using the user's Windows login credentials
                val encrypted = Crypt32Util.cryptProtectData(plainBytes)
                dpapiFile.writeBytes(encrypted)
                if (encFile.exists()) encFile.delete()
            } else {
                saveAesGcm(plainBytes)
            }

            // Always purge legacy plaintext file
            if (legacyPlainFile.exists()) {
                legacyPlainFile.delete()
            }
        } catch (e: Throwable) {
            println("[CredentialStore] Failed to save encrypted token: ${e.message}")
            // Fallback to AES-GCM if DPAPI throws
            saveAesGcm(spDc.toByteArray(Charsets.UTF_8))
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

        // Try Windows DPAPI first
        if (Platform.isWindows() && dpapiFile.exists()) {
            try {
                val bytes = dpapiFile.readBytes()
                val decrypted = Crypt32Util.cryptUnprotectData(bytes)
                return String(decrypted, Charsets.UTF_8)
            } catch (e: Throwable) {
                println("[CredentialStore] DPAPI decrypt failed: ${e.message}")
            }
        }

        // Try AES-GCM fallback file
        if (encFile.exists()) {
            try {
                return loadAesGcm()
            } catch (e: Throwable) {
                println("[CredentialStore] AES-GCM fallback decrypt failed: ${e.message}")
            }
        }

        return null
    }

    fun clearSpDc() {
        if (dpapiFile.exists()) dpapiFile.delete()
        if (encFile.exists()) encFile.delete()
        if (legacyPlainFile.exists()) legacyPlainFile.delete()
    }

    private fun saveAesGcm(plainBytes: ByteArray) {
        try {
            val key = deriveFallbackKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(plainBytes)
            encFile.writeBytes(iv + ciphertext)
        } catch (_: Exception) {}
    }

    private fun loadAesGcm(): String? {
        val bytes = encFile.readBytes()
        if (bytes.size <= 12) return null
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveFallbackKey(), GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveFallbackKey(): SecretKeySpec {
        val seed = "${System.getProperty("user.home")}:${System.getProperty("user.name")}:MeldFallbackSecretKey"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }
}
