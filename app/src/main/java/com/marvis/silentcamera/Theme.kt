package com.marvis.silentcamera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme=darkColorScheme(
    primary=Color(0xFFAFF0C4),onPrimary=Color(0xFF00391E),primaryContainer=Color(0xFF1C5134),
    secondary=Color(0xFFADCDBB),background=Color(0xFF101813),surface=Color(0xFF131D17),
    surfaceVariant=Color(0xFF263B2D),onSurface=Color(0xFFE3EBE3),onSurfaceVariant=Color(0xFFBCCCBD),
)
private val LightScheme=lightColorScheme(
    primary=Color(0xFF28633F),onPrimary=Color.White,primaryContainer=Color(0xFFDAF5E0),
    secondary=Color(0xFF526C5B),background=Color(0xFFF5F8F2),surface=Color(0xFFFAFCF7),
    surfaceVariant=Color(0xFFE4EEE2),onSurface=Color(0xFF17231B),onSurfaceVariant=Color(0xFF4C5D50),
)
@Composable
fun SilentCameraTheme(theme:String="system",content:@Composable ()->Unit) {
    val dark=when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
    MaterialTheme(colorScheme=if(dark)DarkScheme else LightScheme,content=content)
}
