package dali.hamza.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Sovereign Ledger dark color scheme — the design (`ui-design.pen`) is dark-only (v1),
 * so [ExchangeCurrencyAppTheme] always applies this scheme regardless of the system
 * setting (the `darkTheme` parameter is kept for API stability).
 *
 * Primary = light blue `#B9C7E4` on navy — the design's CTA/active-accent pairing.
 * Secondary = green `#4EDEA3` (positive/live), tertiary = gold `#E9C349` (premium).
 */
val LedgerDarkColors = darkColorScheme(
    primary = LedgerColors.Blue,
    onPrimary = LedgerColors.NavyPanel,
    primaryContainer = LedgerColors.NavyPanel,
    onPrimaryContainer = LedgerColors.BlueSoft,
    secondary = LedgerColors.Green,
    onSecondary = LedgerColors.Canvas,
    secondaryContainer = LedgerColors.GreenGlow,
    onSecondaryContainer = LedgerColors.Green,
    tertiary = LedgerColors.Gold,
    onTertiary = LedgerColors.Canvas,
    tertiaryContainer = LedgerColors.GoldGlow,
    onTertiaryContainer = LedgerColors.Gold,
    background = LedgerColors.Canvas,
    onBackground = LedgerColors.TextPrimary,
    surface = LedgerColors.Surface,
    onSurface = LedgerColors.TextPrimary,
    onSurfaceVariant = LedgerColors.TextSecondary,
    surfaceVariant = LedgerColors.SurfaceElevated,
    surfaceContainerLowest = LedgerColors.Canvas,
    surfaceContainerLow = LedgerColors.SurfaceElevated,
    surfaceContainer = LedgerColors.SurfaceRaised,
    surfaceContainerHigh = LedgerColors.Card,
    surfaceContainerHighest = LedgerColors.CardAlt,
    error = LedgerColors.Error,
    onError = LedgerColors.Canvas,
    errorContainer = LedgerColors.Error.copy(alpha = 0.12f),
    onErrorContainer = LedgerColors.Error,
    outline = LedgerColors.Steel,
    outlineVariant = LedgerColors.Card,
    inverseSurface = LedgerColors.TextPrimary,
    inverseOnSurface = LedgerColors.Canvas,
    inversePrimary = LedgerColors.Green,
    scrim = LedgerColors.Scrim,
    surfaceTint = LedgerColors.Card,
)

/**
 * Root theme of the shared Compose Multiplatform app (single design source for
 * Android + iOS). Signature unchanged from the pre-redesign version.
 */
@Composable
fun ExchangeCurrencyAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LedgerDarkColors,
        typography = ledgerTypography(),
        shapes = shapes,
        content = content,
    )
}
