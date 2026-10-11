package com.marvis.silentcamera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightScheme=lightColorScheme(
    primary=Color(0xFF16734F),onPrimary=Color.White,primaryContainer=Color(0xFFE3F2EA),onPrimaryContainer=Color(0xFF14583E),
    secondary=Color(0xFF52645A),onSecondary=Color.White,secondaryContainer=Color(0xFFEEF2EF),onSecondaryContainer=Color(0xFF33483D),
    tertiary=Color(0xFF16734F),onTertiary=Color.White,
    background=Color(0xFFF5F6F5),onBackground=Color(0xFF161C18),surface=Color.White,onSurface=Color(0xFF161C18),
    surfaceVariant=Color(0xFFEEF1EE),onSurfaceVariant=Color(0xFF5D665F),outline=Color(0xFFACB5AD),outlineVariant=Color(0xFFE5E9E5),
    surfaceTint=Color.Transparent,error=Color(0xFFB83F3F),errorContainer=Color(0xFFFCE9E8),
)
private val DarkScheme=darkColorScheme(
    primary=Color(0xFF80D8AE),onPrimary=Color(0xFF0A3523),primaryContainer=Color(0xFF203E2F),onPrimaryContainer=Color(0xFFA8E8C4),
    secondary=Color(0xFFABC2B3),onSecondary=Color(0xFF17291E),secondaryContainer=Color(0xFF28322C),onSecondaryContainer=Color(0xFFD7E3DB),
    tertiary=Color(0xFF80D8AE),background=Color(0xFF101412),onBackground=Color(0xFFECF1ED),
    surface=Color(0xFF1A201C),onSurface=Color(0xFFECF1ED),surfaceVariant=Color(0xFF252E27),onSurfaceVariant=Color(0xFF97A69B),
    outline=Color(0xFF627065),outlineVariant=Color(0xFF303B33),surfaceTint=Color.Transparent,
    error=Color(0xFFFFAEAA),errorContainer=Color(0xFF492723),
)
private val AppTypography=Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=38.sp,letterSpacing=(-0.6).sp),
    headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=32.sp,letterSpacing=(-0.4).sp),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=22.sp,lineHeight=28.sp,letterSpacing=(-0.4).sp),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=24.sp),
    titleSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
    bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=22.sp),
    bodySmall=TextStyle(fontSize=12.sp,lineHeight=18.sp),labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
    labelMedium=TextStyle(fontWeight=FontWeight.Medium,fontSize=11.sp,lineHeight=16.sp),labelSmall=TextStyle(fontSize=10.sp,lineHeight=14.sp),
)
@Composable
fun SilentCameraTheme(theme:String="system",content:@Composable ()->Unit) {
    val dark=when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
    MaterialTheme(colorScheme=if(dark)DarkScheme else LightScheme,typography=AppTypography,
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(30.dp)),content=content)
}
