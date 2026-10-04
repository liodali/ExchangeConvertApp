package dali.hamza.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.shared.platform.isIos
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Top-level destinations of the Sovereign Ledger app — 3 tabs matching the
 * design's bottom navigation (Home · History · Account).
 */
enum class LedgerDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    HISTORY("History", Icons.Outlined.History),
    ACCOUNT("Account", Icons.Outlined.Person),
}

// ---- iOS: floating liquid-glass capsule ----

/** Capsule content height — sized to what an icon+label tab actually needs. */
private val CapsuleHeight = 60.dp

/** Gap between the capsule and the system gesture inset (it floats). */
private val FloatGap = 10.dp

/**
 * Total vertical clearance iOS top-level hosts reserve so their last item
 * scrolls clear of the floating capsule. Android uses the Scaffold
 * bottomBar slot — the framework pads content, so hosts need none.
 */
@Composable
fun ledgerNavClearance(): Dp =
    if (isIos()) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            CapsuleHeight + FloatGap + 24.dp
    } else {
        0.dp
    }

/**
 * Ledger bottom navigation — platform split:
 *
 * - **iOS**: floating "liquid glass" capsule (Tier-1 emulation, iOS 26
 *   flavoured) — wraps its three tabs, floats 10dp above the home
 *   indicator; palette SurfaceBlur fill (the system-configured
 *   translucency), gloss wash, hairline edge; the selected tab sits in
 *   its own glass lens over a faint accent tint. Lives in an overlay Box
 *   in the app root so content slides beneath it.
 * - **Android**: the original design, unchanged — full-width 101dp bar,
 *   flat [LedgerColors.SurfaceBlur], 24dp top corners, 63dp item row with
 *   the solid raised pill (`#201F1F`, radius 16). Hosted in a Scaffold
 *   bottomBar slot; content stops at the bar — no glass peek-through.
 */
@Composable
fun LedgerBottomNav(
    currentDestination: LedgerDestination,
    onSelectDestination: (LedgerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isIos()) {
        val capsule = RoundedCornerShape(percent = 50)
        Row(
            modifier = modifier
                .navigationBarsPadding()
                .padding(bottom = FloatGap)
                .height(CapsuleHeight)
                .clip(capsule)
                // the system-configured "liquid amount": the palette's
                // SurfaceBlur token (alpha tuned per theme in Color.kt)
                .background(LedgerColors.SurfaceBlur)
                // gloss wash on top of the fill (palette gloss tokens)
                .background(
                    Brush.verticalGradient(listOf(LedgerColors.GlossTop, LedgerColors.GlossBottom))
                )
                // edge hairline — specular white in dark, dark hairline in light
                .border(1.dp, LedgerColors.GlassEdge, capsule)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            LedgerDestination.entries.forEach { destination ->
                LedgerNavItem(
                    destination = destination,
                    selected = currentDestination == destination,
                    onClick = { onSelectDestination(destination) },
                    glass = true,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                // fill recipe: mostly the background surface, a neutral
                // elevation step (Steel) for separation, and a whisper of
                // the primary — visible without reading as "green"
                .background(LedgerColors.SurfaceBlur)
                .background(LedgerColors.Steel.copy(alpha = 0.08f))
                .background(LedgerColors.Green.copy(alpha = 0.03f))
                .navigationBarsPadding(),
        ) {
            // crisp top hairline — defines the bar edge in both themes
            // (square corners: no clip, no radius)
            Box(Modifier.fillMaxWidth().height(1.dp).background(LedgerColors.BorderSoft))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 86dp content + ~16dp gesture inset ≈ 102dp total
                    // (design refresh: down from the original 117dp total)
                    .height(86.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(63.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    LedgerDestination.entries.forEach { destination ->
                        LedgerNavItem(
                            destination = destination,
                            selected = currentDestination == destination,
                            onClick = { onSelectDestination(destination) },
                            modifier = Modifier.weight(1f, fill = false),
                            glass = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerNavItem(
    destination: LedgerDestination,
    onClick: () -> Unit,
    selected: Boolean,
    glass: Boolean,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(16.dp)
    val contentColor by animateColorAsState(
        targetValue = if (selected) LedgerColors.Blue else LedgerColors.TextMuted,
        animationSpec = tween(200),
        label = "navColor",
    )
    Column(
        modifier = modifier
            .clip(pill)
            .then(
                if (selected) {
                    if (glass) {
                        // glass lens: faint accent tint under a bright gradient
                        // cap, edged with the specular hairline
                        Modifier
                            .background(LedgerColors.Blue.copy(alpha = 0.10f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.07f),
                                    )
                                )
                            )
                            .border(1.dp, LedgerColors.GlassEdge, RoundedCornerShape(percent = 50))
                    } else {
                        // classic raised pill (design `#201F1F`, radius 16)
                        Modifier.background(LedgerColors.SurfaceRaised)
                    }
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (glass) 18.dp else 24.dp,
                vertical = if (glass) 7.dp else 8.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
