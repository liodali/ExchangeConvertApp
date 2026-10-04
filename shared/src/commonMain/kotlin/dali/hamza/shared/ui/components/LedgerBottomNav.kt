package dali.hamza.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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

/** Capsule content height — sized to what an icon+label tab actually needs. */
private val CapsuleHeight = 60.dp

/** Gap between the capsule and the system gesture inset (it floats). */
private val FloatGap = 10.dp

/**
 * Total vertical clearance top-level hosts reserve so their last item
 * scrolls clear of the floating capsule: capsule + gap + breathing room
 * on top of the system-bar inset.
 */
@Composable
fun ledgerNavClearance(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        CapsuleHeight + FloatGap + 24.dp

/**
 * Ledger bottom navigation — floating "liquid glass" capsule (Tier-1
 * emulation, iOS 26 flavoured):
 *
 * - wraps its content — three tabs — instead of spanning the screen;
 *   floats 10dp above the home indicator / gesture bar
 * - glass material: translucent gradient fill, gloss wash and a specular
 *   hairline all around the capsule ([LedgerColors.GlassEdge])
 * - the selected tab sits in its own glass lens (bright gradient +
 *   hairline border) over a faint accent tint, Apple's selected-tab look
 *
 * Content scrolls beneath it — the app root overlays the bar instead of
 * using a Scaffold bottomBar slot.
 */
@Composable
fun LedgerBottomNav(
    currentDestination: LedgerDestination,
    onSelectDestination: (LedgerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            )
        }
    }
}

@Composable
private fun LedgerNavItem(
    destination: LedgerDestination,
    onClick: () -> Unit,
    selected: Boolean,
) {
    val pill = RoundedCornerShape(percent = 50)
    val contentColor by animateColorAsState(
        targetValue = if (selected) LedgerColors.Blue else LedgerColors.TextMuted,
        animationSpec = tween(200),
        label = "navColor",
    )
    Column(
        modifier = Modifier
            .clip(pill)
            .then(
                if (selected) {
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
                        .border(1.dp, LedgerColors.GlassEdge, pill)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
