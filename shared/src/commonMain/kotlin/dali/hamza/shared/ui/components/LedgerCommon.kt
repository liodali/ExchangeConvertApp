package dali.hamza.shared.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Horizontal gap helper used across Ledger components — a fixed-width [Spacer]
 * for inline (Row) spacing between icons and labels.
 */
@Composable
fun RowScope.Gap(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}
