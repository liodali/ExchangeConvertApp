package dali.hamza.shared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sovereign Ledger palette — extracted from `ui-design.pen` (see
 * `plans/redesign-migration-strategy.md` §1.3, the design spec of record).
 *
 * Dark-only theme (v1): the design file defines a single dark palette.
 * Semantic M3 roles ([LedgerColors] in Theme.kt) map onto these values so
 * screens keep using `MaterialTheme.colorScheme.*` exclusively.
 */
object LedgerColors {

    // ---- Backgrounds (layered) ----
    val Canvas = Color(0xFF0E0E0E)          // app background (deepest)
    val Surface = Color(0xFF131313)         // screen scaffold
    val SurfaceElevated = Color(0xFF1C1B1B) // raised panels
    val SurfaceRaised = Color(0xFF201F1F)   // cards / active nav pill

    // ---- Card / surface fills ----
    val Card = Color(0xFF2A2A2A)
    val CardAlt = Color(0xFF353534)
    val CardHigh = Color(0xFF393939)

    // ---- Text ----
    val TextPrimary = Color(0xFFE5E2E1)     // warm white
    val TextOnColor = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFC5C6CD)
    val TextTertiary = Color(0xFF8F9097)

    // ---- Muted labels / dividers-adjacent ----
    val TextMuted = Color(0xFF64748B)
    val TextMutedAlt = Color(0xFF74829D)

    // ---- Deep navy info surfaces ----
    val NavyPanel = Color(0xFF0D1C32)
    val NavyDeep = Color(0xFF0A192F)
    val Steel = Color(0xFF44474D)

    // ---- Accents ----
    val Green = Color(0xFF4EDEA3)           // positive change, live indicators
    val Gold = Color(0xFFE9C349)            // sovereign/premium highlights
    val Blue = Color(0xFFB9C7E4)            // info accents (balance, finance icons)
    val BlueSoft = Color(0xFFD6E3FF)

    // ---- Status ----
    val Error = Color(0xFFFFB4AB)

    // ---- Overlays ----
    val Scrim = Color(0xFF020617)

    // ---- Alpha helpers (glows / glass tints used by the design) ----
    val GreenGlow = Green.copy(alpha = 0.12f)   // #4edea31a-ish
    val GreenSoft = Green.copy(alpha = 0.20f)   // #4edea333
    val GoldGlow = Gold.copy(alpha = 0.12f)     // #e9c3491a
    val BlueGlow = Blue.copy(alpha = 0.12f)     // #b9c7e41a
    val SurfaceBlur = Surface.copy(alpha = 0.70f) // #131313b2 — translucent nav/app bars
}
