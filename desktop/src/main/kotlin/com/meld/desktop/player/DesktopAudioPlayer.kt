package com.meld.desktop.player

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

    private val _isVlcAvailable = MutableStateFlow(true)
    val isVlcAvailable: StateFlow<Boolean> = _isVlcAvailable.asStateFlow()

    init {
        try {
            val vlcFound = uk.co.caprica.vlcj.factory.discovery.NativeDiscovery().discover()
            println("[DesktopAudioPlayer] VLC NativeDiscovery: $vlcFound")
            if (!vlcFound) {
                val vlcDir = "C:\\Program Files\\VideoLAN\\VLC"
                if (java.io.File(vlcDir, "libvlc.dll").exists()) {
                    System.setProperty("jna.library.path", vlcDir)
                    System.setProperty("VLC_PLUGIN_PATH", "$vlcDir\\plugins")
                    println("[DesktopAudioPlayer] Set fallback VLC path to $vlcDir")
                }
            }
            mediaPlayerFactory = MediaPlayerFactory("--no-video", "--intf=dummy")
            mediaPlayer = mediaPlayerFactory?.mediaPlayers()?.newMediaPlayer()?.apply {
                events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                    override fun playing(mp: MediaPlayer?) {
                        _isPlaying.value = true
                        startProgressPolling()
                        println("[DesktopAudioPlayer] VLC state: PLAYING")
                    }

                    override fun paused(mp: MediaPlayer?) {
                        _isPlaying.value = false
                        stopProgressPolling()
                        println("[DesktopAudioPlayer] VLC state: PAUSED")
                    }

                    override fun stopped(mp: MediaPlayer?) {
                        _isPlaying.value = false
                        _positionMs.value = 0L
                        stopProgressPolling()
                        println("[DesktopAudioPlayer] VLC state: STOPPED")
                    }

                    override fun finished(mp: MediaPlayer?) {
                        _isPlaying.value = false
                        _positionMs.value = 0L
                        stopProgressPolling()
                        println("[DesktopAudioPlayer] VLC state: FINISHED")
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
            }
            println("[DesktopAudioPlayer] MediaPlayer initialized successfully: $mediaPlayer")
        } catch (e: Throwable) {
            println("[DesktopAudioPlayer] VLC initialization error: ${e.message}")
            _isVlcAvailable.value = false
        }
    }

    fun play(streamUrl: String) {
        val player = mediaPlayer
        if (player != null) {
            println("[DesktopAudioPlayer] Playing stream: ${streamUrl.take(70)}...")
            player.media().play(streamUrl)
            _isPlaying.value = true
        } else {
            println("[DesktopAudioPlayer] MediaPlayer is null, falling back to simulated mode")
            simulatePlayback()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.status().isPlaying) {
                player.controls().pause()
            } else {
                player.controls().play()
            }
        } else {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer
        if (player != null) {
            player.controls().setTime(positionMs)
        }
        _positionMs.value = positionMs
    }

    fun setVolume(volumeLevel: Int) {
        val clamped = volumeLevel.coerceIn(0, 100)
        mediaPlayer?.audio()?.setVolume(clamped)
        _volume.value = clamped
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

    private fun simulatePlayback() {
        _isPlaying.value = true
        startProgressPolling()
    }

    fun release() {
        stopProgressPolling()
        mediaPlayer?.release()
        mediaPlayerFactory?.release()
    }
}
