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
                    if let comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
                        for item in comps.queryItems ?? [] {
                            if item.name == "base" { base = item.value }
                            if item.name == "quote" { quote = item.value }
                        }
                    }
                    let route = host == "history" ? "history" : "home"
                    DeepLinkNav.shared.stash(
                        route: route,
                        pairBase: base,
                        pairQuote: quote
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
