package com.meld.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
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

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Meld PC - Spotify & YouTube Music Client"
    ) {
        val scope = rememberCoroutineScope()
        val audioPlayer = remember { DesktopAudioPlayer(scope) }
        val playerController = remember { PlayerController(audioPlayer, scope) }
        val spotifySession = remember { SpotifySession(scope) }

        var currentScreen by remember { mutableStateOf(DesktopNavScreen.HOME) }
        var showLoginDialog by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                audioPlayer.release()
            }
        }

        MeldDesktopTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Sidebar(
                            currentScreen = currentScreen,
                            onNavigate = { currentScreen = it },
                            spotifySession = spotifySession,
                            onOpenLoginDialog = { showLoginDialog = true }
                        )

                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))

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
                                    onOpenLoginDialog = { showLoginDialog = true }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline))

                    // Persistent Bottom Player Bar
                    PlayerBar(playerController = playerController)
                }

                if (showLoginDialog) {
                    LoginDialog(
                        spotifySession = spotifySession,
                        onDismiss = { showLoginDialog = false }
                    )
                }
            }
        }
    }
}
