package com.meld.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.meld.desktop.player.DesktopTrackMatcher
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.state.toActiveTrack
import com.meld.desktop.ui.components.formatMs
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated
import com.metrolist.innertube.models.SongItem
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.CancellationException

@Composable
fun HomeView(
    spotifySession: SpotifySession,
    playerController: PlayerController,
    onOpenLoginDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    val currentUser by spotifySession.currentUser.collectAsState()

    val moodChips = listOf(
        "Trending" to "Top Hits Today",
        "Relax" to "Relaxing Music",
        "Workout" to "Workout Music Hits",
        "Focus" to "Focus Beats Study Music",
        "Energize" to "High Energy Hits"
    )
    var selectedMoodIndex by remember { mutableIntStateOf(0) }

    var recommendedSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var spotifyPersonalizedTracks by remember { mutableStateOf<List<SpotifyTrack>>(emptyList()) }
    var isLoadingRecommendations by remember { mutableStateOf(true) }

    // Direct LaunchedEffect without inner unmanaged scope.launch
    // Automatically cancels previous network call when switching mood chip
    LaunchedEffect(selectedMoodIndex) {
        isLoadingRecommendations = true
        try {
            val query = moodChips[selectedMoodIndex].second
            val songs = DesktopTrackMatcher.fetchRecommendedSongs(query)
            recommendedSongs = songs
        } catch (_: CancellationException) {
            // Rapid chip change, ignore cancelled job
        } catch (e: Exception) {
            println("[HomeView] Error fetching recommendations: ${e.message}")
        } finally {
            isLoadingRecommendations = false
        }
    }

    // Load real Spotify personalized tracks when user is authenticated
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val userTracks = DesktopTrackMatcher.fetchSpotifyRecommendations()
                spotifyPersonalizedTracks = userTracks
            } catch (e: Exception) {
                println("[HomeView] Error fetching Spotify recommendations: ${e.message}")
            }
        } else {
            spotifyPersonalizedTracks = emptyList()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with greeting (full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = if (isLoggedIn) "Welcome back, ${currentUser?.displayName ?: "Listener"}" else "Explore Music",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLoggedIn)
                        "Your Spotify library is synced. Browse your personal favorites below or discover trending music."
                    else
                        "Stream high quality music instantly from top charts and curated artists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Mood & Genre Chips Row (full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodChips.forEachIndexed { index, (label, _) ->
                    val isSelected = index == selectedMoodIndex
                    Surface(
                        color = if (isSelected) MeldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { selectedMoodIndex = index }
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Spotify Personalized Tracks Section (if logged in and tracks available)
        if (isLoggedIn && spotifyPersonalizedTracks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Your Spotify Favorites",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Quick access to your saved tracks from Spotify",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val spotifyActiveQueue = spotifyPersonalizedTracks.map { it.toActiveTrack() }
            items(spotifyPersonalizedTracks.take(8)) { spotifyTrack ->
                val trackIndex = spotifyPersonalizedTracks.indexOf(spotifyTrack)
                DesktopSpotifyCard(track = spotifyTrack) {
                    playerController.playQueue(spotifyActiveQueue, trackIndex)
                }
            }
        }

        // 4. Quick Picks Adaptive Grid (individual 180dp cells)
        if (recommendedSongs.isNotEmpty() && !isLoadingRecommendations) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Quick Picks",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val quickPicks = recommendedSongs.take(8)
            val allActive = recommendedSongs.map { it.toActiveTrack() }

            items(quickPicks) { song ->
                val songIndex = recommendedSongs.indexOf(song)
                DesktopQuickPickCard(song = song) {
                    playerController.playQueue(allActive, songIndex)
                }
            }
        }

        // 5. Song List Table Section Header (full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "${moodChips[selectedMoodIndex].first} Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Table column header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
                    Text("TITLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                    Text("ARTIST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("TIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            }
        }

        // 6. Song Table Rows (each spans maxLineSpan)
        if (isLoadingRecommendations) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MeldPrimary)
                }
            }
        } else if (recommendedSongs.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "No songs found for this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val listTracks = recommendedSongs.drop(8).ifEmpty { recommendedSongs }
            val activeQueue = listTracks.map { it.toActiveTrack() }

            itemsIndexed(listTracks, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, song ->
                DesktopTableRow(
                    index = index + 1,
                    song = song,
                    onPlay = {
                        playerController.playQueue(activeQueue, index)
                    },
                    onAddToQueue = {
                        playerController.addToQueue(song.toActiveTrack())
                    }
                )
            }
        }
    }
}

/**
 * Spotify Track Card for the Home page with hover elevation and play overlay.
 */
@Composable
private fun DesktopSpotifyCard(
    track: SpotifyTrack,
    onPlay: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        color = if (isHovered) MaterialTheme.colorScheme.surfaceVariant else MeldSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onPlay)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val thumb = track.album?.images?.firstOrNull()?.url
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(36.dp))
                    }
                }

                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MeldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Desktop Quick Pick Card with hover elevation, play overlay, and pointer cursor.
 */
@Composable
private fun DesktopQuickPickCard(
    song: SongItem,
    onPlay: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        color = if (isHovered) MaterialTheme.colorScheme.surfaceVariant else MeldSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onPlay)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (!song.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(36.dp))
                    }
                }

                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MeldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Desktop Table-style song row with hover highlight, play button on hover, and queue action.
 */
@Composable
private fun DesktopTableRow(
    index: Int,
    song: SongItem,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val dur = song.duration
    val durationText = if (dur != null && dur > 0) formatMs(dur * 1000L) else "--:--"

    Surface(
        color = if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onPlay)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isHovered) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MeldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!song.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp)
            )

            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = "Add to Queue",
                    tint = if (isHovered) MeldPrimary else Color.Transparent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
