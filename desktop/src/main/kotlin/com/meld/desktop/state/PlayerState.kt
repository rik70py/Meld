package com.meld.desktop.state

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

data class ActiveTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L
)

class PlayerController(
    val audioPlayer: DesktopAudioPlayer,
    private val scope: CoroutineScope
) {
    private val _currentTrack = MutableStateFlow<ActiveTrack?>(null)
    val currentTrack: StateFlow<ActiveTrack?> = _currentTrack.asStateFlow()

    private val _isResolvingStream = MutableStateFlow(false)
    val isResolvingStream: StateFlow<Boolean> = _isResolvingStream.asStateFlow()

    private val queue = mutableListOf<SpotifyTrack>()
    private var queueIndex = -1

    fun playSpotifyTrack(track: SpotifyTrack, newQueue: List<SpotifyTrack>? = null) {
        if (newQueue != null) {
            queue.clear()
            queue.addAll(newQueue)
            queueIndex = queue.indexOfFirst { it.id == track.id }
        }

        _currentTrack.value = ActiveTrack(
            id = track.id,
            title = track.name,
            artist = track.artists.joinToString(", ") { it.name },
            albumName = track.album?.name,
            thumbnailUrl = track.album?.images?.firstOrNull()?.url,
            durationMs = track.durationMs.toLong()
        )

        scope.launch(Dispatchers.IO) {
            _isResolvingStream.value = true
            try {
                val resolved = DesktopTrackMatcher.resolveStream(track)
                if (resolved != null) {
                    println("[PlayerController] Streaming audio from: ${resolved.streamUrl}")
                    audioPlayer.play(resolved.streamUrl)
                } else {
                    println("[PlayerController] Could not resolve audio stream for track: ${track.name}")
                }
            } catch (e: Exception) {
                println("[PlayerController] Playback error: ${e.message}")
            } finally {
                _isResolvingStream.value = false
            }
        }
    }

    fun playSong(song: SongItem) {
        _currentTrack.value = ActiveTrack(
            id = song.id,
            title = song.title,
            artist = song.artists.joinToString(", ") { it.name },
            thumbnailUrl = song.thumbnail,
            durationMs = (song.duration ?: 0) * 1000L
        )

        scope.launch(Dispatchers.IO) {
            _isResolvingStream.value = true
            try {
                val streamUrl = DesktopTrackMatcher.fetchAudioStream(song.id)
                if (streamUrl != null) {
                    println("[PlayerController] Streaming song directly: ${song.title}")
                    audioPlayer.play(streamUrl)
                } else {
                    println("[PlayerController] Failed to resolve stream for song: ${song.title}")
                }
            } catch (e: Exception) {
                println("[PlayerController] Playback error: ${e.message}")
            } finally {
                _isResolvingStream.value = false
            }
        }
    }

    fun playDirect(title: String, artist: String, streamUrl: String, durationMs: Long) {
        _currentTrack.value = ActiveTrack(
            id = System.currentTimeMillis().toString(),
            title = title,
            artist = artist,
            durationMs = durationMs
        )
        audioPlayer.play(streamUrl)
    }

    fun playNext() {
        if (queue.isNotEmpty() && queueIndex + 1 < queue.size) {
            queueIndex++
            playSpotifyTrack(queue[queueIndex])
        }
    }

    fun playPrevious() {
        if (queue.isNotEmpty() && queueIndex - 1 >= 0) {
            queueIndex--
            playSpotifyTrack(queue[queueIndex])
        }
    }
}
