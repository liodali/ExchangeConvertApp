package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.nav_account
import echangecurrencyapp.shared.generated.resources.nav_history
import echangecurrencyapp.shared.generated.resources.nav_overview
import echangecurrencyapp.shared.generated.resources.nav_rates
import echangecurrencyapp.shared.generated.resources.nav_settings
import echangecurrencyapp.shared.generated.resources.nav_support
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.platform.isIos
import org.jetbrains.compose.resources.stringResource

/**
 * Rail destinations for the tablet/iPad shell (plans/tablet-ui.md) —
 * the design's five: Overview · Rates · History · Account · Support.
 * Rates maps to the app's converter surface until the dedicated Rates
 * screen ships (tablet plan Phase 3).
 */
enum class LedgerRailDestination(val icon: ImageVector) {
    OVERVIEW(Icons.Outlined.QueryStats),
    RATES(Icons.Outlined.SwapHoriz),
    HISTORY(Icons.Outlined.History),
    ACCOUNT(Icons.Outlined.Person),
    SUPPORT(Icons.Outlined.HelpOutline),
}

/** Localized rail label (enum constructors cannot be composable). */
@Composable
fun LedgerRailDestination.label(): String = when (this) {
    LedgerRailDestination.OVERVIEW -> stringResource(Res.string.nav_overview)
    LedgerRailDestination.RATES -> stringResource(Res.string.nav_rates)
    LedgerRailDestination.HISTORY -> stringResource(Res.string.nav_history)
    LedgerRailDestination.ACCOUNT -> stringResource(Res.string.nav_account)
    LedgerRailDestination.SUPPORT -> stringResource(Res.string.nav_support)
}

/**
 * The 72dp navigation rail from `iPad — Sovereign Market Dashboard`
 * (`Tablet Navigation Rail` frame). Replaces the bottom bar from MEDIUM
 * windows up. All colors are semantic [LedgerColors] tokens, so every
 * palette (Sovereign/Summer/Orchid/Light) themes it.
 */
@Composable
fun LedgerNavRail(
    selected: LedgerRailDestination,
    onSelect: (LedgerRailDestination) -> Unit,
    initials: String,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.Canvas)
            .then(if (isIos()) Modifier else Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
            ))
            // A phone in landscape can use the rail with little height.
            .then(if (isIos()) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(horizontal = 8.dp, vertical = 15.dp),
    ) {
        // brand mark — 38dp accent tile with the Dinarly "D"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(LedgerColors.Green),
        ) {
            Text(
                text = "D",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = LedgerColors.TextOnColor,
            )
        }

        // main navigation — 54×57 pills, icon over label
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LedgerRailDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                val label = destination.label()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .size(width = 54.dp, height = 57.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LedgerColors.GreenSoft else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(destination) },
                        ),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label,
                        tint = if (isSelected) LedgerColors.Green else LedgerColors.TextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) LedgerColors.Green else LedgerColors.TextMuted,
                    )
                }
            }
        }

        // account tools — settings + avatar initials (design "HM")
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.nav_settings),
                tint = LedgerColors.TextMuted,
                modifier = Modifier
                    .size(19.dp)
                    .then(
                        if (onSettings != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSettings,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(LedgerColors.CardAlt),
            ) {
                Text(
                    text = initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerColors.TextSecondary,
                )
            }
        }
    }
}
