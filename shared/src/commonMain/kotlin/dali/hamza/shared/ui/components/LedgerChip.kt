package dali.hamza.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ledger chip — the design's small status badges: tinted 10% fill, tinted
 * 600-weight caps text. Pill by default (`PRIVATE CLIENT`, `VERIFIED
 * LEDGER`); pass [cornerRadius] 4 for square tags (`PREMIUM`, `Enabled`).
 */
@Composable
fun LedgerChip(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color,
    cornerRadius: Dp = 9999.dp,
    fontSize: TextUnit = 12.sp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
            ),
            color = tint,
        )
    }
}
