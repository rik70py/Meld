package com.meld.desktop.state

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object DesktopSettings {
    private val file = File(System.getProperty("user.home"), ".meld/desktop_settings.properties")
    private val props = Properties()

    val pureBlack = MutableStateFlow(true)
    val autoPlayNext = MutableStateFlow(true)
    val audioQuality = MutableStateFlow("High (Opus 251 - 136 kbps)")
    val normalizeVolume = MutableStateFlow(true)

    init {
        load()
    }

    private fun load() {
        if (file.exists()) {
            try {
                FileInputStream(file).use { props.load(it) }
                pureBlack.value = props.getProperty("pureBlack", "true").toBoolean()
                autoPlayNext.value = props.getProperty("autoPlayNext", "true").toBoolean()
                audioQuality.value = props.getProperty("audioQuality", "High (Opus 251 - 136 kbps)")
                normalizeVolume.value = props.getProperty("normalizeVolume", "true").toBoolean()
            } catch (e: Exception) {
                println("[DesktopSettings] Failed to load settings: ${e.message}")
            }
        }
    }

    fun save() {
        try {
            file.parentFile?.mkdirs()
            props.setProperty("pureBlack", pureBlack.value.toString())
            props.setProperty("autoPlayNext", autoPlayNext.value.toString())
            props.setProperty("audioQuality", audioQuality.value)
            props.setProperty("normalizeVolume", normalizeVolume.value.toString())
            FileOutputStream(file).use { props.store(it, "Meld Desktop Settings") }
        } catch (e: Exception) {
            println("[DesktopSettings] Failed to save settings: ${e.message}")
        }
    }

    fun clearCache(): Long {
        val temp = File(System.getProperty("java.io.tmpdir"))
        var freed = 0L
        temp.listFiles { f -> f.name.startsWith("meld_") }?.forEach {
            freed += it.length()
            it.deleteRecursively()
        }
        return freed
    }
}
