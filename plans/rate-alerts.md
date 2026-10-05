# Rate Alerts — Local Conversion Notifications (Oct 2026)

## Goal

Notify the user about exchange-rate moves for tracked pairs with **local**
notifications (no push server). Two trigger modes, free-tier capped:

| Mode | Trigger |
|---|---|
| `PERIODIC` | every 1h or 2h digest (`intervalMinutes` = 60 / 120) |
| `THRESHOLD` | pair moved ±`thresholdPercent`% vs the previous check |

Free (guest) tier: **max 2 alerts** (`RateAlert.maxAlertsForTier`, enforced in
`CurrencyRepositoryImpl.addRateAlert`; Sovereign login will unlock 10).

## Architecture

```
RateAlertsScreen (shared UI, route "rate-alerts")
  └─ RateAlertsViewModel ──► IRepository (alerts CRUD, cap, dedupe per pair)
        │                     └─ SQLDelight RateAlert table (migration 2.sqm, v3)
        └─ RateAlertScheduler.update(hasActiveAlerts)   [expect/actual]
              ├─ Android: WorkManager — unique periodic (30 min) + one
              │           immediate check; RateAlertCheckWorker resolves the
              │           engine from Koin
              └─ iOS: no-op — Swift host schedules BGAppRefreshTask
                         (RateAlertsBackground.swift, gated by hasRateAlerts)

RateAlertsEngine (shared, the "job"):
  fetch live rates once per distinct base (CurrencyApi.getLiveRates)
  → compare each enabled alert vs its stored lastRate (the "previous data")
  → PERIODIC: fire when interval elapsed since lastNotifiedAt
    THRESHOLD: fire when |Δ%| ≥ threshold (15 min min-gap, trailing baseline)
  → LocalNotifier.notify() [expect/actual] + persist new baseline
  gates: Account "Push Notifications" pref + OS permission; 10 min self-throttle
```

Entry points to run the engine:
- Android: WorkManager periodic (30 min) + `ExchangeApplication` start sync +
  `MainActivity.onResume` (`RateAlertsAndroid.checkOnForeground()`)
- iOS: BGAppRefreshTask handler + every scene `.active`
  (`RateAlertsBridgeKt.runRateAlertsCheckNow`)

## Key files

| File | Purpose |
|---|---|
| `shared/.../domain/models/RateAlert.kt` | model + `RateAlertMode` + tier cap |
| `shared/.../database/rateAlerts.sq` + `2.sqm` | table + queries + migration (v2→v3) |
| `shared/.../data/alerts/RateAlertsEngine.kt` | evaluation/notification logic |
| `shared/.../platform/Notifications.kt` (+ android/ios actuals) | local notifier abstraction |
| `shared/.../platform/RateAlertScheduler.kt` (+ actuals) | background-job alignment |
| `shared/.../platform/RateAlertsBridge.kt` (iosMain) | Swift entry points |
| `shared/.../ui/screens/RateAlertsScreen.kt` | management UI (add/toggle/delete) |
| `shared/.../ui/viewmodel/RateAlertsViewModel.kt` | screen state |
| `app/src/main/AndroidManifest.xml` | `POST_NOTIFICATIONS` permission |
| `app/.../MainActivity.kt` | permission-result bridge + foreground check |
| `iosApp/.../RateAlertsBackground.swift` | BGTaskScheduler registration/schedule |
| `iosApp/.../Info.plist` | `BGTaskSchedulerPermittedIdentifiers` |

## Gotchas discovered

- SQLDelight sqlite-3.25 dialect: no `BOOLEAN` type; `INTEGER AS Boolean`
  demands a column adapter → store `INTEGER` 0/1, map `row.enabled != 0L`.
- `.sq` file name → queries property (`rateAlerts.sq` → `rateAlertsQueries`).
- Kotlin/Native binds `UNMutableNotificationContent` props as read-only
  (`val`) — set via KVC `content.setValue(x, forKey = "title")`
  (needs `import platform.Foundation.setValue`).
- `BGTaskScheduler.submitTaskRequest` keeps its `NSError**` param in K/N —
  scheduling stays Swift-side.
- Kotlin `fun init…` exports to ObjC as `do…` (avoids ObjC initializer
  semantics): Swift calls `SharedKoinKt.doInitSharedKoin(serverURL:accessKey:)`.
- iOS BG refresh is best-effort (OS decides cadence); foreground checks and
  the 10-min engine throttle keep it correct when the app is used.

## UI entry points

- Home → Market Overview cards: **bell** on each card — muted bell adds an
  alert for that pair (dialog pre-filled `base → market`), gold bell (alert
  exists) opens the Rate Alerts screen. Shared dialog:
  `shared/.../ui/components/AddRateAlertDialog.kt` (also used by the
  Rate Alerts screen; guards the free-tier cap with a note + disabled Add).
- Account → Preferences → **Rate Alerts** row
- Account top-bar **bell** icon (previously opened Support; Support keeps its
  own card + CTA at the bottom of the Account screen)

## Tests

`shared/src/commonTest/.../RateAlertsEngineTest.kt` — 6 tests: baseline
seeding, threshold fire + rebase, periodic interval wait, master-switch and
permission gates, disabled alerts, offline pass-through. Run on JVM + iOS
simulator (`testDebugUnitTest`, `iosSimulatorArm64Test`).

## Verified on Android (emulator, API 36 — Oct 2026)

End-to-end manual run on `com.sovereignledger.app.debug`:

- Onboarding → Account → Preferences → Rate Alerts entry row ✓
- Add dialog: From/To currency pickers, mode segments (Hourly / 2 Hours /
  On move), threshold input ✓
- `POST_NOTIFICATIONS` runtime dialog fired from the add flow ✓
- Alert row: mode chips (`±0.50%`, `Every 1h`), last rate, last
  notification, `1 of 2` → `2 of 2 alerts used` counter + cap chip ✓
- Add button disabled at the 2-alert cap; toggle persists; delete works ✓
- WorkManager one-time + periodic jobs scheduled, ran, completed ✓
- Engine: seeded baselines via both the worker and the foreground
  (onResume) path; +0.03% move → correctly no fire; +11.62% (manipulated
  baseline) → notification posted on the `rate_alerts` channel with title
  `USD/EUR ▲ +11.62%`; baseline rebased after firing (next check +0.00%,
  no repeat) ✓

Fixes made during the run:
- "Last notification" label only renders after a real firing
  (`lastNotifiedAt > createdAt` — the value is seeded with the creation
  time so periodic alerts wait one full interval first).
- Napier logging added to the engine (gates, fetch result, per-alert
  decision); the Android sink is planted in `AndroidAppContext.appContext`
  (iOS already planted one in `HttpClientFactory.ios.kt`).
