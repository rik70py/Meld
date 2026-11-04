package com.meld.desktop.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meld.desktop.state.DesktopSettings
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated

@Composable
fun SettingsView(
    spotifySession: SpotifySession,
    onOpenLoginDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    val currentUser by spotifySession.currentUser.collectAsState()

    var pureBlack by remember { mutableStateOf(DesktopSettings.pureBlack.value) }
    var autoPlayNext by remember { mutableStateOf(DesktopSettings.autoPlayNext.value) }
    var normalizeVolume by remember { mutableStateOf(DesktopSettings.normalizeVolume.value) }
    var audioQuality by remember { mutableStateOf(DesktopSettings.audioQuality.value) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }

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
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure audio streaming, Spotify sync, and application preferences.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Account & Integrations
        item {
            SettingsCard(title = "Account & Integrations", icon = Icons.Default.AccountCircle) {
                if (isLoggedIn) {
                    SettingsActionRow(
                        title = "Spotify Account",
                        subtitle = "Connected as ${currentUser?.displayName ?: "Spotify User"}"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { spotifySession.logout() },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Disconnect", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    SettingsActionRow(
                        title = "Spotify Integration",
                        subtitle = "Sync your personal playlists and Spotify recommendations"
                    ) {
                        Button(
                            onClick = onOpenLoginDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Connect Spotify", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
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
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Switch", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)

                SettingsToggleRow(
                    title = "Auto-play Next Track",
                    subtitle = "Automatically queue and play the next song when the current track finishes",
                    checked = autoPlayNext,
                    onCheckedChange = {
                        autoPlayNext = it
                        DesktopSettings.autoPlayNext.value = it
                        DesktopSettings.save()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)

                SettingsToggleRow(
                    title = "Volume Normalization",
                    subtitle = "Balance loudness levels across different audio streams",
                    checked = normalizeVolume,
                    onCheckedChange = {
                        normalizeVolume = it
                        DesktopSettings.normalizeVolume.value = it
                        DesktopSettings.save()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)

                SettingsActionRow(
                    title = "Audio Engine Backend",
                    subtitle = "LibVLC 64-bit Direct Audio Output with Hardware Decoders"
                ) {
                    Surface(
                        color = MeldPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Active",
                            color = MeldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Appearance
        item {
            SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
                SettingsToggleRow(
                    title = "AMOLED Pure Black",
                    subtitle = "Use pure black #000000 background for modern high-contrast aesthetic",
                    checked = pureBlack,
                    onCheckedChange = {
                        pureBlack = it
                        DesktopSettings.pureBlack.value = it
                        DesktopSettings.save()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)

                SettingsActionRow(
                    title = "Accent Theme Color",
                    subtitle = "Metrolist Signature Coral Red (#ED5564)"
                ) {
                    Surface(
                        color = MeldPrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(24.dp)
                    ) {}
                }
            }
        }

        // Section 4: Cache & Storage
        item {
            SettingsCard(title = "Storage & Cache", icon = Icons.Default.Storage) {
                SettingsActionRow(
                    title = "Image & Network Cache",
                    subtitle = cacheMessage ?: "Cached song artwork and temporary session files"
                ) {
                    Button(
                        onClick = {
                            val freed = DesktopSettings.clearCache()
                            cacheMessage = "Cleared temporary files successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Clear Cache", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Section 5: About Meld PC
        item {
            SettingsCard(title = "About Meld PC", icon = Icons.Default.Info) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Meld PC v1.0.0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A modern Compose Multiplatform desktop music client that combines Spotify's recommendation algorithms and personal library with YouTube Music's high-bitrate audio streaming catalog.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Licensed under GNU General Public License v3.0",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MeldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Surface(
            color = MeldSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MeldPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        action()
    }
}
