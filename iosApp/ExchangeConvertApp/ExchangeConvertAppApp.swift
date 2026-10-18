import SwiftUI
import SharedKMP

@main
struct ExchangeConvertAppApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                // widget deep links: sovereign://history?base=X&quote=Y (and
                // sovereign://open) → shared DeepLinkNav, consumed by the
                // Compose NavHost after composition
                .onOpenURL { url in
                    guard url.scheme == "sovereign" else { return }
                    let host = url.host ?? "open"
                    var base: String?
                    var quote: String?
                    var amount: String?
                    if let comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
                        for item in comps.queryItems ?? [] {
                            if item.name == "base" { base = item.value }
                            if item.name == "quote" { quote = item.value }
                            if item.name == "amount" { amount = item.value }
                        }
                    }
                    let route: String
                    switch host {
                    case "history": route = "history"
                    case "converter": route = "full-converter"
                    case "account": route = "account"
                    case "alerts": route = "rate-alerts"
                    case "widgets": route = "widgets"
                    default: route = "home"
                    }
                    DeepLinkNav.shared.stash(
                        route: route,
                        pairBase: base,
                        pairQuote: quote,
                        amount: amount
                    )
                }
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                // rate alerts: one throttled engine pass on foreground
                RateAlertsBackground.checkNow()
            case .background:
                // re-arm the opportunistic background check
                RateAlertsBackground.schedule()
            default:
                break
            }
        }
    }
}
