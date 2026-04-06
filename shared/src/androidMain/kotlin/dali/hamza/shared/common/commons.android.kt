package dali.hamza.shared.commons

actual fun formatString(format: String, vararg args: Any?): String {
    return String.format(format, *args)
}