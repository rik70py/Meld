package com.meld.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import com.meld.desktop.player.DesktopAudioPlayer
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.components.DesktopNavScreen
import com.meld.desktop.ui.components.LoginDialog
import com.meld.desktop.ui.components.PlayerBar
import com.meld.desktop.ui.components.Sidebar
import com.meld.desktop.ui.screens.HomeView
import com.meld.desktop.ui.screens.LikedSongsView
import com.meld.desktop.ui.screens.SearchView
import com.meld.desktop.ui.screens.SettingsView
import com.meld.desktop.ui.theme.MeldDesktopTheme
import com.meld.desktop.ui.theme.MeldPrimary
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI

fun main() {
    try {
        val logFile = java.io.File(System.getProperty("user.home"), ".meld/meld.log").apply { parentFile?.mkdirs() }
        val ps = java.io.PrintStream(java.io.FileOutputStream(logFile, true), true)
        System.setOut(ps)
        System.setErr(ps)
        println("\n=== Meld Desktop started at ${java.util.Date()} ===")
    } catch (e: Exception) {
        System.err.println("Failed to initialize log file: ${e.message}")
    }

    application {
    val windowState = rememberWindowState(width = 1240.dp, height = 820.dp)

    val scope = rememberCoroutineScope()
    val audioPlayer = remember { DesktopAudioPlayer(scope) }
    val playerController = remember { PlayerController(audioPlayer, scope) }
    val spotifySession = remember { SpotifySession(scope) }

    var currentScreen by remember { mutableStateOf(DesktopNavScreen.HOME) }
    var showLoginDialog by remember { mutableStateOf(false) }

    val isVlcAvailable by audioPlayer.isVlcAvailable.collectAsState()
    val isQueueOpen by playerController.isQueueDrawerOpen.collectAsState()
    val isInputFocused by playerController.isInputFocused.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Meld",
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                when {
                    // Global Ctrl+F always switches to search
                    event.isCtrlPressed && event.key == Key.F -> {
                        currentScreen = DesktopNavScreen.SEARCH
                        true
                    }
                    // Media controls only trigger when not actively typing in an input field
                    !isInputFocused -> {
                        when {
                            event.key == Key.Spacebar -> {
                                audioPlayer.togglePlayPause()
                                true
                            }
                            event.isCtrlPressed && event.key == Key.DirectionRight -> {
                                playerController.playNext()
                                true
                            }
                            event.isCtrlPressed && event.key == Key.DirectionLeft -> {
                                playerController.playPrevious()
                                true
                            }
                            event.key == Key.DirectionRight -> {
                                audioPlayer.seekBy(5000L) // seek +5s
                                true
                            }
                            event.key == Key.DirectionLeft -> {
                                audioPlayer.seekBy(-5000L) // seek -5s
                                true
                            }
                            event.key == Key.DirectionUp -> {
                                audioPlayer.adjustVolume(5) // volume +5%
                                true
                            }
                            event.key == Key.DirectionDown -> {
                                audioPlayer.adjustVolume(-5) // volume -5%
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        window.minimumSize = Dimension(960, 640)

        MeldDesktopTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Missing LibVLC Notification Header Banner
                    if (!isVlcAvailable) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "LibVLC (64-bit) not detected. Audio playback requires VLC to be installed on your system.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Button(
                                    onClick = {
                                        try {
                                            Desktop.getDesktop().browse(URI("https://www.videolan.org/vlc/"))
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text("Download VLC", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }

                    // Main Content Row (Sidebar + Screen + Queue Drawer)
                    Row(modifier = Modifier.weight(1f)) {
                        Sidebar(
                            currentScreen = currentScreen,
                            onNavigate = { currentScreen = it },
                            spotifySession = spotifySession,
                            onOpenLoginDialog = { showLoginDialog = true }
                        )

                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (currentScreen) {
                                DesktopNavScreen.HOME -> HomeView(
                                    spotifySession = spotifySession,
                                    playerController = playerController,
                                    onOpenLoginDialog = { showLoginDialog = true }
                                )
                                DesktopNavScreen.SEARCH -> SearchView(
                                    playerController = playerController
                                )
                                DesktopNavScreen.LIKED_SONGS -> LikedSongsView(
                                    spotifySession = spotifySession,
                                    playerController = playerController,
                                    onOpenLoginDialog = { showLoginDialog = true }
                                )
                                DesktopNavScreen.SETTINGS -> SettingsView(
                                    spotifySession = spotifySession,
                                    playerController = playerController,
                                    onOpenLoginDialog = { showLoginDialog = true }
                                )
                            }
                        }

                        // Slide-out Queue Panel on the Right
                        AnimatedVisibility(
                            visible = isQueueOpen,
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { it })
                        ) {
                            Row(modifier = Modifier.fillMaxHeight()) {
                                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                                QueueDrawer(playerController = playerController)
                            }
                        }
                    }

                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))

                    // Persistent Bottom Player Bar
                    PlayerBar(playerController = playerController)
                }

                // Global Toast / Snackbar Notifications
                SnackbarHost(
                    hostState = playerController.snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp),
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            actionColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                )

                if (showLoginDialog) {
                    LoginDialog(
                        spotifySession = spotifySession,
                        onInputFocusChanged = { playerController.isInputFocused.value = it },
                        onDismiss = { showLoginDialog = false }
                    )
                }
            }
        }
    }
}
}

/**
 * Slide-out Queue Drawer showing current track and upcoming queue tracks.
 */
@Composable
private fun QueueDrawer(playerController: PlayerController) {
    val currentTrack by playerController.currentTrack.collectAsState()
    val queue by playerController.queue.collectAsState()
    val queueIndex by playerController.queueIndex.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp).fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Play Queue (${queue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { playerController.toggleQueueDrawer() },
                    modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Now Playing
            if (currentTrack != null) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MeldPrimary.copy(alpha = 0.12f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thumb = currentTrack?.thumbnailUrl
                    if (!thumb.isNullOrBlank()) {
                        AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack?.artist ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Up Next
            Text(
                text = "UP NEXT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (queue.isEmpty() || (queueIndex + 1 >= queue.size)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty. Play songs or albums to add them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val nextItems = queue.drop(queueIndex + 1)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(nextItems) { idx, track ->
                        val actualIdx = queueIndex + 1 + idx
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerController.playTrackAt(actualIdx) }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { playerController.removeFromQueue(actualIdx) },
                                    modifier = Modifier.size(26.dp).pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
