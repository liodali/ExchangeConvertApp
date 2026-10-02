package dali.hamza.shared.platform

/**
 * System biometric authentication (app lock) — expect/actual.
 *
 * Android: androidx BiometricPrompt. iOS: LAContext device-owner
 * authentication. Results are delivered on the main thread.
 */
expect class BiometricAuthenticator {

    /** True when the device has biometrics (or device credential) enrolled. */
    fun isAvailable(): Boolean

    /**
     * Show the system prompt. [onResult] receives `true` on success.
     * Called on the main thread.
     */
    fun authenticate(onResult: (Boolean) -> Unit)
}

expect fun createBiometricAuthenticator(): BiometricAuthenticator
