package dali.hamza.shared.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Skeleton loading primitives — placeholder UI that mirrors the geometry of
 * the real components (market cards, charts, text lines) while data loads,
 * so the layout never reflows when content arrives.
 *
 * Performance model: [ShimmerHost] runs ONE infinite transition per skeleton
 * section and exposes the progress via [LocalShimmerProgress]. Placeholder
 * boxes read that state only inside [Modifier.drawBehind] (drawing phase):
 * animation frames re-draw without recomposition, layout, or per-frame
 * allocations — the sweep is drawn with plain color rects, not rebuilt
 * gradient brushes.
 */
internal val LocalShimmerProgress = staticCompositionLocalOf<State<Float>?> { null }

/**
 * Runs a single shimmer clock for [content]. Idempotent: when a host is
 * already present (e.g. a card skeleton inside a skeleton section) the
 * existing clock is reused instead of creating another transition.
 */
@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    if (LocalShimmerProgress.current != null) {
        content()
        return
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
        ),
        label = "shimmerShift",
    )
    CompositionLocalProvider(LocalShimmerProgress provides progress) {
        content()
    }
}

/**
 * A rounded placeholder box with the shimmer sweep. The base tone is
 * [LedgerColors.CardHigh] (one step above the card fill) so skeletons read
 * as "content-shaped holes" in both light and dark themes. Outside a
 * [ShimmerHost] it renders as a quiet static block (no animation, no
 * allocation) — used for permanent "unavailable" placeholders.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    val progress = LocalShimmerProgress.current
    // highlight colors are derived here, once per composition — the draw
    // phase below only reads them
    val base = LedgerColors.CardHigh
    val soft = LedgerColors.TextMuted.copy(alpha = 0.16f)
    val core = LedgerColors.TextMuted.copy(alpha = 0.42f)
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawRect(color = base)
                val clock = progress ?: return@drawBehind
                // sweep a soft band from off-left to off-right so the loop
                // never pops; two stacked color rects approximate a gradient
                // without shader/brush allocations per frame
                val bandWidth = (size.width * 0.55f).coerceAtLeast(36f)
                val x = -bandWidth + clock.value * (size.width + 2f * bandWidth)
                drawRect(
                    color = soft,
                    topLeft = Offset(x, 0f),
                    size = Size(bandWidth, size.height),
                )
                drawRect(
                    color = core,
                    topLeft = Offset(x + bandWidth * 0.32f, 0f),
                    size = Size(bandWidth * 0.36f, size.height),
                )
            },
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
 * rate line, same reserved 64dp chart slot — so the swap from skeleton to
 * data is pixel-stable and nothing jumps. Self-hosting: wraps itself in a
 * [ShimmerHost] when used standalone, reuses an outer host when nested.
 */
@Composable
fun MarketCardSkeleton(
    modifier: Modifier = Modifier,
) {
    ShimmerHost {
        BentoCard(
            modifier = modifier,
            fill = LedgerColors.Card,
            padding = PaddingValues(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    // pair title (EUR/USD, bold 18sp — bar matches line box)
                    ShimmerBox(modifier = Modifier.size(width = 84.dp, height = 22.dp))
                    Spacer(Modifier.height(4.dp))
                    // currency name (bodySmall line box)
                    ShimmerBox(modifier = Modifier.size(width = 56.dp, height = 14.dp))
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
            // live rate (bold 30sp — bar matches its line box)
            Spacer(Modifier.height(16.dp))
            ShimmerBox(
                modifier = Modifier.size(width = 128.dp, height = 34.dp),
                cornerRadius = 8.dp,
            )
            // sparkline slot (always reserved — see PairCard)
            Spacer(Modifier.height(16.dp))
            ChartSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}
