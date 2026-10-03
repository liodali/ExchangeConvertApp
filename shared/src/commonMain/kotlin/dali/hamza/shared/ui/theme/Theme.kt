package dali.hamza.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material color scheme derived from a [LedgerPalette] (single source for
 * both appearance modes). Primary = the palette's info blue pairing;
 * secondary = the accent (positive/live); tertiary = gold (premium).
 */
fun ledgerColorScheme(p: LedgerPalette, dark: Boolean): ColorScheme {
    val onAccent = if (dark) p.canvas else Color.White
    return if (dark) {
        darkColorScheme(
            primary = p.blue,
            onPrimary = p.navyPanel,
            primaryContainer = p.navyPanel,
            onPrimaryContainer = p.blueSoft,
            secondary = p.green,
            onSecondary = p.canvas,
            secondaryContainer = p.greenGlow,
            onSecondaryContainer = p.green,
            tertiary = p.gold,
            onTertiary = p.canvas,
            tertiaryContainer = p.goldGlow,
            onTertiaryContainer = p.gold,
            background = p.canvas,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            onSurfaceVariant = p.textSecondary,
            surfaceVariant = p.surfaceElevated,
            surfaceContainerLowest = p.canvas,
            surfaceContainerLow = p.surfaceElevated,
            surfaceContainer = p.surfaceRaised,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.cardAlt,
            error = p.error,
            onError = p.canvas,
            errorContainer = p.error.copy(alpha = 0.12f),
            onErrorContainer = p.error,
            outline = p.steel,
            outlineVariant = p.card,
            inverseSurface = p.textPrimary,
            inverseOnSurface = p.canvas,
            inversePrimary = p.green,
            scrim = p.scrim,
            surfaceTint = p.card,
        )
    } else {
        lightColorScheme(
            primary = p.blue,
            onPrimary = Color.White,
            primaryContainer = p.navyPanel,
            onPrimaryContainer = p.navyDeep,
            secondary = p.green,
            onSecondary = Color.White,
            secondaryContainer = p.greenGlow,
            onSecondaryContainer = p.green,
            tertiary = p.gold,
            onTertiary = Color.White,
            tertiaryContainer = p.goldGlow,
            onTertiaryContainer = p.gold,
            background = p.canvas,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            onSurfaceVariant = p.textSecondary,
            surfaceVariant = p.cardAlt,
            surfaceContainerLowest = p.canvas,
            surfaceContainerLow = p.surface,
            surfaceContainer = p.surfaceRaised,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.cardAlt,
            error = p.error,
            onError = Color.White,
            errorContainer = p.error.copy(alpha = 0.12f),
            onErrorContainer = p.error,
            outline = p.steel,
            outlineVariant = p.cardAlt,
            inverseSurface = p.textPrimary,
            inverseOnSurface = p.canvas,
            inversePrimary = p.green,
            scrim = p.scrim,
            surfaceTint = p.card,
        )
    }
}

/** Resolved appearance for the current composition (mode + palette). */
data class LedgerAppearance(
    val mode: LedgerThemeMode,
    val palette: LedgerColorPalette,
    val dark: Boolean,
)

val LocalLedgerAppearance = staticCompositionLocalOf {
    LedgerAppearance(LedgerThemeMode.DARK, LedgerColorPalette.SOVEREIGN, dark = true)
}

/**
 * Root theme of the shared Compose Multiplatform app (single design source
 * for Android + iOS). Applies the palette selected in Account → Appearance;
 * SYSTEM follows the platform setting.
 */
@Composable
fun ExchangeCurrencyAppTheme(
    themeMode: LedgerThemeMode = LedgerThemeMode.DARK,
    colorPalette: LedgerColorPalette = LedgerColorPalette.SOVEREIGN,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        LedgerThemeMode.DARK -> true
        LedgerThemeMode.LIGHT -> false
        LedgerThemeMode.SYSTEM -> systemDark
    }
    val resolved = remember(dark, colorPalette) { resolvePalette(themeMode, colorPalette, systemDark) }
    val scheme = remember(resolved, dark) { ledgerColorScheme(resolved, dark) }

    CompositionLocalProvider(
        LocalLedgerPalette provides resolved,
        LocalLedgerAppearance provides LedgerAppearance(themeMode, colorPalette, dark),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ledgerTypography(),
            shapes = shapes,
            content = content,
        )
    }
}
