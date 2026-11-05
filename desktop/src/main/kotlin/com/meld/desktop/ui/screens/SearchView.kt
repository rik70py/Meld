package com.meld.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.meld.desktop.state.toActiveTrack
import com.meld.desktop.ui.components.formatMs
import com.meld.desktop.ui.theme.MeldPrimary
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchView(
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchResults by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filterChips = listOf("All", "Songs", "Artists", "Albums")

    // Debounced search with 300ms delay & automatic cancellation of previous job
    LaunchedEffect(searchQuery, selectedFilter) {
        val query = searchQuery.trim()
        if (query.length >= 2) {
            delay(300) // 300ms debounce
            isSearching = true
            try {
                val filter = when (selectedFilter) {
                    "Songs" -> YouTube.SearchFilter.FILTER_SONG
                    "Artists" -> YouTube.SearchFilter.FILTER_ARTIST
                    "Albums" -> YouTube.SearchFilter.FILTER_ALBUM
                    else -> null // All
                }
                val results = DesktopTrackMatcher.searchYouTubeDirect(query, filter)
                searchResults = results
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[SearchView] Search error: ${e.message}")
            } finally {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input Bar with focus tracking so global shortcuts are not intercepted
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Songs, artists, or albums", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotEmpty()) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            searchResults = emptyList()
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    playerController.isInputFocused.value = focusState.isFocused
                },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MeldPrimary,
                cursorColor = MeldPrimary,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips: All, Songs, Artists, Albums
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterChips.forEach { chip ->
                val isSelected = chip == selectedFilter
                Surface(
                    color = if (isSelected) MeldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { selectedFilter = chip }
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = chip,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MeldPrimary)
            }
        } else if (searchQuery.trim().length >= 2 && searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results found for \"$searchQuery\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (searchResults.isNotEmpty()) {
            val songsInResults = remember(searchResults) { searchResults.filterIsInstance<SongItem>() }
            val songsActiveQueue = remember(songsInResults) { songsInResults.map { it.toActiveTrack() } }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(searchResults) { index, item ->
                    when (item) {
                        is SongItem -> {
                            val songIdx = songsInResults.indexOf(item)
                            SearchResultSongRow(
                                index = index + 1,
                                song = item,
                                onPlay = {
                                    playerController.playQueue(songsActiveQueue, songIdx.coerceAtLeast(0))
                                },
                                onAddToQueue = {
                                    playerController.addToQueue(item.toActiveTrack())
                                }
                            )
                        }
                        is ArtistItem -> {
                            SearchResultArtistRow(
                                artist = item,
                                onPlay = {
                                    scope.launch {
                                        playerController.notify("Loading tracks for ${item.title}...")
                                        val artistSongs = DesktopTrackMatcher.fetchArtistTopSongs(item)
                                        if (artistSongs.isNotEmpty()) {
                                            playerController.playQueue(artistSongs.map { it.toActiveTrack() }, 0)
                                        } else {
                                            playerController.notify("No tracks found for artist.")
                                        }
                                    }
                                }
                            )
                        }
                        is AlbumItem -> {
                            SearchResultAlbumRow(
                                album = item,
                                onPlay = {
                                    scope.launch {
                                        playerController.notify("Loading tracks for ${item.title}...")
                                        val albumSongs = DesktopTrackMatcher.fetchAlbumSongs(item)
                                        if (albumSongs.isNotEmpty()) {
                                            playerController.playQueue(albumSongs.map { it.toActiveTrack() }, 0)
                                        } else {
                                            playerController.notify("No tracks found for album.")
                                        }
                                    }
                                }
                            )
                        }
                        else -> {}
                    }
                }
            }
        } else {
            // Initial Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search across millions of songs, artists, and albums",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultSongRow(
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
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MeldPrimary, modifier = Modifier.size(20.dp))
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
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(42.dp)
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

@Composable
private fun SearchResultArtistRow(
    artist: ArtistItem,
    onPlay: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

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
            if (!artist.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = artist.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Artist",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeldPrimary
                )
            }

            Surface(
                color = if (isHovered) MeldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play Artist",
                        tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultAlbumRow(
    album: AlbumItem,
    onPlay: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

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
            if (!album.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = album.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val artistText = album.artists?.joinToString(", ") { it.name }.orEmpty()
                val yearText = album.year?.toString()
                val subtitle = listOfNotNull(artistText.ifBlank { null }, yearText, "Album").joinToString(" • ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = if (isHovered) MeldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
