package dali.hamza.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Sparkline — the design's mini rate charts (dashboard "Top Pairs" cards):
 * a smoothed line with an optional vertical gradient fill underneath, or
 * (bar style, matching [LedgerBarChart]) bottom-aligned rounded bars with
 * the newest values in the full color and older ones muted.
 *
 * Values are normalized to the available space; fewer than 2 points draw nothing.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = LedgerColors.Green,
    fillUnder: Boolean = true,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    style: LedgerChartStyle = LedgerChartStyle.LINE,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val paddingY = size.height * 0.08f
        val usableHeight = size.height - paddingY * 2
        val stepX = size.width / (values.size - 1)

        if (style == LedgerChartStyle.BAR) {
            // same ramp as the big chart: newest 3 bars full, older muted
            val slot = size.width / values.size
            val barWidth = minOf(8.dp.toPx(), slot * 0.55f)
            val cornerRadius = CornerRadius(minOf(1.5.dp.toPx(), barWidth / 2f))
            val minBarHeight = 2.dp.toPx()
            val accentFrom = values.size - 3.coerceIn(0, values.size)
            values.forEachIndexed { index, value ->
                val y = paddingY + (usableHeight * (1.0 - (value - minValue) / range)).toFloat()
                val height = (size.height - y).coerceAtLeast(minBarHeight)
                drawRoundRect(
                    color = if (index >= accentFrom) color else color.copy(alpha = 0.45f),
                    topLeft = Offset(slot * (index + 0.5f) - barWidth / 2f, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = cornerRadius,
                )
            }
            return@Canvas
        }

        val points = values.mapIndexed { index, value ->
            val x = stepX * index
            // Invert Y: larger values higher on screen
            val y = paddingY + (usableHeight * (1.0 - (value - minValue) / range)).toFloat()
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            // Midpoint cubic smoothing (S-curves between points)
            for (i in 1 until points.size) {
                val previous = points[i - 1]
                val current = points[i]
                val controlX = (previous.x + current.x) / 2f
                cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
            }
        }

        if (fillUnder) {
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.25f),
                        color.copy(alpha = 0.0f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
            )
        }

        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
