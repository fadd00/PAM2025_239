package com.sample.image_board.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Electric Violet Dark Theme (Primary theme for this app)
private val DarkColorScheme =
        darkColorScheme(
                primary = Primary,
                onPrimary = OnPrimary,
                background = Background,
                surface = Surface,
                onBackground = OnSurface,
                onSurface = OnSurface,
                surfaceVariant = SurfaceVariant,
                onSurfaceVariant = OnSurfaceVariant,
                error = Error,
                onError = OnError
        )

// Light theme (fallback, but app primarily uses dark)
private val LightColorScheme =
        lightColorScheme(
                primary = Color(0xFF6750A4),
                onPrimary = Color.White,
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F)
        )

@Composable
fun ImageboardTheme(
        darkTheme: Boolean = true, // Default to dark theme
        dynamicColor: Boolean = false, // Disabled for consistent branding
        content: @Composable () -> Unit
) {
        // Always use dark theme for this app (SRS requirement)
        val colorScheme = DarkColorScheme

        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
