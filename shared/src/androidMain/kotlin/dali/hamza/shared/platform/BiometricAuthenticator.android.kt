package dali.hamza.shared.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dali.hamza.shared.AndroidAppContext

/**
 * Android actual — androidx [BiometricPrompt] against the current
 * foreground activity (see [AndroidAppContext.currentActivity]).
 */
actual class BiometricAuthenticator {

    actual fun isAvailable(): Boolean {
        val activity = AndroidAppContext.currentActivity ?: return false
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    actual fun authenticate(onResult: (Boolean) -> Unit) {
        val activity = AndroidAppContext.currentActivity
        if (activity == null || !isAvailable()) {
            onResult(false)
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sovereign Ledger")
            .setDescription("Unlock your ledger")
            // biometrics OR device PIN/pattern — nobody gets locked out
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }
}

actual fun createBiometricAuthenticator(): BiometricAuthenticator = BiometricAuthenticator()
