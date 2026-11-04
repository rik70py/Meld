package com.meld.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated
import com.metrolist.spotify.Spotify
import com.metrolist.spotify.models.SpotifySavedTrack
import com.metrolist.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LikedSongsView(
    spotifySession: SpotifySession,
    playerController: PlayerController,
    onOpenLoginDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    var likedTracks by remember { mutableStateOf<List<SpotifySavedTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            isLoading = true
            errorMsg = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    Spotify.likedSongs(limit = 50)
                }
                result.onSuccess { paging ->
                    likedTracks = paging.items
                }.onFailure { e ->
                    errorMsg = "Failed to load liked songs: ${e.message}"
                }
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        // Hero Header matching Metrolist Mobile Library Screen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MeldPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Liked Songs",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isLoggedIn) "${likedTracks.size} tracks from your Spotify library" else "Sync your Spotify liked songs",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoggedIn && likedTracks.isNotEmpty()) {
                val allTracks = remember(likedTracks) { likedTracks.map { it.track } }
                Button(
                    onClick = {
                        allTracks.firstOrNull()?.let { first ->
                            playerController.playSpotifyTrack(first, newQueue = allTracks)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        if (!isLoggedIn) {
            Surface(
                color = MeldSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connect Spotify to view your Liked Songs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign in to access your entire Spotify library and stream tracks seamlessly in high quality.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onOpenLoginDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Connect Spotify", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MeldPrimary)
            }
        } else if (errorMsg != null) {
            Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
        } else if (likedTracks.isEmpty()) {
            Text(
                "No liked songs found in your Spotify account.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val allTracks = remember(likedTracks) { likedTracks.map { it.track } }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(likedTracks) { savedTrack ->
                    val track = savedTrack.track
                    LikedTrackRow(
                        track = track,
                        onPlay = {
                            playerController.playSpotifyTrack(track, newQueue = allTracks)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedTrackRow(
    track: SpotifyTrack,
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
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val thumbUrl = track.album?.images?.firstOrNull()?.url
                if (!thumbUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbUrl,
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
                        text = track.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val artistAndAlbum = buildString {
                        append(track.artists.joinToString(", ") { it.name })
                        if (!track.album?.name.isNullOrBlank()) {
                            append(" • ")
                            append(track.album?.name)
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
