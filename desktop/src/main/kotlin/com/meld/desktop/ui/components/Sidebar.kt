package com.meld.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated

enum class DesktopNavScreen {
    HOME,
    SEARCH,
    LIKED_SONGS,
    SETTINGS
}

@Composable
fun Sidebar(
    currentScreen: DesktopNavScreen,
    onNavigate: (DesktopNavScreen) -> Unit,
    spotifySession: SpotifySession,
    onOpenLoginDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    val currentUser by spotifySession.currentUser.collectAsState()

    Column(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Branding Header - Metrolist Signature Style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, bottom = 28.dp, top = 4.dp)
            ) {
                Surface(
                    color = MeldPrimary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "M",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MELD",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Metrolist Desktop",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Material 3 Navigation Pill Items
            SidebarNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentScreen == DesktopNavScreen.HOME,
                onClick = { onNavigate(DesktopNavScreen.HOME) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarNavItem(
                icon = Icons.Default.Search,
                label = "Search",
                isSelected = currentScreen == DesktopNavScreen.SEARCH,
                onClick = { onNavigate(DesktopNavScreen.SEARCH) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarNavItem(
                icon = Icons.Default.Favorite,
                label = "Liked Songs",
                isSelected = currentScreen == DesktopNavScreen.LIKED_SONGS,
                onClick = { onNavigate(DesktopNavScreen.LIKED_SONGS) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentScreen == DesktopNavScreen.SETTINGS,
                onClick = { onNavigate(DesktopNavScreen.SETTINGS) }
            )
        }

        // Account status block at bottom of sidebar
        Surface(
            color = MeldSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MeldPrimary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (currentUser?.displayName?.firstOrNull() ?: 'U').uppercase(),
                                    color = MeldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName ?: "Spotify User",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Synced",
                                fontSize = 11.sp,
                                color = MeldPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { spotifySession.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Log Out", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spotify Account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sync playlists and daily recommendations.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenLoginDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MeldPrimary.copy(alpha = 0.16f) else Color.Transparent
    val contentColor = if (isSelected) MeldPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
