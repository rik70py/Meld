package com.meld.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.launch

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
    var isLoadingRecommendations by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedMoodIndex, isLoggedIn) {
        scope.launch {
            isLoadingRecommendations = true
            try {
                val query = moodChips[selectedMoodIndex].second
                recommendedSongs = DesktopTrackMatcher.fetchRecommendedSongs(query)
            } catch (e: Exception) {
                println("[HomeView] Error fetching recommendations: ${e.message}")
            } finally {
                isLoadingRecommendations = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header with greeting
        item {
            Column {
                Text(
                    text = if (isLoggedIn) "Welcome back, ${currentUser?.displayName ?: "Listener"}" else "Explore Music",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLoggedIn)
                        "Curated recommendations based on top hits and your listening preferences."
                    else
                        "Stream high quality music instantly from top charts and artists worldwide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Mood & Genre Chips Row
        item {
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

        // 3. Quick Picks Grid (Desktop 4-column adaptive layout)
        if (recommendedSongs.isNotEmpty() && !isLoadingRecommendations) {
            item {
                Column {
                    Text(
                        text = "Quick Picks",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val quickPicks = recommendedSongs.take(8)
                    val rows = quickPicks.chunked(4)
                    val allActive = remember(recommendedSongs) { recommendedSongs.map { it.toActiveTrack() } }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (rowItems in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                for (song in rowItems) {
                                    val songIndex = recommendedSongs.indexOf(song)
                                    Box(modifier = Modifier.weight(1f)) {
                                        DesktopQuickPickCard(song = song) {
                                            playerController.playQueue(allActive, songIndex)
                                        }
                                    }
                                }
                                // Fill remaining slots in incomplete row
                                for (i in 0 until (4 - rowItems.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Desktop Table Song List Header
        item {
            Column {
                Text(
                    text = "${moodChips[selectedMoodIndex].first} Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Table Column Titles Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = "TITLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "ARTIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            }
        }

        // 5. Songs Table Rows
        if (isLoadingRecommendations) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MeldPrimary)
                }
            }
        } else if (recommendedSongs.isEmpty()) {
            item {
                Text(
                    text = "No songs found for this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val listTracks = recommendedSongs.drop(8).ifEmpty { recommendedSongs }
            val activeQueue = listTracks.map { it.toActiveTrack() }

            itemsIndexed(listTracks) { index, song ->
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
        color = if (isHovered) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
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

                // Hover play overlay
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
            // Index number or Play icon on hover
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

            // Thumbnail + Title
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

            // Artist
            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp)
            )

            // Add to Queue button
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
