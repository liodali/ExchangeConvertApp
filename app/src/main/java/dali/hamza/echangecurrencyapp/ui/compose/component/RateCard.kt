package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_card_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_surface_muted
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_stroke
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_success

/**
 * RateCard component matching the design from ui-design.pen
 * Displays currency code, name, rate value, and change percentage
 */
@Composable
fun RateCard(
    currencyCode: String,
    currencyName: String,
    rateValue: Double,
    changePercent: Double?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = design_card_background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(design_card_background)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency Info (left side)
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currencyCode,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_on_background
                )
                Text(
                    text = currencyName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = design_on_surface_muted
                )
            }

            // Rate Value (right side)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatRateValue(rateValue),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_on_background
                )
                changePercent?.let {
                    Text(
                        text = formatChangePercent(it),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (it >= 0) design_success else Color(0xFFFFB4AB)
                    )
                }
            }
        }
    }
}

/**
 * Formats the rate value based on magnitude for optimal display
 */
private fun formatRateValue(value: Double): String {
    return when {
        value >= 10000 -> String.format("%.0f", value)
        value >= 1000 -> String.format("%.2f", value)
        value >= 1 -> String.format("%.2f", value)
        else -> String.format("%.4f", value)
    }
}

/**
 * Formats the change percentage with + or - sign
 */
private fun formatChangePercent(percent: Double): String {
    val sign = if (percent >= 0) "+" else ""
    return "$sign${String.format("%.2f", percent)}%"
}
