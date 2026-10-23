package dali.hamza.shared.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive window classes for the tablet/iPad shell (plans/tablet-ui.md).
 *
 * Measured from the live window width — never from device type — so
 * foldables (Apple's folding duo included) reflow live: folded ≈ COMPACT,
 * unfolded ≈ MEDIUM, iPad landscape ≈ EXPANDED.
 */
enum class LedgerWindowMode {
    /** Phones (and folded duos) — bottom navigation, single column. */
    COMPACT,

    /** Small tablets / unfoldables — design rail + single column. */
    MEDIUM,

    /** iPad-class — design rail + two-column workspaces. */
    EXPANDED,
}

/** Material3-style breakpoints over the current window width. */
fun ledgerWindowMode(width: Dp): LedgerWindowMode = when {
    width < 600.dp -> LedgerWindowMode.COMPACT
    width < 840.dp -> LedgerWindowMode.MEDIUM
    else -> LedgerWindowMode.EXPANDED
}

/** True when the design navigation rail replaces the bottom bar. */
val LedgerWindowMode.useRail: Boolean
    get() = this != LedgerWindowMode.COMPACT

/** True for two-column workspaces (Home tools column, Rates side panel). */
val LedgerWindowMode.isExpanded: Boolean
    get() = this == LedgerWindowMode.EXPANDED

/** Provides [content] with the [LedgerWindowMode] of the current window. */
@Composable
fun LedgerWindowMode(content: @Composable BoxScope.(LedgerWindowMode) -> Unit) {
    // fillMaxSize: without it the box wraps its content and both the
    // reported maxWidth and any child alignment (e.g. centered onboarding
    // columns) collapse to the content's own width — centering silently
    // becomes flush-left.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        content(ledgerWindowMode(maxWidth))
    }
}
