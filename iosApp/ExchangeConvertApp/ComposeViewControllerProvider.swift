import SwiftUI
import SharedKMP

/// Hosts the shared Compose Multiplatform UI (see shared/src/iosMain/.../MainViewController.kt).
///
/// The Kotlin framework `shared` is built by the ExchangeConvertApp scheme
/// pre-action (`:shared:embedAndSignAppleFrameworkForXcode`) and re-exported
/// through the local Swift package `SharedKMP`.
struct ComposeViewControllerProvider: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            serverURL: ExchangeSecrets.apiHost,
            accessKey: ExchangeSecrets.apiToken
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Static composition; state lives inside the shared ViewModel.
    }
}
