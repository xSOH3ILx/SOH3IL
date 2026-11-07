package com.soh3il.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.soh3il.core.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = FarsiPrimaryDark,
    onPrimary = Color.White,
    background = FarsiDarkBackground,
    surface = FarsiDarkSurface,
    surfaceVariant = FarsiDarkSurfaceVariant,
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    outline = FarsiDarkBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = FarsiPrimaryDark,
    onPrimary = Color.White,
    background = Color.Black,
    surface = Color(0xFF0F0F11),
    surfaceVariant = Color(0xFF18181B),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF27272A)
)

private val LightColorScheme = lightColorScheme(
    primary = FarsiPrimary,
    onPrimary = Color.White,
    background = FarsiLightBackground,
    surface = FarsiLightSurface,
    surfaceVariant = FarsiLightSurfaceVariant,
    onBackground = Color(0xFF09090B),
    onSurface = Color(0xFF09090B),
    outline = FarsiLightBorder
)

@Composable
fun Soh3ilTheme(
    themePreference: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AMOLED_DARK -> true
        AppTheme.SYSTEM -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        themePreference == AppTheme.AMOLED_DARK -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // Force RTL layout direction for true Persian experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Soh3ilTypography,
            content = content
        )
    }
}
