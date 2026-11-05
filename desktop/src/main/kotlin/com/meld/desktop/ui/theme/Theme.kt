package com.meld.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meld.desktop.state.DesktopSettings

// Metrolist Authentic Signature Brand Colors
val MeldPrimary = Color(0xFFED5564) // Coral Red
val MeldSecondary = Color(0xFFFF7A8A)
val MeldPrimaryContainer = Color(0xFF381518)
val MeldOnPrimary = Color.White
val MeldOnPrimaryContainer = Color(0xFFFFDADB)

// Pure AMOLED Black palette
val MeldPureBlack = Color(0xFF000000)
val MeldPureSurface = Color(0xFF0D0D0D)
val MeldPureSurfaceVariant = Color(0xFF181818)
val MeldPureSurfaceElevated = Color(0xFF141414)

// Standard Dark palette
val MeldDarkBackground = Color(0xFF121212)
val MeldDarkSurface = Color(0xFF1E1E1E)
val MeldDarkSurfaceVariant = Color(0xFF282828)
val MeldDarkSurfaceElevated = Color(0xFF222222)
val MeldDarkOnBackground = Color(0xFFF4F4F4)
val MeldDarkOnSurface = Color(0xFFEEEEEE)
val MeldDarkOnSurfaceVariant = Color(0xFFAAAAAA)
val MeldDarkOutline = Color(0xFF2E2E2E)

// Light palette
val MeldLightBackground = Color(0xFFF9F9FB)
val MeldLightSurface = Color(0xFFFFFFFF)
val MeldLightSurfaceVariant = Color(0xFFEFEFEF)
val MeldLightSurfaceElevated = Color(0xFFF4F4F6)
val MeldLightOnBackground = Color(0xFF1A1A1A)
val MeldLightOnSurface = Color(0xFF202020)
val MeldLightOnSurfaceVariant = Color(0xFF6E6E6E)
val MeldLightOutline = Color(0xFFE2E2E2)

// Common surfaces for quick reference
val MeldSurfaceElevated: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val MeldTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun MeldDesktopTheme(content: @Composable () -> Unit) {
    val themeMode by DesktopSettings.themeMode.collectAsState()
    val pureBlack by DesktopSettings.pureBlack.collectAsState()

    val isDark = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        if (pureBlack) {
            darkColorScheme(
                primary = MeldPrimary,
                onPrimary = MeldOnPrimary,
                primaryContainer = MeldPrimaryContainer,
                onPrimaryContainer = MeldOnPrimaryContainer,
                secondary = MeldSecondary,
                background = MeldPureBlack,
                onBackground = MeldDarkOnBackground,
                surface = MeldPureSurface,
                onSurface = MeldDarkOnSurface,
                surfaceVariant = MeldPureSurfaceVariant,
                onSurfaceVariant = MeldDarkOnSurfaceVariant,
                outline = MeldDarkOutline
            )
        } else {
            darkColorScheme(
                primary = MeldPrimary,
                onPrimary = MeldOnPrimary,
                primaryContainer = MeldPrimaryContainer,
                onPrimaryContainer = MeldOnPrimaryContainer,
                secondary = MeldSecondary,
                background = MeldDarkBackground,
                onBackground = MeldDarkOnBackground,
                surface = MeldDarkSurface,
                onSurface = MeldDarkOnSurface,
                surfaceVariant = MeldDarkSurfaceVariant,
                onSurfaceVariant = MeldDarkOnSurfaceVariant,
                outline = MeldDarkOutline
            )
        }
    } else {
        lightColorScheme(
            primary = MeldPrimary,
            onPrimary = MeldOnPrimary,
            primaryContainer = MeldPrimaryContainer,
            onPrimaryContainer = MeldOnPrimaryContainer,
            secondary = MeldSecondary,
            background = MeldLightBackground,
            onBackground = MeldLightOnBackground,
            surface = MeldLightSurface,
            onSurface = MeldLightOnSurface,
            surfaceVariant = MeldLightSurfaceVariant,
            onSurfaceVariant = MeldLightOnSurfaceVariant,
            outline = MeldLightOutline
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeldTypography,
        content = content
    )
}
