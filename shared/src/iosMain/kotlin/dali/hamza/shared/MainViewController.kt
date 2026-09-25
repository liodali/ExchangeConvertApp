package dali.hamza.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import dali.hamza.shared.di.initSharedKoin
import dali.hamza.shared.ui.ExchangeCurrencyApp
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import platform.UIKit.UIViewController

/**
 * Entry point for the shared Compose Multiplatform UI on iOS.
 *
 * The Kotlin framework is consumed by iosApp through the local Swift package
 * `SharedKMP`; SwiftUI hosts this controller (see
 * iosApp/ExchangeConvertApp/ComposeViewControllerProvider.swift).
 */
fun MainViewController(
    serverURL: String,
    accessKey: String,
): UIViewController {
    val koin = initSharedKoin(serverURL = serverURL, accessKey = accessKey)
    return ComposeUIViewController {
        val viewModel = remember { koin.get<SharedViewModel>() }
        ExchangeCurrencyApp(viewModel = viewModel)
    }
}
