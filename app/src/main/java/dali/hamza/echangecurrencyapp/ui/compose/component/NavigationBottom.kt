package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import dali.hamza.echangecurrencyapp.R

enum class NavigationBottomItemEnum(val vector: ImageVector?, val painter: Int?) {
    Conversion(null, painter = R.drawable.currency_conversion),
    Historic(Icons.Filled.History, null),
    Setting(Icons.Filled.Settings, null)
}


@Composable
fun NavigationBottom(bottomNavigate: (NavigationBottomItemEnum) -> Unit) {
    val selectedItem = remember { mutableIntStateOf(0) }

    NavigationBar {
        NavigationBottomItemEnum.entries.forEachIndexed { index, navigationBottomItemEnum ->
            NavigationBarItem(
                selected = selectedItem.intValue == index,
                onClick = {
                    bottomNavigate(navigationBottomItemEnum)
                    selectedItem.intValue = index
                },
                icon = {
                    navigationBottomItemEnum.vector?.let { vector ->
                        Icon(
                            imageVector = vector,
                            contentDescription = navigationBottomItemEnum.name
                        )
                    } ?: navigationBottomItemEnum.painter?.let { painterId ->
                        Icon(
                            painter = painterResource(id = painterId),
                            contentDescription = navigationBottomItemEnum.name
                        )
                    }
                })
        }
    }
}