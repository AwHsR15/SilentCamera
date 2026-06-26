package com.marvis.silentcamera

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 应用配色（深色绿意主题）。 */
object AppColors {
    val Primary = Color(0xFF8BC34A)
    val PrimaryDark = Color(0xFF558B2F)
    val Secondary = Color(0xFF4DB6AC)
    val Background = Color(0xFF101412)
    val Surface = Color(0xFF1A201D)
    val SurfaceVariant = Color(0xFF232B27)
    val OnSurface = Color(0xFFE6EAE7)
    val OnSurfaceMuted = Color(0xFF9AA39E)

    val Success = Color(0xFF66BB6A)
    val Warning = Color(0xFFFFB74D)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF64B5F6)
}

private val DarkScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color(0xFF0E1A05),
    secondary = AppColors.Secondary,
    background = AppColors.Background,
    surface = AppColors.Surface,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurface = AppColors.OnSurface,
    onSurfaceVariant = AppColors.OnSurfaceMuted,
    error = AppColors.Error,
)

@Composable
fun SilentCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
