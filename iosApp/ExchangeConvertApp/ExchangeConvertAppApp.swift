import SwiftUI

@main
struct ExchangeConvertAppApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
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
