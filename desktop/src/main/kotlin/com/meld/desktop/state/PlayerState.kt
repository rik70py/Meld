package com.meld.desktop.state

import androidx.compose.material3.SnackbarHostState
import com.meld.desktop.player.DesktopAudioPlayer
import com.meld.desktop.player.DesktopTrackMatcher
import com.metrolist.innertube.models.SongItem
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class ActiveTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val originalSpotifyTrack: SpotifyTrack? = null,
    val originalSongItem: SongItem? = null
)

fun SpotifyTrack.toActiveTrack(): ActiveTrack = ActiveTrack(
    id = this.id,
    title = this.name,
    artist = this.artists.joinToString(", ") { it.name },
    albumName = this.album?.name,
    thumbnailUrl = this.album?.images?.firstOrNull()?.url,
    durationMs = this.durationMs.toLong(),
    originalSpotifyTrack = this
)

fun SongItem.toActiveTrack(): ActiveTrack = ActiveTrack(
    id = this.id,
    title = this.title,
    artist = this.artists.joinToString(", ") { it.name },
    albumName = null,
    thumbnailUrl = this.thumbnail,
    durationMs = (this.duration ?: 0) * 1000L,
    originalSongItem = this
)

class PlayerController(
    val audioPlayer: DesktopAudioPlayer,
    private val scope: CoroutineScope
) {
    private val _currentTrack = MutableStateFlow<ActiveTrack?>(null)
    val currentTrack: StateFlow<ActiveTrack?> = _currentTrack.asStateFlow()

    private val _queue = MutableStateFlow<List<ActiveTrack>>(emptyList())
    val queue: StateFlow<List<ActiveTrack>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isResolvingStream = MutableStateFlow(false)
    val isResolvingStream: StateFlow<Boolean> = _isResolvingStream.asStateFlow()

    private val _isQueueDrawerOpen = MutableStateFlow(false)
    val isQueueDrawerOpen: StateFlow<Boolean> = _isQueueDrawerOpen.asStateFlow()

    val snackbarHostState = SnackbarHostState()

    init {
        audioPlayer.onTrackFinished = {
            handleTrackFinished()
        }
    }

    private fun handleTrackFinished() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { playTrackInternal(it) }
            }
            RepeatMode.ALL -> {
                if (_queue.value.isNotEmpty()) {
                    val nextIdx = (_queueIndex.value + 1) % _queue.value.size
                    playTrackAt(nextIdx)
                }
            }
            RepeatMode.OFF -> {
                if (DesktopSettings.autoPlayNext.value) {
                    playNext()
                }
            }
        }
    }

    fun playQueue(tracks: List<ActiveTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val effectiveQueue = if (_isShuffle.value) {
            val startTrack = tracks.getOrElse(startIndex) { tracks.first() }
            listOf(startTrack) + (tracks.filter { it.id != startTrack.id }.shuffled())
        } else {
            tracks
        }
        _queue.value = effectiveQueue
        val actualStart = if (_isShuffle.value) 0 else startIndex.coerceIn(0, effectiveQueue.size - 1)
        _queueIndex.value = actualStart
        playTrackInternal(effectiveQueue[actualStart])
    }

    fun playTrackAt(index: Int) {
        val q = _queue.value
        if (index in q.indices) {
            _queueIndex.value = index
            playTrackInternal(q[index])
        }
    }

    fun playTrack(track: ActiveTrack, newQueue: List<ActiveTrack>? = null) {
        if (newQueue != null) {
            _queue.value = newQueue
            _queueIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = _queue.value + track
            _queueIndex.value = _queue.value.lastIndex
        }
        playTrackInternal(track)
    }

    fun playSpotifyTrack(track: SpotifyTrack, newQueue: List<SpotifyTrack>? = null) {
        val active = track.toActiveTrack()
        val q = newQueue?.map { it.toActiveTrack() }
        playTrack(active, q)
    }

    fun playSong(song: SongItem, newQueue: List<SongItem>? = null) {
        val active = song.toActiveTrack()
        val q = newQueue?.map { it.toActiveTrack() }
        playTrack(active, q)
    }

    fun addToQueue(track: ActiveTrack) {
        _queue.value = _queue.value + track
        notify("Added to queue: ${track.title}")
    }

    fun playNextInQueue(track: ActiveTrack) {
        val currentIdx = _queueIndex.value
        val list = _queue.value.toMutableList()
        val insertAt = (currentIdx + 1).coerceAtMost(list.size)
        list.add(insertAt, track)
        _queue.value = list
        notify("Will play next: ${track.title}")
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _queue.value = list
            if (index < _queueIndex.value) {
                _queueIndex.value = (_queueIndex.value - 1).coerceAtLeast(0)
            }
        }
    }

    fun playNext() {
        val q = _queue.value
        val nextIdx = _queueIndex.value + 1
        if (nextIdx in q.indices) {
            _queueIndex.value = nextIdx
            playTrackInternal(q[nextIdx])
        } else if (_repeatMode.value == RepeatMode.ALL && q.isNotEmpty()) {
            _queueIndex.value = 0
            playTrackInternal(q[0])
        }
    }

    fun playPrevious() {
        // If track played > 3 seconds, restart it; otherwise go to previous
        if (audioPlayer.positionMs.value > 3000L) {
            audioPlayer.seekTo(0L)
            return
        }
        val q = _queue.value
        val prevIdx = _queueIndex.value - 1
        if (prevIdx in q.indices) {
            _queueIndex.value = prevIdx
            playTrackInternal(q[prevIdx])
        } else {
            audioPlayer.seekTo(0L)
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        val current = _isShuffle.value
        _isShuffle.value = !current
        if (!current && _queue.value.isNotEmpty()) {
            // Shuffle remaining queue items
            val curTrack = _currentTrack.value
            val remaining = _queue.value.filter { it.id != curTrack?.id }.shuffled()
            _queue.value = (if (curTrack != null) listOf(curTrack) else emptyList()) + remaining
            _queueIndex.value = 0
        }
    }

    fun toggleQueueDrawer() {
        _isQueueDrawerOpen.value = !_isQueueDrawerOpen.value
    }

    private fun playTrackInternal(track: ActiveTrack) {
        _currentTrack.value = track

        scope.launch(Dispatchers.IO) {
            _isResolvingStream.value = true
            try {
                val streamUrl = when {
                    track.originalSongItem != null -> {
                        DesktopTrackMatcher.fetchAudioStream(track.originalSongItem.id)
                    }
                    track.originalSpotifyTrack != null -> {
                        DesktopTrackMatcher.resolveStream(track.originalSpotifyTrack)?.streamUrl
                    }
                    else -> {
                        // Attempt fallback resolution by searching title + artist
                        DesktopTrackMatcher.fetchAudioStream(track.id)
                    }
                }

                if (streamUrl != null) {
                    audioPlayer.play(streamUrl)
                } else {
                    notify("Failed to resolve audio stream for: ${track.title}")
                    if (DesktopSettings.autoPlayNext.value) {
                        playNext()
                    }
                }
            } catch (e: Exception) {
                notify("Playback error: ${e.message}")
            } finally {
                _isResolvingStream.value = false
            }
        }
    }

    fun notify(message: String) {
        scope.launch {
            try {
                snackbarHostState.showSnackbar(message)
            } catch (_: Exception) {}
        }
    }
}
