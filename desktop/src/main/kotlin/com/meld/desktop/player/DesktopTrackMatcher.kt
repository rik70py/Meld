package com.meld.desktop.player

import com.meld.desktop.state.DesktopSettings
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.spotify.Spotify
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
            "ANDROID_VR_NO_AUTH" to YouTubeClient.ANDROID_VR_NO_AUTH,
            "ANDROID_VR_1_65_10" to YouTubeClient.ANDROID_VR_1_65_10,
            "VISIONOS" to YouTubeClient.VISIONOS
        )

        val preferStandard = DesktopSettings.audioQuality.value.startsWith("Standard")

        for ((name, client) in clientOrder) {
            val result = YouTube.player(videoId, client = client)
            val response = result.getOrNull()
            val formats = response?.streamingData?.adaptiveFormats
                ?: response?.streamingData?.formats
                ?: emptyList()

            val validAudio = formats.filter { it.mimeType.startsWith("audio/") && !it.url.isNullOrBlank() }

            println("[DesktopTrackMatcher] $videoId via $name: status=${response?.playabilityStatus?.status} " +
                "reason=${response?.playabilityStatus?.reason} audioWithUrl=${validAudio.size} " +
                "error=${result.exceptionOrNull()?.message}")

            val chosenFormat = if (preferStandard) {
                validAudio.find { it.mimeType.contains("mp4") || it.itag == 140 }
                    ?: validAudio.minByOrNull { it.bitrate }
            } else {
                validAudio.find { it.itag == 251 }
                    ?: validAudio.maxByOrNull { it.bitrate }
            }

            if (chosenFormat?.url != null) {
                println("[DesktopTrackMatcher] Resolved audio for videoId=$videoId (itag=${chosenFormat.itag}, bitrate=${chosenFormat.bitrate})")
                return@withContext chosenFormat.url
            }
        }
        println("[DesktopTrackMatcher] Failed to resolve playable stream for videoId: $videoId")
        null
    }

    suspend fun resolveStream(track: SpotifyTrack): ResolvedStream? = withContext(Dispatchers.IO) {
        val artistName = track.artists.firstOrNull()?.name ?: ""
        val query = "$artistName - ${track.name}"

        val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        val songs = searchResult?.items?.filterIsInstance<SongItem>() ?: emptyList()

        if (songs.isEmpty()) {
            return@withContext null
        }

        val targetDurationSec = track.durationMs / 1000

        // Score candidates based on duration proximity and title similarity
        val sortedCandidates = songs.sortedBy { song ->
            val durationDiff = song.duration?.let { abs(it - targetDurationSec) } ?: 100
            val titleMatches = if (song.title.contains(track.name, ignoreCase = true)) 0 else 50
            durationDiff + titleMatches
        }.take(3)

        for (candidate in sortedCandidates) {
            val streamUrl = fetchAudioStream(candidate.id)
            if (streamUrl != null) {
                return@withContext ResolvedStream(
                    videoId = candidate.id,
                    title = candidate.title,
                    artist = candidate.artists.firstOrNull()?.name ?: artistName,
                    streamUrl = streamUrl,
                    durationSeconds = candidate.duration ?: targetDurationSec
                )
            }
        }

        println("[DesktopTrackMatcher] All ${sortedCandidates.size} candidates failed stream resolution for track: ${track.name}")
        null
    }

    /**
     * Searches YouTube Music across categories (mixed results when filter is null, or category specific).
     */
    suspend fun searchYouTubeDirect(query: String, filter: YouTube.SearchFilter? = null): List<YTItem> = withContext(Dispatchers.IO) {
        if (filter != null) {
            val result = YouTube.search(query, filter).getOrNull()
            result?.items ?: emptyList()
        } else {
            // When filter is null ("All"), search summary retrieves songs, artists, and albums
            val summary = YouTube.searchSummary(query).getOrNull()
            val allItems = summary?.summaries?.flatMap { it.items }
            if (!allItems.isNullOrEmpty()) {
                allItems
            } else {
                val fallback = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                fallback?.items ?: emptyList()
            }
        }
    }

    /**
     * Resolves playable songs for an album or artist item.
     */
    suspend fun fetchAlbumSongs(album: AlbumItem): List<SongItem> = withContext(Dispatchers.IO) {
        val page = YouTube.album(album.browseId).getOrNull()
        page?.songs ?: emptyList()
    }

    suspend fun fetchArtistTopSongs(artist: ArtistItem): List<SongItem> = withContext(Dispatchers.IO) {
        val page = YouTube.artist(artist.id).getOrNull()
        val songs = page?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty()
        if (songs.isNotEmpty()) songs else searchYouTubeDirect(artist.title, YouTube.SearchFilter.FILTER_SONG).filterIsInstance<SongItem>()
    }

    /**
     * Fetches personalized or trending recommendations.
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

    /**
     * Fetches real Spotify tracks from user library to power personalized recommendations.
     */
    suspend fun fetchSpotifyRecommendations(): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        if (Spotify.accessToken == null) return@withContext emptyList()
        val liked = Spotify.likedSongs(limit = 20).getOrNull()?.items?.map { it.track }.orEmpty()
        liked
    }
}
