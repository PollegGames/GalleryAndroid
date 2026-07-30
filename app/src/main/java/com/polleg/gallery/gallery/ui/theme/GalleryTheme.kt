package com.polleg.gallery.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF335C67),
    onPrimary = Color.White,
    secondary = Color(0xFF8C5D12),
    tertiary = Color(0xFF7A3E48),
    background = Color(0xFFFFF8F1),
    surface = Color(0xFFFFF8F1),
    surfaceVariant = Color(0xFFE6E0DA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCAD6),
    secondary = Color(0xFFFFB95D),
    tertiary = Color(0xFFFFB2BC),
    background = Color(0xFF111416),
    surface = Color(0xFF111416),
    surfaceVariant = Color(0xFF2B3033),
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
