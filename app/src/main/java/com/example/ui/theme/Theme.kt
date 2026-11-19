package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightShayariColorScheme = lightColorScheme(
    primary = CrimsonRosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseTintContainer,
    onPrimaryContainer = OnRoseTintContainer,
    secondary = WarmCoralSecondary,
    onSecondary = Color.White,
    secondaryContainer = WarmPeachContainer,
    onSecondaryContainer = OnWarmPeachContainer,
    tertiary = FreshTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = FreshTealContainer,
    onTertiaryContainer = OnFreshTealContainer,
    background = PearlBackground,
    onBackground = CharcoalInk,
    surface = PureWhiteSurface,
    onSurface = CharcoalInk,
    surfaceVariant = SoftBlushSurface,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLow = PureWhiteSurface,
    surfaceContainer = SoftBlushSurface,
    surfaceContainerHigh = SoftBlushSurfaceHigh,
    outline = CrispOutline,
    outlineVariant = HairlineOutline,
    error = ErrorLight,
    onError = Color.White
)

private val DarkShayariColorScheme = darkColorScheme(
    primary = SoftRosePrimary,
    onPrimary = Color(0xFF2B0712),
    primaryContainer = DarkRoseContainer,
    onPrimaryContainer = OnDarkRoseContainer,
    secondary = WarmGoldSecondary,
    onSecondary = Color(0xFF2E1E00),
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = OnDarkGoldContainer,
    tertiary = AquaTealTertiary,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = DarkTealContainer,
    onTertiaryContainer = OnDarkTealContainer,
    background = MidnightBackground,
    onBackground = CrispSnowInk,
    surface = MidnightSurface,
    onSurface = CrispSnowInk,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = CoolAshMuted,
    surfaceContainerLow = MidnightSurfaceElevated,
    surfaceContainer = MidnightSurfaceVariant,
    surfaceContainerHigh = Color(0xFF2F3647),
    outline = SlateOutlineDark,
    outlineVariant = SlateHairlineDark,
    error = ErrorDark,
    onError = Color(0xFF450A0A)
)

val ShayariShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ShayariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    readingStyle: ShayariReadingStyle = ShayariReadingStyle(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkShayariColorScheme else LightShayariColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalShayariReadingStyle provides readingStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShayariTypography,
            shapes = ShayariShapes,
            content = content
        )
    }
}
