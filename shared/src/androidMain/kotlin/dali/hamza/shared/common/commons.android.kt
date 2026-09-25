package dali.hamza.shared.common

actual fun formatString(format: String, value: Double): String {
    return String.format(format, value)
}

actual fun nowMillis(): Long = System.currentTimeMillis()
