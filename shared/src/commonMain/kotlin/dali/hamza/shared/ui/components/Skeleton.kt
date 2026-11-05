package dali.hamza.shared.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Skeleton loading primitives — placeholder UI that mirrors the geometry of
 * the real components (market cards, charts, text lines) while data loads,
 * so the layout never reflows when content arrives.
 *
 * Design notes:
 *  - the base tone is [LedgerColors.CardHigh] (one step above the card fill)
 *    so skeletons read as "content-shaped holes" in both light and dark
 *  - a light band ([LedgerColors.TextMuted] @ 45%) sweeps left → right in a
 *    1.4s loop — the classic shimmer that says "loading", never "error"
 *  - every box runs its own transition; slightly different element widths
 *    naturally desynchronize the sweeps, which reads organic
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    var widthPx by remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
        ),
        label = "shimmerShift",
    )
    val band = (widthPx * 0.6f).coerceAtLeast(48f)
    // sweep the band from off-left to off-right so the loop never pops
    val startX = -band + shift * (widthPx + 2f * band)
    val base = LedgerColors.CardHigh
    val highlight = LedgerColors.TextMuted.copy(alpha = 0.45f)
    Box(
        modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to base,
                        0.5f to highlight,
                        1f to base,
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + band, band.coerceAtMost(48f)),
                ),
            ),
    )
}

/**
 * Fake-chart silhouette: a row of skeleton bars with a fixed rising-then-
 * dipping height profile. Reads as "a chart is coming" instead of an
 * anonymous rectangle. Heights are a constant — no Random, so the shape is
 * stable across recompositions and identical on every platform.
 */
private val CHART_PROFILE = listOf(
    0.34f, 0.42f, 0.38f, 0.50f, 0.46f, 0.58f, 0.52f, 0.66f,
    0.60f, 0.72f, 0.64f, 0.78f, 0.70f, 0.82f, 0.74f, 0.88f,
    0.80f, 0.92f, 0.84f, 0.96f, 0.88f, 0.78f, 0.90f, 1.00f,
)

@Composable
fun ChartSkeleton(
    modifier: Modifier = Modifier,
    barCount: Int = 24,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        val profile = CHART_PROFILE.subList(0, barCount.coerceIn(4, CHART_PROFILE.size))
        profile.forEach { fraction ->
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction),
                cornerRadius = 2.dp,
            )
        }
    }
}

/**
 * Skeleton twin of the Home market card (PairCard): same BentoCard shell,
 * same 20dp padding, same title/subtitle/delta-chip/bell row, same 30sp
 * rate line, same 64dp chart slot — so the swap from skeleton to data is
 * pixel-stable and nothing jumps.
 */
@Composable
fun MarketCardSkeleton(
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier,
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // pair title (EUR/USD, bold 18sp)
                ShimmerBox(modifier = Modifier.size(width = 84.dp, height = 18.dp))
                Spacer(Modifier.height(6.dp))
                // currency name (bodySmall)
                ShimmerBox(modifier = Modifier.size(width = 56.dp, height = 12.dp))
            }
            // 7-day delta chip
            ShimmerBox(
                modifier = Modifier.size(width = 52.dp, height = 26.dp),
                cornerRadius = 4.dp,
            )
            Spacer(Modifier.width(12.dp))
            // alert bell slot (36dp, 12dp corner — same as the real one)
            ShimmerBox(
                modifier = Modifier.size(36.dp),
                cornerRadius = 12.dp,
            )
        }
        // live rate (bold 30sp)
        Spacer(Modifier.height(16.dp))
        ShimmerBox(
            modifier = Modifier.size(width = 128.dp, height = 30.dp),
            cornerRadius = 8.dp,
        )
        // sparkline slot
        Spacer(Modifier.height(16.dp))
        ChartSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        )
    }
}
