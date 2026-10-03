package dali.hamza.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sovereign Ledger appearance system — extracted from `ui-design.pen`
 * (Account → Appearance, see the "Rates — Light/Summer/Bloom" frames).
 *
 * Two independent axes:
 * - **Mode**: System / Light / Dark
 * - **Palette**: Sovereign / Summer / Bloom (dark palettes; Light mode
 *   uses the Sovereign light tokens — design frame "Rates — Light Mode ·
 *   Sovereign").
 *
 * [LedgerColors] keeps its static-looking API but every token is now a
 * getter over [LocalLedgerPalette], so existing call sites re-theme
 * without changes.
 */

/** Appearance mode selected in Account → Appearance. */
enum class LedgerThemeMode(val stored: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStored(value: String?): LedgerThemeMode =
            entries.firstOrNull { it.stored == value } ?: DARK
    }
}

/** Dark-mode color palette selected in Account → Appearance. */
enum class LedgerColorPalette(val stored: String, val label: String) {
    SOVEREIGN("sovereign", "Sovereign"),
    SUMMER("summer", "Summer"),
    BLOOM("bloom", "Bloom");

    companion object {
        fun fromStored(value: String?): LedgerColorPalette =
            entries.firstOrNull { it.stored == value } ?: SOVEREIGN
    }
}

/** One complete token set — a row in the design's theme variable table. */
data class LedgerPalette(
    // backgrounds (layered)
    val canvas: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceRaised: Color,
    val card: Color,
    val cardAlt: Color,
    val cardHigh: Color,
    // text
    val textPrimary: Color,
    val textOnColor: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val textMutedAlt: Color,
    // info surfaces
    val navyPanel: Color,
    val navyDeep: Color,
    val steel: Color,
    // accents
    val green: Color,      // semantic accent (positive / live / brand CTA)
    val gold: Color,       // sovereign/premium highlights
    val blue: Color,       // info accents
    val blueSoft: Color,
    // status
    val error: Color,
    val scrim: Color,
    // alpha helpers
    val greenGlow: Color,
    val greenSoft: Color,
    val goldGlow: Color,
    val blueGlow: Color,
    val surfaceBlur: Color,
    // hairlines / chart grid
    val gridLine: Color,
    val borderSoft: Color,
    val glossTop: Color,
    val glossBottom: Color,
)

private fun darkPalette(
    background: Long,
    surfaceDeep: Long,
    elevated: Long,
    raised: Long,
    card: Long,
    cardAlt: Long,
    cardHigh: Long,
    text: Color,
    secondary: Color,
    muted: Color,
    border: Long,
    accent: Color,
    accentSoft: Color,
    info: Color,
    negative: Color,
    gold: Color = Color(0xFFE9C349),
): LedgerPalette = LedgerPalette(
    canvas = Color(background),
    surface = Color(surfaceDeep),
    surfaceElevated = Color(elevated),
    surfaceRaised = Color(raised),
    card = Color(card),
    cardAlt = Color(cardAlt),
    cardHigh = Color(cardHigh),
    textPrimary = text,
    textOnColor = Color.White,
    textSecondary = secondary,
    textTertiary = muted,
    textMuted = muted,
    textMutedAlt = secondary,
    navyPanel = Color(0xFF0D1C32),
    navyDeep = Color(0xFF0A192F),
    steel = Color(border),
    green = accent,
    gold = gold,
    blue = info,
    blueSoft = info.copy(alpha = 0.22f),
    error = negative,
    scrim = Color(0xFF020617),
    greenGlow = accent.copy(alpha = 0.12f),
    greenSoft = accentSoft,
    goldGlow = gold.copy(alpha = 0.12f),
    blueGlow = info.copy(alpha = 0.12f),
    surfaceBlur = Color(surfaceDeep).copy(alpha = 0.70f),
    gridLine = Color.White.copy(alpha = 0.10f),
    borderSoft = Color.White.copy(alpha = 0.08f),
    glossTop = Color.White.copy(alpha = 0.06f),
    glossBottom = Color.White.copy(alpha = 0.02f),
)

/** Sovereign dark — the original design palette (v1 token set). */
val SovereignDarkPalette = darkPalette(
    background = 0xFF0E0E0E,
    surfaceDeep = 0xFF131313,
    elevated = 0xFF1C1B1B,
    raised = 0xFF201F1F,
    card = 0xFF2A2A2A,
    cardAlt = 0xFF353534,
    cardHigh = 0xFF393939,
    text = Color(0xFFE5E2E1),
    secondary = Color(0xFFC5C6CD),
    muted = Color(0xFF8F9097),
    border = 0xFF44474D,
    accent = Color(0xFF4EDEA3),
    accentSoft = Color(0x334EDEA3),
    info = Color(0xFFB9C7E4),
    negative = Color(0xFFFFB4AB),
)

/** Summer — warm dark (design `theme-summer-*` variables). */
val SummerDarkPalette = darkPalette(
    background = 0xFF17140F,
    surfaceDeep = 0xFF1B1813,
    elevated = 0xFF211D16,
    raised = 0xFF26211A,
    card = 0xFF2B261C,
    cardAlt = 0xFF332C20,
    cardHigh = 0xFF382F22,
    text = Color(0xFFF7ECD5),
    secondary = Color(0xFFD4BE98),
    muted = Color(0xFFA29170),
    border = 0xFF4A3A25,
    accent = Color(0xFFF5BA59),
    accentSoft = Color(0x33F5BA59),
    info = Color(0xFF8BD3C7),
    negative = Color(0xFFFF8C78),
)

