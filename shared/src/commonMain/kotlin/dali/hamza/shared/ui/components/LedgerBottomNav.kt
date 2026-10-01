package dali.hamza.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Ledger bottom navigation — 101dp translucent bar over blurred content
 * (design: `#131313B2` fill), 3 items; the selected item gets the design's
 * raised pill (`#201F1F`, radius 16) with the light-blue active accent
 * `#B9C7E4`; inactive items use the muted blue-gray `#64748B`.
 *
 * Component-level only for Phase 1 — [dali.hamza.shared.ui.ExchangeCurrencyApp]
 * still uses [SharedBottomNavigation] until the Phase 2 navigation migration.
 */
@Composable
fun LedgerBottomNav(
    currentDestination: LedgerDestination,
    onSelectDestination: (LedgerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LedgerColors.SurfaceBlur)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(101.dp),
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
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerNavItem(
    destination: LedgerDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) LedgerColors.Blue else LedgerColors.TextMuted,
        animationSpec = tween(200),
        label = "navColor",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) LedgerColors.SurfaceRaised else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
