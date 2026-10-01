package dali.hamza.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Ledger line chart (design `72Xe2` historical graph): green rate line with
 * a gradient area fill over faint horizontal grid lines. Points are the
 * series' rates, left (oldest) → right (newest).
 */
@Composable
fun LedgerLineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerColors.Green,
    gridCount: Int = 4,
) {
    Canvas(modifier = modifier.fillMaxWidth()) {
        if (points.size < 2) return@Canvas

        val minValue = points.min()
        val maxValue = points.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: maxValue.takeIf { it > 0.0 } ?: 1.0
        val minPadding = span * 0.10f
        val yMin = minValue - minPadding
        val yMax = maxValue + minPadding
        val ySpan = (yMax - yMin).takeIf { it > 0.0 } ?: 1.0

        // grid lines (design: hairline white, evenly spread)
        val gridColor = Color.White.copy(alpha = 0.07f)
        repeat(gridCount) { index ->
            val y = size.height * (index + 1) / (gridCount + 1)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        fun xFor(index: Int): Float =
            size.width * index / (points.size - 1)

        fun yFor(value: Double): Float =
            (size.height * (1f - ((value - yMin) / ySpan).toFloat())).coerceIn(0f, size.height)

        // area fill
        val area = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { index, value ->
                lineTo(xFor(index), yFor(value))
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.0f)),
                startY = 0f,
                endY = size.height,
            ),
        )

        // rate line
        val line = Path().apply {
            points.forEachIndexed { index, value ->
                val x = xFor(index)
                val y = yFor(value)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx()),
        )

        // latest-point marker
        val lastIndex = points.size - 1
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(xFor(lastIndex), yFor(points[lastIndex])),
        )
    }
}