/** Bloom — violet dark (design `theme-bloom-*` variables). */
val BloomDarkPalette = darkPalette(
    background = 0xFF1A1420,
    surfaceDeep = 0xFF1F1826,
    elevated = 0xFF251B2B,
    raised = 0xFF2A1F31,
    card = 0xFF302337,
    cardAlt = 0xFF3B2C42,
    cardHigh = 0xFF423049,
    text = Color(0xFFF7EBF5),
    secondary = Color(0xFFD8C0D8),
    muted = Color(0xFFAD91AE),
    border = 0xFF503956,
    accent = Color(0xFFF2A9D0),
    accentSoft = Color(0x33F2A9D0),
    info = Color(0xFFB9A6E6),
    negative = Color(0xFFFF9D98),
)

/** Light · Sovereign (design `theme-light-*` variables, frame tW9BY). */
val SovereignLightPalette = LedgerPalette(
    canvas = Color(0xFFF5F7F2),
    surface = Color(0xFFFAFBF8),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardAlt = Color(0xFFEEF1EC),
    cardHigh = Color(0xFFEEF1EC),
    textPrimary = Color(0xFF18231D),
    textOnColor = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF4F5D54),
    textTertiary = Color(0xFF68746D),
    textMuted = Color(0xFF68746D),
    textMutedAlt = Color(0xFF4F5D54),
    navyPanel = Color(0xFFEEF2FA),
    navyDeep = Color(0xFFE3EAF6),
    steel = Color(0xFFDDE5DD),
    green = Color(0xFF167A55),
    gold = Color(0xFF8A6A12),
    blue = Color(0xFF4C6E9E),
    blueSoft = Color(0xFFD6E3FF),
    error = Color(0xFFB74740),
    scrim = Color(0xFF0F172A),
    greenGlow = Color(0xFFDDF2E5),
    greenSoft = Color(0x33167A55),
    goldGlow = Color(0x128A6A12),
    blueGlow = Color(0x124C6E9E),
    surfaceBlur = Color(0xFFFAFBF8).copy(alpha = 0.70f),
    gridLine = Color(0xFF18231D).copy(alpha = 0.10f),
    borderSoft = Color(0xFF18231D).copy(alpha = 0.10f),
    glossTop = Color(0xFF18231D).copy(alpha = 0.04f),
    glossBottom = Color(0xFF18231D).copy(alpha = 0.015f),
)

/** Palette for the current [mode] — Light always resolves to Sovereign light. */
fun resolvePalette(mode: LedgerThemeMode, palette: LedgerColorPalette, systemDark: Boolean): LedgerPalette {
    val dark = when (mode) {
        LedgerThemeMode.DARK -> true
        LedgerThemeMode.LIGHT -> false
        LedgerThemeMode.SYSTEM -> systemDark
    }
    return if (dark) {
        when (palette) {
            LedgerColorPalette.SOVEREIGN -> SovereignDarkPalette
            LedgerColorPalette.SUMMER -> SummerDarkPalette
            LedgerColorPalette.BLOOM -> BloomDarkPalette
        }
    } else {
        SovereignLightPalette
    }
}

/** Provided by [ExchangeCurrencyAppTheme]; defaults to Sovereign dark. */
val LocalLedgerPalette = staticCompositionLocalOf { SovereignDarkPalette }

/**
 * Semantic token accessors (design roles, `ui-design.pen` of record).
 * Every token reads the active [LedgerPalette] — screens never change.
 * Getters mirror `MaterialTheme.colorScheme` (@Composable + read-only).
 */
object LedgerColors {

    // ---- Backgrounds (layered) ----
    val Canvas: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.canvas
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.surface
    val SurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.surfaceElevated
    val SurfaceRaised: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.surfaceRaised

    // ---- Card / surface fills ----
    val Card: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.card
    val CardAlt: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.cardAlt
    val CardHigh: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.cardHigh

    // ---- Text ----
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textPrimary
    val TextOnColor: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textOnColor
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textTertiary

    // ---- Muted labels / dividers-adjacent ----
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textMuted
    val TextMutedAlt: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.textMutedAlt

    // ---- Deep navy info surfaces ----
    val NavyPanel: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.navyPanel
    val NavyDeep: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.navyDeep
    val Steel: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.steel

    // ---- Accents ----
    val Green: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.green
    val Gold: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.gold
    val Blue: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.blue
    val BlueSoft: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.blueSoft

    // ---- Status ----
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.error

    // ---- Overlays ----
    val Scrim: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.scrim

    // ---- Alpha helpers (glows / glass tints used by the design) ----
    val GreenGlow: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.greenGlow
    val GreenSoft: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.greenSoft
    val GoldGlow: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.goldGlow
    val BlueGlow: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.blueGlow
    val SurfaceBlur: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.surfaceBlur

    // ---- Hairlines / chart ----
    val GridLine: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.gridLine
    val BorderSoft: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.borderSoft
    val GlossTop: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.glossTop
    val GlossBottom: Color
        @Composable @ReadOnlyComposable get() = LocalLedgerPalette.current.glossBottom
}
