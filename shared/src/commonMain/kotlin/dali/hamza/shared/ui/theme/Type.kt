package dali.hamza.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.Composable
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.inter_medium
import echangecurrencyapp.shared.generated.resources.inter_regular
import echangecurrencyapp.shared.generated.resources.inter_semibold
import echangecurrencyapp.shared.generated.resources.manrope_bold
import echangecurrencyapp.shared.generated.resources.manrope_extrabold
import echangecurrencyapp.shared.generated.resources.manrope_semibold

/**
 * Sovereign Ledger typography — extracted from `ui-design.pen`:
 * - **Manrope** 600/700/800 → headings & titles
 * - **Inter** 400/500/600 → body, labels, chips
 * - **Liberation Mono** → ledger numbers (mapped to [FontFamily.Monospace])
 *
 * Fonts are bundled as static instances generated with fontTools
 * (`composeResources/font/`, OFL licenses included).
 */
@Composable
fun ledgerFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

@Composable
fun ledgerHeadingFontFamily(): FontFamily = FontFamily(
    Font(Res.font.manrope_semibold, FontWeight.SemiBold),
    Font(Res.font.manrope_bold, FontWeight.Bold),
    Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Numbers/codes in ledger surfaces (rates, amounts, IDs). */
val LedgerNumeric = FontFamily.Monospace

@Composable
fun ledgerTypography(): Typography {
    val body = ledgerFontFamily()
    val heading = ledgerHeadingFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 56.sp),
        displayMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
        displaySmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
    )
}

/**
 * Legacy alias kept for API stability — resolves to the Ledger typography.
 */
@Composable
fun appTypography(): Typography = ledgerTypography()
