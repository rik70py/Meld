package com.meld.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meld.desktop.auth.DesktopSpotifyAuth
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginDialog(
    spotifySession: SpotifySession,
    onInputFocusChanged: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    var spDcInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var isDirectLoggingIn by remember { mutableStateOf(false) }
    var directLoginStatus by remember { mutableStateOf<String?>(null) }
    var directLoginError by remember { mutableStateOf<String?>(null) }

    val isLoading by spotifySession.isLoading.collectAsState()
    val errorMessage by spotifySession.errorMessage.collectAsState()
    val isLoggedIn by spotifySession.isLoggedIn.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onInputFocusChanged(false)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = {
        if (!isDirectLoggingIn && !isLoading) {
            onInputFocusChanged(false)
            onDismiss()
        }
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.width(480.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Connect Spotify",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Synchronize your personal Liked Songs and playlists seamlessly into Meld.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary: Direct 1-Click Browser Login
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isDirectLoggingIn) {
                            CircularProgressIndicator(
                                color = MeldPrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = directLoginStatus ?: "Waiting for Spotify authentication...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in using your account in the browser window. Meld will automatically detect your login and connect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isDirectLoggingIn = true
                                        directLoginError = null
                                        directLoginStatus = "Opening Spotify login window..."
                                        val result = DesktopSpotifyAuth.launchDirectLogin { status ->
                                            directLoginStatus = status
                                        }
                                        result.onSuccess { spDc ->
                                            directLoginStatus = "Session detected! Fetching profile..."
                                            spotifySession.connectWithSpDc(spDc)
                                        }.onFailure { err ->
                                            directLoginError = err.message ?: "Authentication failed."
                                        }
                                        isDirectLoggingIn = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log In Directly with Spotify",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Supports Google, Apple, Facebook, and Email/Password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (directLoginError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = directLoginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional fallback: Manual cookie entry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showManualInput = !showManualInput }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Or enter sp_dc cookie manually",
                        style = MaterialTheme.typography.labelMedium,
                        color = MeldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (showManualInput) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MeldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showManualInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spDcInput,
                        onValueChange = { spDcInput = it },
                        label = { Text("sp_dc Cookie") },
                        placeholder = { Text("AQB...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onInputFocusChanged(it.isFocused) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MeldPrimary,
                            cursorColor = MeldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { spotifySession.connectWithSpDc(spDcInput.trim()) },
                        enabled = spDcInput.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.align(Alignment.End).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Connect with Cookie", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onInputFocusChanged(false)
                            onDismiss()
                        },
                        enabled = !isDirectLoggingIn,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
