package com.meld.desktop.player

import com.meld.desktop.state.DesktopSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File

/**
 * High-performance audio player for Desktop using LibVLC / VLCJ.
 * Handles audio streaming from YouTube Music endpoints directly,
 * progress tracking, volume control, and play/pause events.
 */
class DesktopAudioPlayer(
    private val scope: CoroutineScope
) {
    private var mediaPlayerFactory: MediaPlayerFactory? = null
    private var mediaPlayer: MediaPlayer? = null
    private var progressPollingJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _isVlcAvailable = MutableStateFlow(false)
    val isVlcAvailable: StateFlow<Boolean> = _isVlcAvailable.asStateFlow()

    var onTrackFinished: (() -> Unit)? = null

    init {
        initVlc()
    }

    private fun useBundledVlc(): Boolean {
        val appResourcesDir = System.getProperty("compose.application.resources.dir")
        val candidateDirs = listOfNotNull(
            appResourcesDir?.let { File(it, "vlc") },
            File(System.getProperty("user.dir"), "desktop/resources/windows-x64/vlc"),
            File("resources/windows-x64/vlc")
        )

        for (dir in candidateDirs) {
            if (File(dir, "libvlc.dll").exists()) {
                val path = dir.absolutePath
                System.setProperty("jna.library.path", path)
                com.sun.jna.NativeLibrary.addSearchPath("libvlc", path)
                com.sun.jna.NativeLibrary.addSearchPath("libvlccore", path)
                println("[DesktopAudioPlayer] Using bundled LibVLC from: $path")
                return true
            }
        }
        return false
    }

    private fun initVlc() {
        try {
            val bundledFound = useBundledVlc()
            if (!bundledFound) {
                val vlcFound = uk.co.caprica.vlcj.factory.discovery.NativeDiscovery().discover()
                if (!vlcFound) {
                    val candidateDirs = listOf(
                        "C:\\Program Files\\VideoLAN\\VLC",
                        "C:\\Program Files (x86)\\VideoLAN\\VLC",
                        System.getenv("VLC_PATH") ?: ""
                    )
                    for (dir in candidateDirs) {
                        if (dir.isNotBlank() && File(dir, "libvlc.dll").exists()) {
                            System.setProperty("jna.library.path", dir)
                            System.setProperty("VLC_PLUGIN_PATH", "$dir\\plugins")
                            println("[DesktopAudioPlayer] Found VLC in fallback path: $dir")
                            break
                        }
                    }
                }
            }

            val options = mutableListOf("--no-video", "--intf=dummy")
            if (DesktopSettings.normalizeVolume.value) {
                options.add("--audio-filter=normvol")
            }

            val factory = MediaPlayerFactory(*options.toTypedArray())
            val player = factory.mediaPlayers().newMediaPlayer()

            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun playing(mp: MediaPlayer?) {
                    _isPlaying.value = true
                    startProgressPolling()
                }

                override fun paused(mp: MediaPlayer?) {
                    _isPlaying.value = false
                    stopProgressPolling()
                }

                override fun stopped(mp: MediaPlayer?) {
                    _isPlaying.value = false
                    _positionMs.value = 0L
                    stopProgressPolling()
                }

                override fun finished(mp: MediaPlayer?) {
                    _isPlaying.value = false
                    _positionMs.value = 0L
                    stopProgressPolling()
                    println("[DesktopAudioPlayer] Track finished naturally")
                    scope.launch(Dispatchers.Main) {
                        onTrackFinished?.invoke()
                    }
                }

                override fun lengthChanged(mp: MediaPlayer?, newLength: Long) {
                    if (newLength > 0) {
                        _durationMs.value = newLength
                    }
                }

                override fun error(mp: MediaPlayer?) {
                    _isPlaying.value = false
                    stopProgressPolling()
                    println("[DesktopAudioPlayer] VLC state: ERROR")
                }
            })

            mediaPlayerFactory = factory
            mediaPlayer = player
            _isVlcAvailable.value = true
            println("[DesktopAudioPlayer] LibVLC initialized successfully")
        } catch (e: Throwable) {
            println("[DesktopAudioPlayer] VLC initialization failed: ${e.message}")
            _isVlcAvailable.value = false
            mediaPlayer = null
            mediaPlayerFactory = null
        }
    }

    fun play(streamUrl: String) {
        val player = mediaPlayer
        if (player != null) {
            player.media().play(streamUrl)
            _isPlaying.value = true
        } else {
            println("[DesktopAudioPlayer] Cannot play stream: LibVLC is not available")
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.status().isPlaying) {
            player.controls().pause()
        } else {
            player.controls().play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer
        if (player != null) {
            player.controls().setTime(positionMs)
        }
        _positionMs.value = positionMs
    }

    fun seekBy(deltaMs: Long) {
        val maxDuration = _durationMs.value.coerceAtLeast(0L)
        val target = (_positionMs.value + deltaMs).coerceIn(0L, if (maxDuration > 0) maxDuration else Long.MAX_VALUE)
        seekTo(target)
    }

    fun setVolume(volumeLevel: Int) {
        val clamped = volumeLevel.coerceIn(0, 100)
        mediaPlayer?.audio()?.setVolume(clamped)
        _volume.value = clamped
    }

    fun adjustVolume(delta: Int) {
        setVolume(_volume.value + delta)
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    val time = mp.status().time()
                    if (time >= 0) {
                        _positionMs.value = time
                    }
                    val length = mp.status().length()
                    if (length > 0) {
                        _durationMs.value = length
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    fun release() {
        stopProgressPolling()
        mediaPlayer?.release()
        mediaPlayerFactory?.release()
    }
}
