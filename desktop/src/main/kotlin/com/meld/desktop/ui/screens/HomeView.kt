package com.meld.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.meld.desktop.player.DesktopTrackMatcher
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated
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

    LaunchedEffect(selectedMoodIndex) {
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
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Header with greeting
        item {
            Column {
                Text(
                    text = if (isLoggedIn) "Good day, ${currentUser?.displayName ?: "Listener"}" else "Explore Music",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLoggedIn)
                        "Personalized recommendations powered by your Spotify taste."
                    else
                        "Streaming high-quality music without subscription requirements.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Mood & Genre Chips Row (Inspired by Mobile Metrolist ChipsRow)
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
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Spotify Integration Card (Inspired by Mobile IntegrationCard)
        item {
            if (!isLoggedIn) {
                Surface(
                    color = MeldSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MeldPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("S", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MeldPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect Spotify Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Link your Spotify account to sync Liked Songs, personalized playlists, and algorithmic daily mixes directly into Meld PC.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Button(
                            onClick = onOpenLoginDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            } else {
                Surface(
                    color = MeldSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spotify Taste Profile Active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Logged in as ${currentUser?.displayName ?: "User"}. Your library and recommendations are synchronized.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { spotifySession.logout() },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Quick Picks Horizontal Row (Inspired by Mobile Metrolist QuickPicks)
        if (recommendedSongs.isNotEmpty() && !isLoadingRecommendations) {
            item {
                Column {
                    Text(
                        text = "Quick Picks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        recommendedSongs.take(6).forEach { song ->
                            QuickPickCard(song = song) {
                                playerController.playSong(song)
                            }
                        }
                    }
                }
            }
        }

        // 5. Recommended Songs List Header
        item {
            Column {
                Text(
                    text = "${moodChips[selectedMoodIndex].first} Tracks",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "High bitrate Opus audio ready to stream instantly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6. Songs List (Styled like Mobile Metrolist SongListItem)
        if (isLoadingRecommendations) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MeldPrimary)
                }
            }
        } else if (recommendedSongs.isEmpty()) {
            item {
                Text(
                    text = "No songs found for this selection.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            items(recommendedSongs.drop(6).ifEmpty { recommendedSongs }) { song ->
                MetrolistSongListItem(song = song) {
                    playerController.playSong(song)
                }
            }
        }

        // 7. Audio Engine Features
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetrolistFeatureCard(
                    title = "Lossless & High Bitrate",
                    description = "Streams official audio from YouTube Music's high bitrate Opus catalog.",
                    modifier = Modifier.weight(1f)
                )
                MetrolistFeatureCard(
                    title = "Desktop Audio Engine",
                    description = "Powered by LibVLC native decoders for instant streaming without delay.",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Mobile-inspired Quick Pick Card with square artwork and overlay play icon.
 */
@Composable
private fun QuickPickCard(
    song: SongItem,
    onPlay: () -> Unit
) {
    Surface(
        color = MeldSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onPlay)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
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
                        Text("♪", fontSize = 28.sp, color = MeldPrimary)
                    }
                }

                // Floating circular play button on bottom right of artwork
                Surface(
                    color = MeldPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artists.joinToString(", ") { it.name },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * SongListItem matching Metrolist mobile's exact design.
 */
@Composable
private fun MetrolistSongListItem(
    song: SongItem,
    onPlay: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!song.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("♪", fontSize = 18.sp, color = MeldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = song.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val artistAndAlbum = buildString {
                        append(song.artists.joinToString(", ") { it.name })
                        if (!song.album?.name.isNullOrBlank()) {
                            append(" • ")
                            append(song.album?.name)
                        }
                        val dur = song.duration
                        if (dur != null && dur > 0) {
                            val mins = dur / 60
                            val secs = dur % 60
                            append(" • ")
                            append("%d:%02d".format(mins, secs))
                        }
                    }
                    Text(
                        text = artistAndAlbum,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MeldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetrolistFeatureCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MeldSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MeldPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
