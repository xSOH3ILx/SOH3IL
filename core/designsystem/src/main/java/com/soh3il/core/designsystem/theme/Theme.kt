package com.soh3il.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.soh3il.core.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = FarsiPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0369A1),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = FarsiDarkBackground,
    onBackground = Color(0xFFF4F4F5),
    surface = FarsiDarkSurface,
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = FarsiDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = FarsiDarkBorder,
    outlineVariant = Color(0xFF3F3F46),
    error = DarkHolidayText,
    onError = Color(0xFF450A0A),
    errorContainer = DarkHolidayContainer,
    onErrorContainer = DarkHolidayText
)

private val AmoledColorScheme = darkColorScheme(
    primary = FarsiPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0284C7),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF09090B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = AmoledSurfaceContainerLowest,
    surfaceContainerLow = AmoledSurfaceContainerLow,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest,
    outline = Color(0xFF27272A),
    outlineVariant = Color(0xFF3F3F46),
    error = DarkHolidayText,
    onError = Color.Black,
    errorContainer = DarkHolidayContainer,
    onErrorContainer = DarkHolidayText
)

private val LightColorScheme = lightColorScheme(
    primary = FarsiPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBAE6FD),
    onTertiaryContainer = Color(0xFF0369A1),
    background = FarsiLightBackground,
    onBackground = Color(0xFF09090B),
    surface = FarsiLightSurface,
    onSurface = Color(0xFF09090B),
    surfaceVariant = FarsiLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = FarsiLightBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = LightHolidayText,
    onError = Color.White,
    errorContainer = LightHolidayContainer,
    onErrorContainer = LightHolidayText
)

@Composable
fun Soh3ilTheme(
    themePreference: AppTheme = AppTheme.SYSTEM,
    isAmoled: Boolean = false,
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
        isDark && (isAmoled || themePreference == AppTheme.AMOLED_DARK) -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    // Force RTL layout direction for true Persian experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Soh3ilTypography
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        }
    }
}
