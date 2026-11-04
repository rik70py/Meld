package com.meld.desktop.player

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class ResolvedStream(
    val videoId: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val durationSeconds: Int?
)

/**
 * Maps Spotify tracks to playable YouTube Music streams.
 * Uses fuzzy duration and title matching to find the best official audio match.
 */
object DesktopTrackMatcher {

    /**
     * Resolves a direct audio stream URL from a YouTube videoId using client configurations
     * that bypass web restrictions and botguard.
     */
    suspend fun fetchAudioStream(videoId: String): String? = withContext(Dispatchers.IO) {
        val clientOrder = listOf(
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.ANDROID_VR_1_65_10,
            YouTubeClient.VISIONOS
        )

        for (client in clientOrder) {
            val response = YouTube.player(videoId, client = client).getOrNull()
            val formats = response?.streamingData?.adaptiveFormats
                ?: response?.streamingData?.formats
                ?: emptyList()

            val audioFormat = formats
                .filter { it.mimeType.startsWith("audio/") && !it.url.isNullOrBlank() }
                .maxByOrNull { it.bitrate }

            if (audioFormat?.url != null) {
                println("[DesktopTrackMatcher] Resolved audio stream using client: $client (itag=${audioFormat.itag}, bitrate=${audioFormat.bitrate})")
                return@withContext audioFormat.url
            }
        }
        println("[DesktopTrackMatcher] Failed to resolve playable stream for videoId: $videoId")
        null
    }

    suspend fun resolveStream(track: SpotifyTrack): ResolvedStream? = withContext(Dispatchers.IO) {
        val artistName = track.artists.firstOrNull()?.name ?: ""
        val query = "$artistName - ${track.name}"
        println("[DesktopTrackMatcher] Searching YouTube for: $query")

        val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        val songs = searchResult?.items?.filterIsInstance<SongItem>() ?: emptyList()

        if (songs.isEmpty()) {
            println("[DesktopTrackMatcher] No songs found for $query")
            return@withContext null
        }

        val targetDurationSec = track.durationMs / 1000

        // Score candidates based on duration proximity and title similarity
        val bestMatch = songs.minByOrNull { song ->
            val durationDiff = song.duration?.let { abs(it - targetDurationSec) } ?: 100
            val titleMatches = if (song.title.contains(track.name, ignoreCase = true)) 0 else 50
            durationDiff + titleMatches
        } ?: songs.first()

        println("[DesktopTrackMatcher] Matched: ${bestMatch.title} (${bestMatch.id})")

        val streamUrl = fetchAudioStream(bestMatch.id) ?: return@withContext null

        ResolvedStream(
            videoId = bestMatch.id,
            title = bestMatch.title,
            artist = bestMatch.artists.firstOrNull()?.name ?: artistName,
            streamUrl = streamUrl,
            durationSeconds = bestMatch.duration ?: targetDurationSec
        )
    }

    suspend fun searchYouTubeDirect(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        result?.items?.filterIsInstance<SongItem>() ?: emptyList()
    }

    /**
     * Fetches recommended songs for initial app load and category filtering.
     * Uses YouTube Music home trending items with fallback to search queries.
     */
    suspend fun fetchRecommendedSongs(query: String = "Top Hits Today"): List<SongItem> = withContext(Dispatchers.IO) {
        if (query == "Top Hits Today") {
            val home = YouTube.home().getOrNull()
            val songsFromHome = home?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty()
            if (songsFromHome.isNotEmpty()) {
                return@withContext songsFromHome.distinctBy { it.id }
            }
        }

        val results = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        results?.items?.filterIsInstance<SongItem>()?.distinctBy { it.id } ?: emptyList()
    }
}
