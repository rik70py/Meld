package com.meld.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Metrolist Authentic Signature Brand Colors
val MeldPrimary = Color(0xFFED5564) // Coral Red (DefaultThemeColor from mobile app)
val MeldSecondary = Color(0xFFFF7A8A)
val MeldPrimaryContainer = Color(0xFF381518)
val MeldOnPrimary = Color.White
val MeldOnPrimaryContainer = Color(0xFFFFDADB)

val MeldBackground = Color(0xFF0D0D0D) // Deep AMOLED Black matching mobile app
val MeldSurface = Color(0xFF161616)
val MeldSurfaceVariant = Color(0xFF222222)
val MeldSurfaceElevated = Color(0xFF1E1E1E)

val MeldOnBackground = Color(0xFFF4F4F4)
val MeldOnSurface = Color(0xFFEEEEEE)
val MeldOnSurfaceVariant = Color(0xFF9E9E9E)
val MeldOutline = Color(0xFF2E2E2E)

private val MeldDarkColorScheme = darkColorScheme(
    primary = MeldPrimary,
    onPrimary = MeldOnPrimary,
    primaryContainer = MeldPrimaryContainer,
    onPrimaryContainer = MeldOnPrimaryContainer,
    secondary = MeldSecondary,
    background = MeldBackground,
    onBackground = MeldOnBackground,
    surface = MeldSurface,
    onSurface = MeldOnSurface,
    surfaceVariant = MeldSurfaceVariant,
    onSurfaceVariant = MeldOnSurfaceVariant,
    outline = MeldOutline
)

@Composable
fun MeldDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeldDarkColorScheme,
        content = content
    )
}
