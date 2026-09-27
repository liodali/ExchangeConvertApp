package dali.hamza.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.theme.LedgerColors

/**
 * Ledger button variants — from `ui-design.pen`:
 * - PRIMARY:  light-blue `#B9C7E4` fill, navy `#0D1C32` text (the design CTA)
 * - PREMIUM:  gold `#E9C349` fill (sovereign highlights)
 * - TONAL:    deep canvas `#0E0E0E` fill with hairline steel border (list rows/actions)
 * - GHOST:    no fill/border, secondary text
 */
enum class LedgerButtonVariant { PRIMARY, PREMIUM, TONAL, GHOST }

/**
 * The Ledger button: 56dp tall, radius 16, Manrope 800 label (design's
 * `Button` recipe — fill + optional leading/trailing icon, state in / events out).
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LedgerButtonVariant = LedgerButtonVariant.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = when (variant) {
        LedgerButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = LedgerColors.Blue,
            contentColor = LedgerColors.NavyPanel,
            disabledContainerColor = LedgerColors.Blue.copy(alpha = 0.38f),
            disabledContentColor = LedgerColors.NavyPanel.copy(alpha = 0.60f),
        )
        LedgerButtonVariant.PREMIUM -> ButtonDefaults.buttonColors(
            containerColor = LedgerColors.Gold,
            contentColor = LedgerColors.Canvas,
            disabledContainerColor = LedgerColors.Gold.copy(alpha = 0.38f),
            disabledContentColor = LedgerColors.Canvas.copy(alpha = 0.60f),
        )
        LedgerButtonVariant.TONAL -> ButtonDefaults.buttonColors(
            containerColor = LedgerColors.Canvas,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = LedgerColors.Canvas.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
        )
        LedgerButtonVariant.GHOST -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
        )
    }
    val border = if (variant == LedgerButtonVariant.TONAL) {
        BorderStroke(1.dp, LedgerColors.Steel)
    } else {
        null
    }

    if (variant == LedgerButtonVariant.GHOST) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            colors = colors,
            border = null,
            shape = shape,
        ) {
            LedgerButtonContent(text, leadingIcon, trailingIcon)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            colors = colors,
            border = border,
            shape = shape,
        ) {
            LedgerButtonContent(text, leadingIcon, trailingIcon)
        }
    }
}

@Composable
private fun LedgerButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Gap(8.dp)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        if (trailingIcon != null) {
            Gap(8.dp)
            Icon(imageVector = trailingIcon, contentDescription = null)
        }
    }
}
