package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dali.hamza.shared.ui.theme.LedgerColors
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sovereign Ledger bento card — rounded-24 raised surface, the base container
 * of every dashboard/history/account section in `ui-design.pen`.
 * Fills are layered dark surfaces; [stroke] is optional (design uses hairlines sparingly).
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(24.dp),
    stroke: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .then(
                if (stroke != null) Modifier.border(1.dp, stroke, shape) else Modifier
            )
            .padding(padding),
        content = content,
    )
}

/**
 * Glassmorphism card — the design's "glass" surfaces: subtle white gradient
 * over the dark card fill, hairline white border, rounded [cornerRadius].
 * (Backdrop blur is not applied — Compose MP has no cross-platform backdrop
 * blur; the gradient + border alone reproduce the design's read.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassBrush = Brush.verticalGradient(
        listOf(
            LedgerColors.GlossTop,
            LedgerColors.GlossBottom,
        )
    )
    Column(
        modifier = modifier
            .clip(shape)
            .background(glassBrush)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, LedgerColors.BorderSoft, shape)
            .padding(padding),
        content = content,
    )
}
