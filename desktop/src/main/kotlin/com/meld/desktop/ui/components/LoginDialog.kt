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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.meld.desktop.auth.DesktopSpotifyAuth
import com.meld.desktop.state.SpotifySession
import com.meld.desktop.ui.theme.MeldPrimary
import com.meld.desktop.ui.theme.MeldSurfaceElevated
import kotlinx.coroutines.launch

@Composable
fun LoginDialog(
    spotifySession: SpotifySession,
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
            onDismiss()
        }
    }

    Dialog(onDismissRequest = {
        if (!isDirectLoggingIn && !isLoading) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MeldSurfaceElevated,
            modifier = Modifier.width(480.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Connect Spotify",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sync your personal playlists and Spotify recommendations directly into Meld PC.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary: Direct 1-Click Browser Login
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in using your account in the popup window. Meld will automatically detect your login and connect.",
                                fontSize = 11.sp,
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
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MeldPrimary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log In Directly with Spotify",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Supports Google, Apple, Facebook, and Email/Password.",
                                fontSize = 11.sp,
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
                        fontSize = 12.sp
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional fallback: Manual cookie entry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showManualInput = !showManualInput }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Or enter sp_dc cookie manually",
                        fontSize = 12.sp,
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
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Connect with Cookie", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isDirectLoggingIn
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
