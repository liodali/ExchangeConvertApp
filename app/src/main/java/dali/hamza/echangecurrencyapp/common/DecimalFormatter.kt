package dali.hamza.echangecurrencyapp.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class DoubleFormatter {

    companion object {
        private val local: Locale by lazy {
            Locale.getDefault()
        }
        private val formatter: DecimalFormat by lazy {
            NumberFormat.getInstance(local) as DecimalFormat
        }

        fun format(value: String): String {

            val symbols: DecimalFormatSymbols = formatter.decimalFormatSymbols
            symbols.setGroupingSeparator('.'); // setting the thousand separator
            symbols.setDecimalSeparator(',');// optionally setting the decimal separator

            formatter.decimalFormatSymbols = symbols
            formatter.setMinimumFractionDigits(2)
            formatter.setMaximumFractionDigits(2)
            return when {
                value.toDoubleOrNull() != null -> formatter.format(value.toDouble())
                else -> value
            }

        }
    }
}