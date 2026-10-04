package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_card_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_surface_muted
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_stroke
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_surface_variant

/**
 * HistoryGraphSection component showing 30-day price trend
 * Uses mock data as specified in the design
 */
@Composable
fun HistoryGraphSection(
    baseCurrency: String = "USD",
    targetCurrency: String = "EUR",
    modifier: Modifier = Modifier
) {
    // Generate mock 30-day data
    val mockDataPoints = remember { generateMockDataPoints() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = design_card_background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(design_card_background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Price History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_on_background
                )
                Text(
                    text = "30 day trend (mock data)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = design_on_surface_muted
                )
            }

            // Graph Area (placeholder for actual chart implementation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(design_surface_variant.copy(alpha = 0.25f))
                    .background(
                        Color(0xFF44474d).copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Line chart: $baseCurrency → $targetCurrency",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = design_on_surface_muted
                    )
                    Text(
                        text = "(Mock data for 30 days)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = design_on_surface_muted
                    )
                }
            }
        }
    }
}

/**
 * Generates mock data points for 30-day history
 * Simulates realistic currency rate fluctuations
 */
private fun generateMockDataPoints(): List<Pair<Long, Double>> {
    val dataPoints = mutableListOf<Pair<Long, Double>>()
    val now = System.currentTimeMillis()
    val dayInMillis = 24 * 60 * 60 * 1000L
    var baseRate = 0.92 // Starting rate for USD/EUR mock

    for (i in 29 downTo 0) {
        val timestamp = now - (i * dayInMillis)
        // Add some random fluctuation (±2%)
        val fluctuation = (Math.random() - 0.5) * 0.04
        baseRate = baseRate * (1 + fluctuation)
        dataPoints.add(timestamp to baseRate)
    }

    return dataPoints
}
