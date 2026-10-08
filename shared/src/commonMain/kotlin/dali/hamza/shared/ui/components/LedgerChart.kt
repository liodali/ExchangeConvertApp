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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
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
 * Chart rendering style (Account → Appearance): the historical graph renders
 * as the rate line (design `72Xe2`) or as bottom-aligned bars (design
 * `ATVsN` mobile dashboard graph). Persisted as a storage string.
 */
enum class LedgerChartStyle(val stored: String, val label: String) {
    LINE("line", "Line"),
    BAR("bar", "Bar");

    companion object {
        fun fromStored(value: String?): LedgerChartStyle =
            entries.firstOrNull { it.stored == value } ?: LINE
    }
}

/** Dispatch chart — renders the line or bar variant per the user preference. */
@Composable
fun LedgerChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    style: LedgerChartStyle = LedgerChartStyle.LINE,
    gridCount: Int = 4,
    labels: List<String> = emptyList(),
) {
    when (style) {
        LedgerChartStyle.LINE -> LedgerLineChart(
            points = points,
            modifier = modifier,
            gridCount = gridCount,
            labels = labels,
        )
        LedgerChartStyle.BAR -> LedgerBarChart(
            points = points,
            modifier = modifier,
            gridCount = gridCount,
            labels = labels,
        )
    }
}

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
        color = LedgerColors.TextPrimary,
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

        val scale = chartScale(points)
        val axisWidth = axisGutterWidth(textMeasurer, axisStyle, scale)
        val plotWidth = (size.width - axisWidth).coerceAtLeast(size.width * 0.6f)

        fun xFor(index: Int): Float = plotWidth * index / (points.size - 1)

        fun yFor(value: Double): Float =
            (size.height * (1f - yFraction(value, scale))).coerceIn(0f, size.height)

        drawGridAndAxis(
            textMeasurer = textMeasurer,
            gridCount = gridCount,
            scale = scale,
            plotWidth = plotWidth,
            gridColor = gridColor,
            axisStyle = axisStyle,
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

        drawScrubBubble(
            textMeasurer = textMeasurer,
            anchorX = scrubX,
            anchorY = scrubY,
            plotWidth = plotWidth,
            rate = points[scrub],
            dateText = labels.getOrNull(scrub),
            dateStyle = bubbleDateStyle,
            rateStyle = bubbleRateStyle,
            background = bubbleBg,
            border = bubbleBorder,
        )
    }
}

/**
 * Ledger bar chart (design `ATVsN` mobile dashboard graph): bottom-aligned
 * rounded bars over the same faint grid as the line chart. The most recent
 * [highlightCount] bars render in the palette accent while older ones stay
 * muted — the design's `#3B7654` → `#4EDEA3` ramp expressed with theme
 * tokens, so it follows the active palette and light/dark mode.
 *
 * Shares the line chart's y-axis labels and scrubber: tap or drag to
 * inspect the exact rate and date of a bar.
 *
 * @param points series rates, oldest → newest
 * @param labels optional date (or category) labels parallel to [points],
 *   shown in the scrubber bubble
 * @param highlightCount how many of the newest bars use the accent color
 */
