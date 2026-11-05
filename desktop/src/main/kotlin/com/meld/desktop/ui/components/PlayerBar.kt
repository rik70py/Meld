package com.meld.desktop.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.meld.desktop.state.PlayerController
import com.meld.desktop.state.RepeatMode
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSecondary

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
    val repeatMode by playerController.repeatMode.collectAsState()
    val isShuffle by playerController.isShuffle.collectAsState()
    val isQueueOpen by playerController.isQueueDrawerOpen.collectAsState()

    var isLiked by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableIntStateOf(100) }

    val totalDuration = if (durationMs > 0) durationMs else (currentTrack?.durationMs ?: 1L)
    val progress = (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Left: Current Track Details
            Row(
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val thumb = currentTrack?.thumbnailUrl
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MeldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "No track playing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isResolving) "Connecting stream..." else (currentTrack?.artist ?: "Meld"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isResolving) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (currentTrack != null) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            playerController.notify(if (isLiked) "Added to your Liked Songs" else "Removed from Liked Songs")
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Center: Controls & Proper Hover-Expanding Seek Bar
            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top control buttons row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Shuffle Toggle
                    IconButton(
                        onClick = { playerController.toggleShuffle() },
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous Button
                    IconButton(
                        onClick = { playerController.playPrevious() },
                        modifier = Modifier.size(34.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous (Ctrl+Left)",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play / Pause Circle
                    Surface(
                        onClick = { playerController.audioPlayer.togglePlayPause() },
                        color = MeldPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Next Button
                    IconButton(
                        onClick = { playerController.playNext() },
                        modifier = Modifier.size(34.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next (Ctrl+Right)",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat Toggle (Off, All, One)
                    IconButton(
                        onClick = { playerController.toggleRepeat() },
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat: $repeatMode",
                            tint = if (repeatMode != RepeatMode.OFF) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Seek Bar Row: [0:00] ───[Seek Slider]─── [3:45]
                val seekInteractionSource = remember { MutableInteractionSource() }
                val isSeekHovered by seekInteractionSource.collectIsHoveredAsState()
                val sliderHeight by animateDpAsState(if (isSeekHovered) 6.dp else 4.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatMs(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .hoverable(seekInteractionSource)
                            .pointerHoverIcon(PointerIcon.Hand),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val targetMs = (newProgress * totalDuration).toLong()
                                playerController.audioPlayer.seekTo(targetMs)
                            },
                            interactionSource = seekInteractionSource,
                            modifier = Modifier.fillMaxWidth().height(sliderHeight + 14.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MeldPrimary,
                                thumbColor = if (isSeekHovered) MeldPrimary else Color.Transparent,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Text(
                        formatMs(totalDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                }
            }

            // 3. Right: Queue Drawer Toggle & Volume
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Queue Drawer Toggle Button
                IconButton(
                    onClick = { playerController.toggleQueueDrawer() },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue Panel",
                        tint = if (isQueueOpen) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mute / Unmute Button
                val volumeIcon = when {
                    volume == 0 -> Icons.Default.VolumeMute
                    volume < 50 -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }

                IconButton(
                    onClick = {
                        if (volume > 0) {
                            previousVolume = volume
                            playerController.audioPlayer.setVolume(0)
                        } else {
                            playerController.audioPlayer.setVolume(if (previousVolume > 0) previousVolume else 70)
                        }
                    },
                    modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        volumeIcon,
                        contentDescription = "Volume Mute/Unmute",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { playerController.audioPlayer.setVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .width(100.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
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

fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
