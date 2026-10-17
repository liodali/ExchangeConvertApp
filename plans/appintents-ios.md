# App Intents — Siri / Shortcuts / Apple Intelligence integration (iOS)

**Status:** shipped (Oct 2026) · **Framework:** [AppIntents](https://developer.apple.com/documentation/appintents) (iOS 16+, `@available`-guarded — the app target still deploys to iOS 15) · Android counterpart: [`plans/appfunctions.md`](appfunctions.md)

## What this gives us

The app exposes its core actions to **Siri, the Shortcuts app, Spotlight and
Apple Intelligence** as first-class intents — no extension process needed
(App Intents run inside the app). Users can say *"Convert currency in
Sovereign Ledger"*, *"Add a rate alert in Sovereign Ledger"* or chain the
actions in Shortcuts/automations (e.g. a travel Focus that runs a
conversion).

## Surface (6 intents + entity + shortcuts)

Swift layer: [`iosApp/ExchangeConvertApp/AppIntents/`](../iosApp/ExchangeConvertApp/AppIntents/) ·
Kotlin bridge: [`shared/src/iosMain/.../platform/AppIntentsBridge.kt`](../shared/src/iosMain/kotlin/dali/hamza/shared/platform/AppIntentsBridge.kt)

| Intent | What it does |
|---|---|
| `ConvertCurrencyIntent` | Live conversion (amount/from/to, typed `CurrencyEntity` params) → dialog + numeric output |
| `GetExchangeRateIntent` | "What's the EUR→USD rate" (convert with amount 1) |
| `GetRateHistoryIntent` | min/max/Δ% over a day window (dates computed in Swift, stats in Kotlin) |
| `AddRateAlertIntent` | On-device alert (PERIODIC 60/120 min or THRESHOLD %) — same caps as the app (2 alerts, one per pair) |
| `ListRateAlertsIntent` / `DeleteRateAlertIntent` | Manage alerts; delete picks from an `AlertEntity` query |
| `OpenAppScreenIntent` | **The "show a view" counterpart** — see below |

Plus:
- `CurrencyEntity` + `CurrencyEntityQuery` (`EntityQuery`, `EntityStringQuery`) —
  typed currency parameters with search over the shared `CurrenciesCatalog`
  (curated suggestions in the picker, catalog search while typing)
- `SovereignShortcuts` (`AppShortcutsProvider`) — zero-setup phrases:
  *Convert currency in Sovereign Ledger*, *Rate history in…*, *Add a rate
  alert in…*, *Open Sovereign Ledger*

### Showing app views to Siri/agents

Same idea as Android's PendingIntent pattern, iOS flavor:
`OpenAppScreenIntent` sets `openAppWhenRun = true`, `perform()` stashes a
route via the shared `DeepLinkNav` (the exact mechanism the widgets'
`sovereign://` URLs use), and iOS then brings the app forward — the Compose
NavHost consumes the link on both cold start and warm app. Converter links
carry pair + amount and auto-convert.

## Architecture

```
Shortcuts/Siri ──▶ AppIntents (Swift, app process)
                      │  try await AppIntentsBridge.shared.convert(...)
                      ▼
              Kotlin object (iosMain, suspend → completion handler → Swift async)
                      │  KoinPlatform.getKoin() (warm: AppDelegate didFinishLaunching)
                      ▼
              shared IRepository / CurrencyApi  ← same repo the UI uses
```

- **Errors**: envelope DTOs (`errorMessage: String?`) instead of thrown
  Kotlin exceptions (NSError bridging is lossy); Swift converts them to
  `LocalizedError` so Siri reads them.
- **Koin timing**: App Intents execute in the app process after launch —
  `AppDelegate` already runs `doInitSharedKoin` in `didFinishLaunching`, so
  the container is warm. A missing container still degrades to a readable
  error envelope.
- **Alert scheduling after intent mutations**: iOS BGTask re-arm is
  Swift-owned (`RateAlertsBackground.schedule()` on scenePhase `.background`
  — the Kotlin `RateAlertScheduler` actual is a no-op), so an alert created
  via Siri re-arms its background check the next time the app backgrounds;
  the foreground engine pass covers the rest.
- **Keep in sync**: the bridge mirrors the Android AppFunctions service
  (validation, caps, 40-row list cap, 60/120 intervals). Change one →
  change both.

## Gotchas hit while wiring

- **Xcode 26.5 rejects `-> some PerformResult` witnesses.** The canonical
  AppIntent `perform()` signature fails type-checking on this toolchain
  (verified with bare `swiftc` against the SDK, all language modes):
  `'some' types are only permitted in properties, subscripts, and functions`
  + `reference to invalid associated type 'PerformResult'` — the compiler
  resolves `PerformResult` to the intent's own (uninferred) associated type.
  `Self.PerformResult` and qualified `AppIntent.PerformResult` fail too.
  **Fix:** spell the concrete result type —
  `IntentResultContainer<Value, OpensAppIntent, Snippet, Dialog>` (note the
  4-generic order in the iOS 26 SDK), e.g.
  `-> IntentResultContainer<Double, Never, Never, IntentDialog>`. The
  container's conditional conformances (`ReturnsValue`, `ProvidesDialog`)
  are identical, so Shortcuts metadata is unaffected.
- **`@Parameter(default:)` must be a compile-time literal** in Swift 5 mode —
  entity defaults like `CurrencyEntity.eur` don't qualify ("expect a
  compile-time constant literal"). Entity parameters are optional with
  `requestValueDialog` and coalesced in `perform()`; literal/enum defaults
  (amount, days, mode, interval) are fine.
- A `When(\.$mode, .equals, …)` conditional parameter summary also failed
  to infer its key path on this toolchain — plain `Summary(…)` used instead.
- **Compose 1.12.2 (Kotlin 2.4 toolchain) dropped `iosX64`** — no
  Intel-simulator artifacts on Maven Central, which broke
  `kmpPartiallyResolvedDependenciesChecker` for every shared/iOS build.
  Fix: removed the `iosX64()` target from `shared/build.gradle.kts`
  (Apple Silicon simulator + arm64 device only — what the scheme builds
  anyway). Consequence: always build against a **concrete simulator** (or
  pass `ARCHS=arm64`); a generic "Any iOS Simulator Device" destination
  yields `ARCHS=arm64 x86_64` and the Compose resource-sync task fails on
  the now-unknown x86_64 slice.
- Kotlin **default arguments don't cross the ObjC bridge** — Swift calls
  pass every parameter explicitly (`DeepLinkNav.shared.stash` precedent).
- Kotlin `Long` → Swift `Int64` (explicit conversions at call sites).
- App Intents metadata extraction works from a plain `xcodebuild` build —
  `ExchangeConvertApp.app/Metadata.appintents/` (`extract.actionsdata`,
  `root.ssu.yaml`) is what indexes intents/shortcuts on the OS.

## Verification

```bash
# Kotlin side (framework + generated header)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Full app (runs the scheme pre-action → embedAndSignAppleFrameworkForXcode)
cd iosApp && xcodebuild -project ExchangeConvertApp.xcodeproj \
  -scheme ExchangeConvertApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

On-device / simulator smoke:
1. Shortcuts app → new shortcut → all Sovereign Ledger actions appear with
   typed currency pickers.
2. Siri: "Convert currency in Sovereign Ledger" → parameter prompts → dialog.
3. `AddRateAlertIntent` → alert visible in the app's Rate Alerts screen
   (LOCAL section) on next open.
4. `OpenAppScreenIntent` (converter + pair + amount) → app opens on the full
   converter, prefilled and converted.

## Follow-ups (not done)

- Apple Intelligence **assistant schemas** (`AssistantIntent`) have no
  currency-conversion domain yet — revisit when Apple adds one.
- `ReturnsValue` outputs are scalars only; richer structured output (full
  rate tables, history series) could use a custom `AppEntity` per intent.
- Interactive confirmation cards (SwiftUI `IntentDialog` + snippets) for
  `AddRateAlertIntent`.
