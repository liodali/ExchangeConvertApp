import SwiftUI
import SharedKMP

/// Hosts the shared Compose Multiplatform UI (see shared/src/iosMain/.../MainViewController.kt).
///
/// The Kotlin framework `shared` is built by the ExchangeConvertApp scheme
/// pre-action (`:shared:embedAndSignAppleFrameworkForXcode`) and re-exported
/// through the local Swift package `SharedKMP`.
struct ComposeViewControllerProvider: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController(
            serverURL: ExchangeSecrets.apiHost,
            accessKey: ExchangeSecrets.apiToken
        )
        // Compose's first frame can take several seconds in Debug builds —
        // paint the Sovereign canvas (#0E0E0E) from the very first UIKit
        // frame so the window never flashes white while Compose warms up.
        controller.view.backgroundColor = UIColor(
            red: 0x0E / 255.0, green: 0x0E / 255.0, blue: 0x0E / 255.0, alpha: 1
        )
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Static composition; state lives inside the shared ViewModel.
    }
}
