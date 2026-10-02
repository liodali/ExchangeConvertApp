package dali.hamza.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS actual — LocalAuthentication (Face ID / Touch ID / device passcode).
 */
@OptIn(ExperimentalForeignApi::class)
actual class BiometricAuthenticator {

    actual fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)

    actual fun authenticate(onResult: (Boolean) -> Unit) {
        val context = LAContext()
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)) {
            onResult(false)
            return
        }
        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            localizedReason = "Unlock your ledger",
        ) { success, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                onResult(success)
            }
        }
    }
}

actual fun createBiometricAuthenticator(): BiometricAuthenticator = BiometricAuthenticator()
