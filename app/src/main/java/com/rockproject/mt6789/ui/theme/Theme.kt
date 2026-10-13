package com.rockproject.mt6789.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RockColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHigh: Color,
    val surfaceTranslucent: Color,
    val border: Color,
    val borderLight: Color,
    val separator: Color,
    val label: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val purple: Color,
    val mint: Color,
    val mintInk: Color,
    val isDark: Boolean
)

val LocalRockColors = staticCompositionLocalOf {
    RockColors(
        background = DarkBackground,
        surface = DarkSurface,
        surfaceRaised = DarkSurfaceRaised,
        surfaceHigh = DarkSurfaceHigh,
        surfaceTranslucent = DarkSurfaceTranslucent,
        border = DarkBorder,
        borderLight = DarkBorderLight,
        separator = DarkSeparator,
        label = DarkLabel,
        labelSecondary = DarkLabelSecondary,
        labelTertiary = DarkLabelTertiary,
        blue = AccentBlueDark,
        green = AccentGreen,
        red = AccentRedDark,
        orange = AccentOrange,
        purple = AccentPurple,
        mint = AccentMint,
        mintInk = AccentMintInk,
        isDark = true
    )
}

object RockTheme {
    val colors: RockColors
        @Composable
        get() = LocalRockColors.current
}

@Composable
fun RockProjectTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val rockColors = if (darkTheme) {
        RockColors(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceRaised = DarkSurfaceRaised,
            surfaceHigh = DarkSurfaceHigh,
            surfaceTranslucent = DarkSurfaceTranslucent,
            border = DarkBorder,
            borderLight = DarkBorderLight,
            separator = DarkSeparator,
            label = DarkLabel,
            labelSecondary = DarkLabelSecondary,
            labelTertiary = DarkLabelTertiary,
            blue = AccentBlueDark,
            green = AccentGreen,
            red = AccentRedDark,
            orange = AccentOrange,
            purple = AccentPurple,
            mint = AccentMint,
            mintInk = AccentMintInk,
            isDark = true
        )
    } else {
        RockColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceRaised = LightSurfaceRaised,
            surfaceHigh = LightSurfaceHigh,
            surfaceTranslucent = LightSurfaceTranslucent,
            border = LightBorder,
            borderLight = LightBorderLight,
            separator = LightSeparator,
            label = LightLabel,
            labelSecondary = LightLabelSecondary,
            labelTertiary = LightLabelTertiary,
            blue = AccentBlueLight,
            green = AccentGreen,
            red = AccentRedLight,
            orange = AccentOrange,
            purple = AccentPurple,
            mint = AccentMint,
            mintInk = AccentMintInk,
            isDark = false
        )
    }

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AccentBlueDark,
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = Color.White,
            onBackground = DarkLabel,
            onSurface = DarkLabel
        )
    } else {
        lightColorScheme(
            primary = AccentBlueLight,
            background = LightBackground,
            surface = LightSurface,
            onPrimary = Color.White,
            onBackground = LightLabel,
            onSurface = LightLabel
        )
    }

    CompositionLocalProvider(LocalRockColors provides rockColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
