package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoContainer,
    onPrimaryContainer = PrimaryIndigoGlow,
    secondary = AccentViolet,
    onSecondary = Color.White,
    secondaryContainer = AccentVioletContainer,
    onSecondaryContainer = AccentVioletLight,
    tertiary = AccentCoral,
    onTertiary = Color.White,
    tertiaryContainer = AccentCoralContainer,
    onTertiaryContainer = AccentCoralLight,
    background = ObsidianDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = Color(0xFF243046),
    error = Color(0xFFEF4444),
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F3FF),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFFE11D48),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Luxury modern dark aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
