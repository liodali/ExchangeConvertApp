package dali.hamza.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Sovereign Ledger logo mark — abstract rate-line rising left→right with a
 * mid-dip and an endpoint dot (see `design/logo.svg`). Drawn on Canvas so it
 * stays crisp at any size on both platforms — used as the leading mark in
 * the Ledger top bar.
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
        val stroke = Stroke(
            width = w * 0.065f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        // design/logo.svg geometry, normalized to the canvas
        val p1 = Offset(w * 0.26f, h * 0.67f)
        val p2 = Offset(w * 0.43f, h * 0.50f)
        val p3 = Offset(w * 0.56f, h * 0.57f)
        val p4 = Offset(w * 0.69f, h * 0.43f)
        drawLine(tint, p1, p2, stroke.width, StrokeCap.Round)
        drawLine(tint, p2, p3, stroke.width, StrokeCap.Round)
        drawLine(tint, p3, p4, stroke.width, StrokeCap.Round)
        drawCircle(color = tint, radius = w * 0.065f, center = Offset(w * 0.76f, h * 0.33f))
    }
}
