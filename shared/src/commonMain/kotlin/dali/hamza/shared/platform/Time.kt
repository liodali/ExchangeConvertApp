package dali.hamza.shared.platform

/**
 * Current hour of day (0-23, local time) for time-based greetings.
 */
expect fun currentHourOfDay(): Int

/**
 * Current epoch time in milliseconds (shared `System.currentTimeMillis()`).
 */
expect fun currentEpochMillis(): Long
