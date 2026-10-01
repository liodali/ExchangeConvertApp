package dali.hamza.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Sovereign Ledger logo mark v2 — ascending gold rate bars on a steel
 * baseline (see `design/logo.svg`): an unambiguous market-chart mark.
 * Drawn on Canvas so it stays crisp at any size on both platforms —
 * the leading mark of the Ledger top bar.
 */
@Composable
fun LedgerLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LedgerColors.Gold,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * 0.13f
        val radius = CornerRadius(barWidth * 0.36f, barWidth * 0.36f)
        val gap = w * 0.075f
        val baselineY = h * 0.80f

        // baseline (steel)
        drawLine(
            color = LedgerColors.Steel,
            start = Offset(w * 0.22f, baselineY),
            end = Offset(w * 0.85f, baselineY),
            strokeWidth = w * 0.028f,
        )

        // three ascending bars, bottom-aligned on the baseline
        val heights = listOf(0.26f, 0.41f, 0.56f)
        heights.forEachIndexed { index, fraction ->
            val barHeight = h * fraction
            val left = w * 0.26f + index * (barWidth + gap)
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, baselineY - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}
