package dali.hamza.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.ui.theme.LedgerColors
import kotlin.math.roundToInt

/**
 * Ledger line chart (design `72Xe2` historical graph): green rate line with
 * a gradient area fill over faint horizontal grid lines. Points are the
 * series' rates, left (oldest) → right (newest).
 *
 * Beyond the static drawing it behaves like a real chart:
 * - **Y-axis value labels** — the rate each grid line represents (right edge).
 * - **Scrubber** — tap or drag anywhere on the chart to inspect the exact
 *   rate and date at that point: hairline, enlarged dot and a floating
 *   value bubble that follows the finger. The selection persists after
 *   release so the value can be read.
 *
 * @param points series rates, oldest → newest
 * @param labels optional date (or category) labels parallel to [points],
 *   shown in the scrubber bubble
 */
@Composable
fun LedgerLineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerColors.Green,
    gridCount: Int = 4,
    labels: List<String> = emptyList(),
) {
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    // theme tokens are read in composable scope — DrawScope lambdas can't
    // read CompositionLocals, so capture the resolved colors up front
    val gridColor = LedgerColors.GridLine
    val canvasColor = LedgerColors.Canvas
    // scrub marker: the palette's tertiary (gold) — reads on dark AND light
    val scrubColor = LedgerColors.Gold
    val hairlineColor = LedgerColors.Gold.copy(alpha = 0.55f)
    val bubbleBg = LedgerColors.SurfaceElevated.copy(alpha = 0.98f)
    val bubbleBorder = LedgerColors.Gold.copy(alpha = 0.40f)

    val axisStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = LedgerColors.TextSecondary.copy(alpha = 0.55f),
    )
    val bubbleDateStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        color = LedgerColors.TextSecondary,
    )
    val bubbleRateStyle = MaterialTheme.typography.labelMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = LedgerColors.TextOnColor,
    )

    fun indexForX(x: Float, plotWidth: Float): Int {
        if (points.size < 2) return 0
        val fraction = (x / plotWidth).coerceIn(0f, 1f)
        return (fraction * (points.size - 1)).roundToInt()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    scrubIndex = indexForX(offset.x, size.width.toFloat())
                }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubIndex = indexForX(offset.x, size.width.toFloat())
                    },
                    onDragEnd = { /* keep the selection for reading */ },
                ) { change, _ ->
                    scrubIndex = indexForX(change.position.x, size.width.toFloat())
                    change.consume()
                }
            },
    ) {
        if (points.size < 2) return@Canvas

        val minValue = points.min()
        val maxValue = points.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: maxValue.takeIf { it > 0.0 } ?: 1.0
        val minPadding = span * 0.10f
        val yMin = minValue - minPadding
        val yMax = maxValue + minPadding
        val ySpan = (yMax - yMin).takeIf { it > 0.0 } ?: 1.0

        // axis gutter: width of the widest y label + breathing room
        val sampleLabels = listOf(formatRateValue(yMin), formatRateValue(yMax))
        val axisWidth = sampleLabels.maxOf {
            textMeasurer.measure(it, axisStyle).size.width
        } + 10.dp.toPx()
        val plotRight = (size.width - axisWidth).coerceAtLeast(size.width * 0.6f)
        val plotWidth = plotRight

        fun xFor(index: Int): Float = plotWidth * index / (points.size - 1)

        fun yFor(value: Double): Float =
            (size.height * (1f - ((value - yMin) / ySpan).toFloat())).coerceIn(0f, size.height)

        fun rateAtY(y: Float): Double = yMin + (1f - y / size.height) * ySpan

        // grid lines (design: hairline, evenly spread) + y value labels
        repeat(gridCount) { index ->
            val y = size.height * (index + 1) / (gridCount + 1)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(plotWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = formatRateValue(rateAtY(y)),
                style = axisStyle,
                topLeft = Offset(plotWidth + 4.dp.toPx(), y - axisStyle.fontSize.toPx() * 0.6f),
            )
        }
        // top (max) and bottom (min) edge labels
        drawText(
            textMeasurer = textMeasurer,
            text = formatRateValue(yMax),
            style = axisStyle,
            topLeft = Offset(plotWidth + 4.dp.toPx(), 0f),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = formatRateValue(yMin),
            style = axisStyle,
            topLeft = Offset(
                plotWidth + 4.dp.toPx(),
                (size.height - axisStyle.fontSize.toPx() * 1.2f).coerceAtLeast(0f),
            ),
        )

        // area fill
        val area = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { index, value ->
                lineTo(xFor(index), yFor(value))
            }
            lineTo(plotWidth, size.height)
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

        // ---- scrubber ----------------------------------------------------
        val scrub = scrubIndex?.coerceIn(0, points.size - 1) ?: return@Canvas
        val scrubX = xFor(scrub)
        val scrubY = yFor(points[scrub])

        // vertical hairline through the selected day
        drawLine(
            color = hairlineColor,
            start = Offset(scrubX, 0f),
            end = Offset(scrubX, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
        )
        // enlarged dot with ring
        drawCircle(
            color = canvasColor,
            radius = 5.dp.toPx(),
            center = Offset(scrubX, scrubY),
        )
        drawCircle(
            color = scrubColor,
            radius = 4.dp.toPx(),
            center = Offset(scrubX, scrubY),
        )

        // value bubble: date + rate
        val dateText = labels.getOrNull(scrub)
        val dateLayout = dateText?.let { textMeasurer.measure(it, bubbleDateStyle) }
        val rateLayout = textMeasurer.measure(formatRateValue(points[scrub]), bubbleRateStyle)

        val bubblePadH = 10.dp.toPx()
        val bubblePadV = 8.dp.toPx()
        val bubbleGap = 2.dp.toPx()
        val bubbleW = (maxOf(dateLayout?.size?.width ?: 0, rateLayout.size.width)
            + bubblePadH * 2).coerceAtLeast(56.dp.toPx())
        val bubbleH = (dateLayout?.size?.height ?: 0) +
            (if (dateLayout != null) bubbleGap else 0f) +
            rateLayout.size.height + bubblePadV * 2

        val bubbleX = (scrubX - bubbleW / 2)
            .coerceIn(0f, (plotWidth - bubbleW).coerceAtLeast(0f))
        val bubbleTop = scrubY - 10.dp.toPx() - bubbleH
        val bubbleY = if (bubbleTop >= 0f) bubbleTop else (scrubY + 10.dp.toPx()).coerceAtMost(size.height - bubbleH)

        drawRoundRect(
            color = bubbleBg,
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleW, bubbleH),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
        drawRoundRect(
            color = bubbleBorder,
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleW, bubbleH),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )

        var textY = bubbleY + bubblePadV
        dateLayout?.let {
            drawText(
                it,
                topLeft = Offset(bubbleX + (bubbleW - it.size.width) / 2f, textY),
            )
            textY += it.size.height + bubbleGap
        }
        drawText(
            rateLayout,
            topLeft = Offset(bubbleX + (bubbleW - rateLayout.size.width) / 2f, textY),
        )
    }
}

/** Rate display: 4 decimals ≥ 1, 6 below (small cross rates) — matches screens. */
private fun formatRateValue(value: Double): String {
    val abs = kotlin.math.abs(value)
    val precision = if (abs >= 1.0) 4 else 6
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val rounded = kotlin.math.round(abs * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}
