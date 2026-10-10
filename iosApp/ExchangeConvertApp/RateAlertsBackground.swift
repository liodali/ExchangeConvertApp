import SwiftUI
import BackgroundTasks
import Sentry
import UserNotifications
import SharedKMP

/// Local rate-alert plumbing (shared `RateAlertsEngine`):
/// - foreground: every scene activation runs one engine pass (the engine
///   self-throttles to ~10 minutes)
/// - background: a BGAppRefreshTask re-armed at ~30 minutes, best effort —
///   iOS decides when it actually runs
/// The task identifier must match Info.plist (BGTaskSchedulerPermittedIdentifiers)
/// and the shared `RateAlertSchedulerIos.TASK_ID`.
enum RateAlertsBackground {
    static let taskId = "dali.hamza.shared.ratealerts"

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskId, using: nil) { task in
            guard let refresh = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(refresh)
        }
    }

    static func schedule() {
        // only arm the background refresh when alerts are actually
        // configured (checked in the shared module's database)
        RateAlertsBridgeKt.hasRateAlerts(onResult: { active in
            guard active.boolValue else { return }
            let request = BGAppRefreshTaskRequest(identifier: taskId)
            request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
            try? BGTaskScheduler.shared.submit(request)
        })
    }

    static func checkNow() {
        RateAlertsBridgeKt.runRateAlertsCheckNow(onDone: { _ in })
    }

    /// APNs token → shared registration loop (Phase 3). Kotlin default
    /// arguments don't cross the ObjC bridge — onDone is explicit.
    static func registerPushToken(tokenHex: String, bundleId: String) {
        PushRegistrationBridgeKt.registerPushToken(
            tokenHex: tokenHex,
            bundleId: bundleId,
            onDone: { _ in }
        )
    }

    private static func handle(_ task: BGAppRefreshTask) {
        schedule() // re-arm the next background check right away
        let work = DispatchWorkItem {
            RateAlertsBridgeKt.runRateAlertsCheckNow(onDone: { _ in
                task.setTaskCompleted(success: true)
            })
        }
        task.expirationHandler = {
            work.cancel()
            task.setTaskCompleted(success: false)
        }
        DispatchQueue.global().async(execute: work)
    }
}

/// Hosts the BGTaskScheduler registration (must happen before the app
/// finishes launching) and warms the shared Koin container so a cold
/// background launch can resolve the engine before any UI exists.
/// Phase 3: also registers for remote notifications — the APNs token
/// flows through the shared `PushSessionManager` loop.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Crash reporting → self-hosted GlitchTip (Sentry protocol), same
        // server as the Android build. Captures native AND Kotlin crashes
        // (kfun symbols survive). Empty DSN (CI template) = disabled.
        if !ExchangeSecrets.glitchTipDSN.isEmpty {
            SentrySDK.start { options in
                options.dsn = ExchangeSecrets.glitchTipDSN
            }
        }
        // Kotlin exports init*-named functions with a "do" prefix to ObjC
        SharedKoinKt.doInitSharedKoin(
            serverURL: ExchangeSecrets.apiHost,
            accessKey: ExchangeSecrets.apiToken
        )
        RateAlertsBackground.register()

        // Server-push rate alerts (Phase 3): foreground presentation +
        // APNs registration. The token arrives asynchronously below; Koin
        // is warm by then. Needs the Push Notifications capability
        // (aps-environment entitlement) — tokens are silently absent
        // without it.
        UNUserNotificationCenter.current().delegate = self
        UIApplication.shared.registerForRemoteNotifications()
        return true
    }

    /// APNs delivered a device token → hex → shared registration loop
    /// (`POST /alerts/devices`, platform IOS, bundleId as the topic).
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02x", $0) }.joined()
        let bundleId = Bundle.main.bundleIdentifier ?? ""
        RateAlertsBackground.registerPushToken(tokenHex: tokenHex, bundleId: bundleId)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // Simulator without push support, missing entitlement, or no
        // network — the next launch retries (registration runs every open).
        NSLog("[PushSession] APNs registration failed: \(error.localizedDescription)")
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Show pushes while the app is foregrounded (default hides them).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        completionHandler()
    }
}
