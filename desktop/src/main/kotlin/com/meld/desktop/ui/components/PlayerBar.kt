package com.meld.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated

@Composable
fun PlayerBar(
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val currentTrack by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.audioPlayer.isPlaying.collectAsState()
    val positionMs by playerController.audioPlayer.positionMs.collectAsState()
    val durationMs by playerController.audioPlayer.durationMs.collectAsState()
    val volume by playerController.audioPlayer.volume.collectAsState()
    val isResolving by playerController.isResolvingStream.collectAsState()

    val totalDuration = if (durationMs > 0) durationMs else (currentTrack?.durationMs ?: 1L)
    val progress = (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = MeldSurfaceElevated,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top scrub progress bar
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val targetMs = (newProgress * totalDuration).toLong()
                    playerController.audioPlayer.seekTo(targetMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = MeldPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Active Track Details with Artwork
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thumb = currentTrack?.thumbnailUrl
                    if (!thumb.isNullOrBlank()) {
                        AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("♪", fontSize = 20.sp, color = MeldPrimary)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = currentTrack?.title ?: "No track playing",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isResolving) "Connecting stream..." else (currentTrack?.artist ?: "Meld Desktop"),
                            fontSize = 12.sp,
                            color = if (isResolving) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Center: Playback Controls & Timers
                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { playerController.playPrevious() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { playerController.audioPlayer.togglePlayPause() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MeldPrimary),
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { playerController.playNext() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.width(280.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatMs(positionMs),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatMs(totalDuration),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Volume control
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val volumeIcon = when {
                        volume == 0 -> Icons.Default.VolumeMute
                        volume < 50 -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    }
                    Icon(
                        volumeIcon,
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { playerController.audioPlayer.setVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.width(110.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MeldPrimary,
                            thumbColor = MeldPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
