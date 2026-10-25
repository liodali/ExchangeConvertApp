package dali.hamza.shared.platform

import dali.hamza.shared.AndroidAppContext

/**
 * Reads the (undocumented but stable) system resource
 * `config_navBarInteractionMode`: 0 = 3-button, 1 = 2-button, 2 = gestures.
 * Anything unexpected falls back to gesture-style (the slimmer layout).
 */
actual fun usesButtonNavigation(): Boolean = runCatching {
    val context = AndroidAppContext.appContext ?: return false
    val id = context.resources.getIdentifier(
        "config_navBarInteractionMode",
        "integer",
        "android",
    )
    id != 0 && context.resources.getInteger(id) < 2
}.getOrDefault(false)
