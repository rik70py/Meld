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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meld.desktop.state.DesktopSettings

// Metrolist Authentic Signature Brand Colors
val MeldPrimary = Color(0xFFED5564) // Coral Red
val MeldSecondary = Color(0xFFFF7A8A)

// Dark theme container colors
val MeldDarkPrimaryContainer = Color(0xFF381518)
val MeldDarkOnPrimaryContainer = Color(0xFFFFDADB)
val MeldOnPrimary = Color.White

// Light theme dedicated container colors
val MeldLightPrimaryContainer = Color(0xFFFFDAD9)
val MeldLightOnPrimaryContainer = Color(0xFF410006)

// Pure AMOLED Black palette
val MeldPureBlack = Color(0xFF000000)
val MeldPureSurface = Color(0xFF0D0D0D)
val MeldPureSurfaceVariant = Color(0xFF181818)

// Standard Dark palette
val MeldDarkBackground = Color(0xFF121212)
val MeldDarkSurface = Color(0xFF1E1E1E)
val MeldDarkSurfaceVariant = Color(0xFF282828)
val MeldDarkOnBackground = Color(0xFFF4F4F4)
val MeldDarkOnSurface = Color(0xFFEEEEEE)
val MeldDarkOnSurfaceVariant = Color(0xFFAAAAAA)
val MeldDarkOutline = Color(0xFF2E2E2E)

// High-contrast Light palette
val MeldLightBackground = Color(0xFFF6F7F9)
val MeldLightSurface = Color(0xFFFFFFFF)
val MeldLightSurfaceVariant = Color(0xFFECEFF2)
val MeldLightOnBackground = Color(0xFF141517)
val MeldLightOnSurface = Color(0xFF1C1E21)
val MeldLightOnSurfaceVariant = Color(0xFF656970)
val MeldLightOutline = Color(0xFFD6DBE1)

val MeldSurfaceElevated: Color
    @Composable
    get() {
        val themeMode by DesktopSettings.themeMode.collectAsState()
        val isDark = when (themeMode) {
            "Light" -> false
            "Dark" -> true
            else -> isSystemInDarkTheme()
        }
        return if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    }

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
                primaryContainer = MeldDarkPrimaryContainer,
                onPrimaryContainer = MeldDarkOnPrimaryContainer,
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
                primaryContainer = MeldDarkPrimaryContainer,
                onPrimaryContainer = MeldDarkOnPrimaryContainer,
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
            primaryContainer = MeldLightPrimaryContainer,
            onPrimaryContainer = MeldLightOnPrimaryContainer,
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
