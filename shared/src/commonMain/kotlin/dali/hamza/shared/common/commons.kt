package dali.hamza.shared.common

expect fun formatString(format: String, value: Double): String

/**
 * Current wall-clock time in epoch milliseconds (multiplatform).
 */
expect fun nowMillis(): Long
