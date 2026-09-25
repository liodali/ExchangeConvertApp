package dali.hamza.shared.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/**
 * Destinations of the shared bottom navigation.
 */
enum class SharedDestination(val label: String, val symbol: String) {
    CONVERTER("Converter", "⇄"),
    RATES("Rates", "💱"),
}

/**
 * Bottom navigation shared between Android and iOS.
 * (Icons are text glyphs for now — the icon set arrives with the Phase 7 redesign.)
 */
@Composable
fun SharedBottomNavigation(
    currentDestination: SharedDestination,
    onSelectDestination: (SharedDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        SharedDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination == destination,
                onClick = { onSelectDestination(destination) },
                icon = {
                    Text(
                        text = destination.symbol,
                        fontSize = if (destination == SharedDestination.RATES) 18.sp else 22.sp
                    )
                },
                label = { Text(text = destination.label) }
            )
        }
    }
}
