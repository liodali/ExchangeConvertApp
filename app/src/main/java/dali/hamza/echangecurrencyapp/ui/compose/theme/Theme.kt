package dali.hamza.echangecurrencyapp.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = Yellow200,
    primaryContainer = Yellow500,
    secondary = Teal300,
    error = errorDark,

    )

private val LightColorPalette = lightColorScheme(
    primary = Yellow700,
  //  primaryVariant = Yellow500,
    primaryContainer = Yellow500,
    secondary = Teal300,
  //  secondaryVariant = Teal800,
    secondaryContainer = Teal800,
    error = error,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,

    )

@Composable
fun ExchangeCurrencyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}