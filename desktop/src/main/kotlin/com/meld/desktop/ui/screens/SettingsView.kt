package com.meld.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meld.desktop.state.DesktopSettings
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import java.awt.Desktop
import java.net.URI

@Composable
fun SettingsView(
    spotifySession: SpotifySession,
    playerController: PlayerController,
    onOpenLoginDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    val currentUser by spotifySession.currentUser.collectAsState()
    val isVlcAvailable by playerController.audioPlayer.isVlcAvailable.collectAsState()

    var themeMode by remember { mutableStateOf(DesktopSettings.themeMode.value) }
    var pureBlack by remember { mutableStateOf(DesktopSettings.pureBlack.value) }
    var autoPlayNext by remember { mutableStateOf(DesktopSettings.autoPlayNext.value) }
    var normalizeVolume by remember { mutableStateOf(DesktopSettings.normalizeVolume.value) }
    var audioQuality by remember { mutableStateOf(DesktopSettings.audioQuality.value) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure audio streaming, appearance, and audio engine preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Appearance & Theme
        item {
            SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
                // Theme Mode: Dark, Light, System
                SettingsActionRow(
                    title = "Application Theme",
                    subtitle = "Select between Dark, Light, or System default appearance"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Dark", "Light", "System").forEach { mode ->
                            val isSelected = themeMode == mode
                            Surface(
                                color = if (isSelected) MeldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clickable {
                                        themeMode = mode
                                        DesktopSettings.themeMode.value = mode
                                        DesktopSettings.save()
                                    }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                // AMOLED Pure Black
                SettingsToggleRow(
                    title = "AMOLED Pure Black",
                    subtitle = "Use pure black #000000 background for maximum contrast in dark mode",
                    checked = pureBlack,
                    onCheckedChange = {
                        pureBlack = it
                        DesktopSettings.pureBlack.value = it
                        DesktopSettings.save()
                    }
                )
            }
        }

        // Section 2: Player & Audio Engine
        item {
            SettingsCard(title = "Player & Audio Engine", icon = Icons.Default.VolumeUp) {
                SettingsActionRow(
                    title = "Streaming Quality",
                    subtitle = audioQuality
                ) {
                    OutlinedButton(
                        onClick = {
                            val newQuality = if (audioQuality.startsWith("High")) {
                                "Standard (AAC - 128 kbps)"
                            } else {
                                "High (Opus 251 - 136 kbps)"
                            }
                            audioQuality = newQuality
                            DesktopSettings.audioQuality.value = newQuality
                            DesktopSettings.save()
                            playerController.notify("Audio streaming set to: $newQuality")
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Switch", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                SettingsToggleRow(
                    title = "Auto-play Next Track",
                    subtitle = "Automatically queue and advance to the next song when playback finishes",
                    checked = autoPlayNext,
                    onCheckedChange = {
                        autoPlayNext = it
                        DesktopSettings.autoPlayNext.value = it
                        DesktopSettings.save()
                    }
                )

                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                SettingsToggleRow(
                    title = "Volume Normalization",
                    subtitle = "Balance loudness levels across different audio streams (requires app restart)",
                    checked = normalizeVolume,
                    onCheckedChange = {
                        normalizeVolume = it
                        DesktopSettings.normalizeVolume.value = it
                        DesktopSettings.save()
                    }
                )

                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                // Audio Backend & VLC Status
                SettingsActionRow(
                    title = "Audio Engine Backend",
                    subtitle = if (isVlcAvailable)
                        "LibVLC 64-bit Direct Audio Output with Hardware Decoders"
                    else
                        "LibVLC not detected. VLC 64-bit must be installed for audio playback."
                ) {
                    if (isVlcAvailable) {
                        Surface(
                            color = MeldPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = MeldPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                try {
                                    Desktop.getDesktop().browse(URI("https://www.videolan.org/vlc/"))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("Download VLC", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                }
            }
        }

        // Section 3: Spotify Account
        item {
            SettingsCard(title = "Spotify Account", icon = Icons.Default.AccountCircle) {
                if (isLoggedIn) {
                    SettingsActionRow(
                        title = "Spotify Connected",
                        subtitle = "Logged in as ${currentUser?.displayName ?: "User"}"
                    ) {
                        OutlinedButton(
                            onClick = { spotifySession.logout() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Disconnect", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    SettingsActionRow(
                        title = "Connect Spotify",
                        subtitle = "Sign in to synchronize Liked Songs and playlists"
                    ) {
                        Button(
                            onClick = onOpenLoginDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Connect", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                }
            }
        }

        // Section 4: Cache & Maintenance
        item {
            SettingsCard(title = "Storage & Cache", icon = Icons.Default.Storage) {
                SettingsActionRow(
                    title = "Clear Audio Cache",
                    subtitle = "Free up temporary cache files and streamed audio segments"
                ) {
                    OutlinedButton(
                        onClick = {
                            val freed = DesktopSettings.clearCache()
                            val mb = freed / (1024 * 1024)
                            playerController.notify("Cleaned ${mb} MB of temporary audio cache.")
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Clear Cache", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MeldPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        action()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MeldPrimary
            )
        )
    }
}
