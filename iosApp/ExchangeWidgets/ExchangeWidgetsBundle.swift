import WidgetKit
import SwiftUI

// Sovereign Ledger — WidgetKit extension (plans/widgets.md Phase 3).
// Deliberately pure Swift: reads the App Group snapshot JSON written by
// the shared WidgetsRepository and falls back to a direct /latest fetch.
// No Kotlin framework link — the extension is a separate process with a
// tiny refresh budget, so it stays lean.

@main
struct ExchangeWidgetsBundle: WidgetBundle {
    var body: some Widget {
        MarketRateWidget()
    }
}
