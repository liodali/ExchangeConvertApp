import AppIntents

/// Zero-setup Siri/Shortcuts entry points (≤ 10 per app; phrases must name
/// the app via \(.applicationName). Everything else is discoverable in the
/// Shortcuts editor and via Spotlight/App Intents metadata.
@available(iOS 16.0, *)
struct SovereignShortcuts: AppShortcutsProvider {
    @AppShortcutsBuilder
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: ConvertCurrencyIntent(),
            phrases: [
                "Convert currency in \(.applicationName)",
                "\(.applicationName) conversion",
            ],
            shortTitle: "Convert",
            systemImageName: "arrow.left.arrow.right"
        )
        AppShortcut(
            intent: GetRateHistoryIntent(),
            phrases: ["Rate history in \(.applicationName)"],
            shortTitle: "Rate History",
            systemImageName: "chart.line.uptrend.xyaxis"
        )
        AppShortcut(
            intent: GetExchangeRateIntent(),
            phrases: [
                "Exchange rates in \(.applicationName)",
                "\(.applicationName) exchange rate",
                "Get the exchange rate in \(.applicationName)",
            ],
            shortTitle: "Exchange Rate",
            systemImageName: "banknote"
        )
        AppShortcut(
            intent: AddRateAlertIntent(),
            phrases: ["Add a rate alert in \(.applicationName)"],
            shortTitle: "Add Alert",
            systemImageName: "bell.badge"
        )
        AppShortcut(
            intent: OpenAppScreenIntent(),
            phrases: ["Open \(.applicationName)"],
            shortTitle: "Open App",
            systemImageName: "arrow.up.forward.app"
        )
    }
}