@Composable
fun LedgerBarChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    barColor: Color = LedgerColors.Green,
    gridCount: Int = 4,
    labels: List<String> = emptyList(),
    highlightCount: Int = 3,
) {
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val gridColor = LedgerColors.GridLine
    val scrubColor = LedgerColors.Gold
    val hairlineColor = LedgerColors.Gold.copy(alpha = 0.55f)
    val bubbleBg = LedgerColors.SurfaceElevated.copy(alpha = 0.98f)
    val bubbleBorder = LedgerColors.Gold.copy(alpha = 0.40f)
    val mutedBarColor = barColor.copy(alpha = 0.45f)

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
        color = LedgerColors.TextPrimary,
    )

    fun indexForX(x: Float, plotWidth: Float): Int {
        if (points.size < 2) return 0
        val fraction = (x / plotWidth).coerceIn(0f, 1f)
        return (fraction * points.size).roundToInt().coerceIn(0, points.size - 1)
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

        val scale = chartScale(points)
        val axisWidth = axisGutterWidth(textMeasurer, axisStyle, scale)
        val plotWidth = (size.width - axisWidth).coerceAtLeast(size.width * 0.6f)

        fun xCenterFor(index: Int): Float = plotWidth * (index + 0.5f) / points.size

        fun yFor(value: Double): Float =
            (size.height * (1f - yFraction(value, scale))).coerceIn(0f, size.height)

        drawGridAndAxis(
            textMeasurer = textMeasurer,
            gridCount = gridCount,
            scale = scale,
            plotWidth = plotWidth,
            gridColor = gridColor,
            axisStyle = axisStyle,
        )

        // bars (design: 11dp wide, 3dp rounded, bottom-aligned; thinner
        // slots with long series shrink the width instead of overlapping)
        val slot = plotWidth / points.size
        val barWidth = minOf(11.dp.toPx(), slot * 0.62f)
        val cornerRadius = CornerRadius(minOf(3.dp.toPx(), barWidth / 2f))
        val minBarHeight = 3.dp.toPx()
        val accentFrom = points.size - highlightCount.coerceIn(0, points.size)

        points.forEachIndexed { index, value ->
            val top = yFor(value)
            val height = (size.height - top).coerceAtLeast(minBarHeight)
            drawRoundRect(
                color = if (index >= accentFrom) barColor else mutedBarColor,
                topLeft = Offset(xCenterFor(index) - barWidth / 2f, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = cornerRadius,
            )
        }

        // ---- scrubber ----------------------------------------------------
        val scrub = scrubIndex?.coerceIn(0, points.size - 1) ?: return@Canvas
        val scrubX = xCenterFor(scrub)
        val scrubY = yFor(points[scrub])

        // vertical hairline through the selected bar
        drawLine(
            color = hairlineColor,
            start = Offset(scrubX, 0f),
            end = Offset(scrubX, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
        )
        // gold outline around the selected bar
        val selectedTop = yFor(points[scrub])
        val selectedHeight = (size.height - selectedTop).coerceAtLeast(minBarHeight)
        drawRoundRect(
            color = scrubColor,
            topLeft = Offset(scrubX - barWidth / 2f, size.height - selectedHeight),
            size = Size(barWidth, selectedHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        drawScrubBubble(
            textMeasurer = textMeasurer,
            anchorX = scrubX,
            anchorY = scrubY,
            plotWidth = plotWidth,
            rate = points[scrub],
            dateText = labels.getOrNull(scrub),
            dateStyle = bubbleDateStyle,
            rateStyle = bubbleRateStyle,
            background = bubbleBg,
            border = bubbleBorder,
        )
    }
}

// ---- shared chart drawing helpers -----------------------------------------

/** Y-range shared by both charts: min/max padded by 10% of the span. */
private class ChartScale(val yMin: Double, val yMax: Double) {
    val ySpan: Double = (yMax - yMin).takeIf { it > 0.0 } ?: 1.0
}

private fun chartScale(points: List<Double>): ChartScale {
    val minValue = points.min()
    val maxValue = points.max()
    val span = (maxValue - minValue).takeIf { it > 0.0 } ?: maxValue.takeIf { it > 0.0 } ?: 1.0
    val minPadding = span * 0.10f
    return ChartScale(minValue - minPadding, maxValue + minPadding)
}

private fun yFraction(value: Double, scale: ChartScale): Float =
    ((value - scale.yMin) / scale.ySpan).toFloat()

/** Axis gutter: width of the widest y label + breathing room. */
private fun DrawScope.axisGutterWidth(
    textMeasurer: TextMeasurer,
    axisStyle: TextStyle,
    scale: ChartScale,
): Float =
    listOf(formatRateValue(scale.yMin), formatRateValue(scale.yMax)).maxOf {
        textMeasurer.measure(it, axisStyle).size.width
    } + 10.dp.toPx()

/** Hairline grid + the rate each line represents (right edge). */
private fun DrawScope.drawGridAndAxis(
    textMeasurer: TextMeasurer,
    gridCount: Int,
    scale: ChartScale,
    plotWidth: Float,
    gridColor: Color,
    axisStyle: TextStyle,
) {
    fun yFor(value: Double): Float =
        (size.height * (1f - yFraction(value, scale))).coerceIn(0f, size.height)

    fun rateAtY(y: Float): Double = scale.yMin + (1f - y / size.height) * scale.ySpan

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
        text = formatRateValue(scale.yMax),
        style = axisStyle,
        topLeft = Offset(plotWidth + 4.dp.toPx(), 0f),
    )
    drawText(
        textMeasurer = textMeasurer,
        text = formatRateValue(scale.yMin),
        style = axisStyle,
        topLeft = Offset(
            plotWidth + 4.dp.toPx(),
            (size.height - axisStyle.fontSize.toPx() * 1.2f).coerceAtLeast(0f),
        ),
    )
}

/** Floating scrubber bubble: date + rate, gold border, clamped to the plot. */
private fun DrawScope.drawScrubBubble(
    textMeasurer: TextMeasurer,
    anchorX: Float,
    anchorY: Float,
    plotWidth: Float,
    rate: Double,
    dateText: String?,
    dateStyle: TextStyle,
    rateStyle: TextStyle,
    background: Color,
    border: Color,
) {
    val dateLayout: TextLayoutResult? =
        dateText?.let { textMeasurer.measure(it, dateStyle) }
    val rateLayout = textMeasurer.measure(formatRateValue(rate), rateStyle)

    val bubblePadH = 10.dp.toPx()
    val bubblePadV = 8.dp.toPx()
    val bubbleGap = 2.dp.toPx()
    val bubbleW = (maxOf(dateLayout?.size?.width ?: 0, rateLayout.size.width)
        + bubblePadH * 2).coerceAtLeast(56.dp.toPx())
    val bubbleH = (dateLayout?.size?.height ?: 0) +
        (if (dateLayout != null) bubbleGap else 0f) +
        rateLayout.size.height + bubblePadV * 2

    val bubbleX = (anchorX - bubbleW / 2)
        .coerceIn(0f, (plotWidth - bubbleW).coerceAtLeast(0f))
    val bubbleTop = anchorY - 10.dp.toPx() - bubbleH
    val bubbleY = if (bubbleTop >= 0f) bubbleTop else {
        (anchorY + 10.dp.toPx()).coerceAtMost(size.height - bubbleH)
    }

    drawRoundRect(
        color = background,
        topLeft = Offset(bubbleX, bubbleY),
        size = Size(bubbleW, bubbleH),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    drawRoundRect(
        color = border,
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
