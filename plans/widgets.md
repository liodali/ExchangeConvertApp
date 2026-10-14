# Home-Screen Widgets — Market Rate + Quick Convert (Oct 2026)

## Goal

Home-screen widgets on **both platforms** (Android: Jetpack Glance; iOS: WidgetKit)
that keep the app one glance away:

| Widget | Size | What it shows |
|---|---|---|
| **Market Rate** | small (2x2 / system small) | one selected pair `BASE/QUOTE` (e.g. `EUR/USD`), live rate, day Δ%, "as of" time. Tap → opens the converter pre-filled with the pair |
| **Quick Convert** | medium (4x2 / system medium) | amount + pair on the widget; result computed from the **cached** rate; swap button. Tap → opens the app converter with amount + pair |

No backend changes — `GET /latest?base=X[&symbol=Y][&amount=N]` already covers
everything (day Δ via `GET /historical`).

## Design sketch (`design/ui-design.pen`, Oct 2026)

Concept frames drawn **directly via `pen interactive`** (no AI agent — deterministic
`execute` calls against the live pen.dev app), right of the existing screens
(x = 10055; the 8 locked redesign frames verified untouched). Token-accurate:
Manrope 700 numerals, Inter labels, `$appearance-dark-accent` variables.

| Frame ID | Name | Size | Content |
|---|---|---|---|
| `prEnT` | Widget — Market Rate (small) | 158×158 | pair label + Manrope rate + day-Δ chip + "as of" row |
| `GDiky` | Widget — Quick Convert (medium) | 338×158 | amount + pair chips + swap ↔ converted result, cached-rate note, "Open app" gold CTA |
| `p7ayhz` | Widget — Market Grid (medium) | 338×158 | 3 market mini-cards ("follows your app selection" = Market Preferences), per-card Δ chips |
| `m9PNz` | Widget — Home context | 390×844 | home-screen mock (wallpaper, clock, dock) with the widgets placed in context |
| `RxvJn` | Widget — Market Rate (wave blob) | 158×158 | **Android-only visual**: M3-expressive soft scallop — **7 lobes radiating from the center** (`r = R + 4.5·sin(7θ)`, odd count centers a crest at top; m3.material.io/styles/shape), palette-tinted halo + surface body, no card background. Pair / rate / Δ centered inside |
| `I1L1a0` | Widget — Wave Blob Palettes | 158×158 | 2×2 mini wave-blob variants per Account palette — **Android-only**. (Frame kept being removed from the canvas; recreate on demand.) |

**Platform shape strategy (decided Oct 2026):** the circular-wave shape is an
**Android-only** identity. iOS widgets use the standard rounded-card look
(`prEnT` sketch): system-clipped rounded rectangle, dark surface +
palette accent (Δ chip / rate), sized for the small widget family — no wave
artwork, no lock-screen circular variant planned.

Pen-CLI gotchas learned (apply to any future direct drawing):
- The one-shot agent mode needs its own model auth; `pen interactive --app desktop`
  attaches to the running pen.dev app and needs none.
- **Insert-time layout adds +50px to every child's y** (flex AND absolute);
  `Update` applies coordinates literally. Workaround: draw, then run a normalizing
  `Update({y: n.y - 50})` pass — or author coordinates +50 too low and fix after.
- Flex layout (`layout:"vertical"/"horizontal"`) is unreliable in this app build
  (phantom heights, `space_between` overflow); the original screens are
  absolutely-positioned imports — build new UI with `layout:"none"` + explicit x/y.
- Numbers use **Manrope 700** (matches the original screens); "Liberation Mono"
  is present in old nodes but rejected as invalid on new ones.
- Editing via the app session edits the in-memory doc; `save()` writes it to disk
  (clobbering any on-disk changes made after the file was opened).

## UX entry points (per the request: "the user from Account can add a widget")

1. **Account → Preferences → "Widgets" row** (shared UI, new screen):
   - **Android**: pair picker → *Add to Home screen* →
     `AppWidgetManager.requestPinAppWidget()` with the pair passed as widget
     extras (user confirms; launcher support checked via
     `isRequestPinAppWidgetSupported` — most launchers OK). Also lists active
     widget instances (reconfigure / remove).
   - **iOS**: apps cannot pin widgets — the screen becomes a guide sheet
     ("long-press Home → + → Sovereign Ledger") with a live config preview.
     `WidgetCenter.reloadAllTimelines()` on any relevant pref change.
2. **Standard picker** (long-press launcher / iOS gallery) — both widget types
   registered with previews; Android launches a config activity on drop,
   iOS uses an in-gallery AppIntent configuration.

## Architecture

Everything data-side is shared; only the widget *surfaces* are platform code
(mirrors the rate-alerts split: shared engine, platform hosts).

```
                    shared/src/commonMain
  ┌───────────────────────────────────────────────────────────┐
  │ domain/models/WidgetSnapshot.kt                           │
  │   data class WidgetSnapshot(                              │
  │     pair: Pair(String,String), rate, prevClose,           │
  │     dayDeltaPercent, fetchedAtEpochMs)                    │
  │ data/widgets/WidgetsRepository.kt                         │
  │   - snapshots(configs): one getLiveRates() per DISTINCT   │
  │     base (the rate-alerts trick), one getHistorical()     │
  │     per base for prevClose                                │
  │   - convertCached(from,to,amount): live else last cache   │
  │ data/widgets/WidgetSnapshotStore.kt      [expect/actual]  │
  │   JSON blob(s): key = base currency (one blob per base)   │
  │   Android actual: file in filesDir (same process, no      │
  │     sharing needed)  ·  iOS actual: App Group             │
  │     UserDefaults(suiteName:) — extension reads it         │
  │ domain/models/WidgetConfig.kt  (pair / follow-markets     │
  │   option / widgetType) — serialized into the store        │
  │ platform/WidgetRefreshScheduler.kt       [expect/actual]  │
  │   fun onWidgetsChanged(active: Boolean)                   │
  │   Android: unique periodic 30-min Worker → fetch →        │
  │     GlanceAppWidgetManager.updateAll()                    │
  │   iOS: no-op (WidgetKit TimelineProvider drives itself)   │
  │ platform/WidgetBridge.kt                [iosMain]         │
  │   entry points for the extension process:                 │
  │   snapshots(configs) / fetchFresh(configs) / catalog()    │
  │   (extension inits its own Koin — see Gotchas)            │
  └───────────────────────────────────────────────────────────┘
        │                                    │
┌───────▼────────────────────┐   ┌───────────▼──────────────────┐
│ app module (Android)       │   │ iosApp/ExchangeWidgets       │
│ widgets/MarketRateWidget   │   │ (NEW WidgetKit extension)    │
│   GlanceAppWidget+Receiver │   │ MarketRateWidget: Timeline   │
│ widgets/QuickConvertWidget │   │   Provider + AppIntentConfig │
│   TextField(Glance), swap, │   │ QuickConvertWidget: iOS 17+  │
│   result from cached rate  │   │   interactive (buttons/      │
│ widgets/WidgetConfigActivity│  │   TextField on iOS 26)       │
│   (APPWIDGET_CONFIGURE)    │   │ views read App Group blob;   │
│ widgets/WidgetUpdateWorker │   │ timeline fetches fresh via   │
│ Account → Widgets screen   │   │   WidgetBridge (Ktor)        │
│   + requestPinAppWidget    │   │ framework LINKED, embedded   │
│                            │   │ once in the app target       │
└────────────────────────────┘   └──────────────────────────────┘
```

**Glance receivers run in the app process** → they resolve Koin directly
(`KoinPlatform.getKoin()`) like `RateAlertCheckWorker` does. **The iOS extension
is a separate process** → it never opens the SQLDelight DB; it reads the App
Group snapshot blob and fetches fresh rates itself through the shared framework
(backend needs no access key, nothing in the Keychain — clean for an extension).

## Configuration model

- **Android**: config activity (Compose, reuses the shared currency-picker
  sheet components) stores `WidgetConfig` keyed by `appWidgetId` in the shared
  store; "Edit" relaunches it (`android.appwidget.action.APPWIDGET_CONFIGURE`).
  Config choice "Follow Market Preferences" → widget mirrors the up-to-3
  Account market list (top pair on small, grid on medium).
- **iOS**: `AppIntentConfiguration` (iOS 17+) — dynamic pair options come from
  `WidgetBridge.catalog()` (CurrenciesCatalog); WidgetKit owns config
  persistence, our store is not involved.

## Refresh cadence

| Platform | Trigger |
|---|---|
| Android | periodic Worker **30 min** + one immediate fetch after config change + one after app foreground (`MainActivity.onResume`, piggybacked where the rate-alerts pass runs) |
| iOS | `TimelineProvider`: hourly entries for the next ~6 h, `.after(nextHour)` policy — stays inside the WidgetKit ~40–70 fetches/day budget |
| Tier (later) | guest cadence as above; Sovereign → 15-min Android / 30-min iOS entries (`DataTier` already in `ISessionStorage`) |

Offline / stale: snapshot carries `fetchedAt`; UI shows a subtle "delayed"
state when older than the cadence window. Quick Convert multiplies the cached
rate and says so.

## Deep links (needed by both widget types)

`sovereign://converter?from=EUR&to=USD&amount=100`
- Android: intent-filter on `MainActivity` → parse → navigate
  `Routes.CONVERTER` (extend with optional `from`/`to`/`amount` args).
- iOS: `.widgetURL(...)` → SwiftUI `onOpenURL` → same navigation call.
- Implemented once as shared nav args; used by widgets (and reusable later by
  rate-alert notifications, which currently just open the app).

## Phases

| Phase | Scope | Deliverable |
|---|---|---|
| **0 — shared groundwork** | `WidgetSnapshot`, `WidgetConfig`, `WidgetsRepository`, snapshot store expect/actual (Android file + iOS App Group), `WidgetRefreshScheduler` actuals, deep-link routes/args | `:shared:build` + common tests green; no UI yet |
| **1 — Android Market Rate** | Glance receiver + config activity + 30-min worker + manifest + **Account → Widgets** screen with `requestPinAppWidget` | shippable Android widget |
| **2 — Android Quick Convert** | medium Glance widget: `TextField` amount, pair, swap action, cached-rate math, deep link | both Android types live |
| **3 — iOS extension** | new `ExchangeWidgets` target (App Group + framework-link setup, see Gotchas), market widget + timeline, AppIntent config, Quick Convert (interactive where OS allows), Account guide sheet. **Visual: standard rounded card** (`prEnT` sketch) — WidgetKit clips widgets to its own rounded rect, so no wave shape on iOS (Android-only identity) | both iOS types live |
| **4 — polish** | day Δ% + prevClose via `historic`, Ledger palette/theming of widget surfaces, resize variants (large grid), tier-gated cadence, widget previews for the gallery | store-ready |

Phases 1/2 and 3 are independent after Phase 0 — can be parallelized.

## Implementation status (Oct 13 2026)

**Phase 0 + Phase 1 shipped** (wave-blob visual, Android first):

| File | Purpose |
|---|---|
| `shared/.../domain/models/WidgetSnapshot.kt` | `WidgetConfig` + `WidgetSnapshot` (+`dayDeltaPercent`), kotlinx-serializable |
| `shared/.../data/widgets/WidgetsRepository.kt` | per-distinct-base fetch + prev-close (day Δ), never throws |
| `shared/.../data/widgets/WidgetSnapshotStore.kt` (+ android/ios actuals) | JSON blob store: Android `filesDir/widgets/`, iOS NSUserDefaults (App Group in Phase 3) |
| `shared/.../di/SharedKoin.kt` | registers store + repository |
| `app/.../widgets/WaveBlobRenderer.kt` | runtime bitmap of the wave blob (same `r = R + A·sin(10θ)` as the .pen sketch) — RemoteViews can't draw shapes |
| `app/.../widgets/MarketRateWidget.kt` | Glance widget: blob background + pair/rate/Δ chip, palette accent, tap → app |
| `app/.../widgets/MarketRateWidgetConfigActivity.kt` | APPWIDGET_CONFIGURE pair picker (catalog + search), platform result contract |
| `app/.../widgets/MarketRateWidgetReceiver.kt` | Glance receiver; keeps the worker aligned (enables on first, cleans config on delete) |
| `app/.../widgets/WidgetRefreshWorker.kt` | 30-min periodic + on-demand worker (`WidgetRefreshScheduler`) |
| `app/.../widgets/WidgetsHostBridgeAndroid.kt` | pin (`requestPinAppWidget` + pair extras), reconfigure, refresh — registered in `appModule` |
| `shared/.../platform/WidgetsHostBridge.kt` | capability interface + no-op default (iOS shows guide until Phase 3) |
| `shared/.../ui/screens/WidgetsScreen.kt` + `WidgetsViewModel.kt` | **Account → Widgets** management: list (pair, rate, Δ), add via pin dialog (`AddWidgetPairDialog`), reconfigure, forget, refresh |
| `shared/.../ui/ExchangeCurrencyApp.kt` + `AccountScreen.kt` | `Routes.WIDGETS` + Preferences row (next to Rate Alerts) |
| `app/src/main/AndroidManifest.xml` + `res/xml|layout` | receiver, config activity, provider info, loading/preview layouts |

Deferred from Phase 1 (next): converter deep-link on tap, common tests for
`WidgetsRepository`. "Forget" only clears the stored config — providers cannot
unplace widgets (the instance shows "Pick a market" until removed on the launcher).

**Glance 1.2 gotchas learned** (verify against these before touching the widget code):
- `clickable` lives in **core** (`androidx.glance.action.clickable`), NOT appwidget.
- `actionStartActivity` is appwidget (`androidx.glance.appwidget.action`), Intent-based.
- `provideContent` must be imported explicitly (`androidx.glance.appwidget.provideContent`).
- `Glance.text.FontFamily` is **string-only** (sans/serif/mono/cursive) — no custom
  typefaces, so Manrope/Inter aren't usable in widget text; platform sans + weights.
- `GlanceAppWidgetManager.getGlanceIds(MarketRateWidget::class.java)` — takes the
  **widget** class, not the receiver.
- `ImageProvider(Bitmap)` (core) for runtime bitmaps — keep ≤256 px (Binder ~1 MB limit);
  the earlier plan's file + `GlanceFileProvider` approach is unnecessary.
- **No text input exists in Glance** (no `TextField`) — Quick Convert uses amount
  preset chips via `actionRunCallback` + per-widget SharedPreferences.
- `defaultWeight()` is a Row/Column scope member (no import).

**Deep links (shipped, light version):** widget taps carry a route extra;
`MainActivity` stashes it in `shared/.../ui/DeepLinkNav` and the shared NavHost
consumes it once after composition. Quick Convert → `full-converter`; market
widgets → app home (converter-pair prefill is future work). Known limitation:
handled in `onCreate` (new-activity launches); an `onNewIntent` re-delivery
path needs the singleTop + reactive-trigger follow-up.

**Widget types in the management list:** each `WidgetConfig.type` is now one of
`MARKET_CARD / MARKET_WAVE / MARKET_GRID / QUICK_CONVERT`, set by the widget at
self-configuration (preserved on reconfigure). Old `MARKET_RATE` values coerce
to `MARKET_CARD` via `coerceInputValues` in the store's Json.

**Config activity renders as a bottom sheet** over the launcher
(`Theme.ExchangeCurrencyApp.Sheet`: translucent window + dim scrim; sheet composable
inside the activity). Currency selection expands **in place** within the sheet
(searchable list, no nested dialogs); scrim-tap/Cancel keeps the platform
RESULT_CANCELED contract.

**Launcher reality (Funtouch/Vivo, Oct 2026):** the launcher places widgets
WITHOUT launching the config activity — pin extras never arrive. Countermeasures:
- **Self-configuration**: a widget rendering with no config adopts the pending
  pair stashed by the Account add flow (`WidgetSnapshotStore.savePendingPair`),
  saves it, and triggers a snapshot fetch — the config activity became optional.
- Unconfigured widgets render "Pick a market" and **tapping opens the picker**
  (config activity) instead of the app.

**Widget types shipped (Android):** `MarketRateCardWidget` (`prEnT`),
`MarketRateWidget` (wave, `RxvJn` — 7-lobe M3 soft scallop, R 64/68 A 3.5, rendered
with `ContentScale.Fit` so it stays perfectly circular in non-square cells;
a 4-sided-cookie variant from the M3 shape library is a future alternative),
`MarketGridWidget` (`p7ayhz` — zero-config, mirrors Account market preferences),
`QuickConvertWidget` (`GDiky` — **styled to the sketch**: two-column amount/pair
chips + swap ⇄ action / converted + cached-rate note / gold "Open app ↗",
amount presets 1/10/100/1K over the cached rate). All share the
worker refresh and the add dialog's TYPE selector (kind cards with descriptions;
pair pickers only for pair-based kinds).

## Key files (planned)

| File | Purpose |
|---|---|
| `shared/.../domain/models/WidgetSnapshot.kt` | snapshot + `WidgetConfig` + widget-type enum |
| `shared/.../data/widgets/WidgetsRepository.kt` | per-distinct-base fetch, day Δ, cached convert |
| `shared/.../data/widgets/WidgetSnapshotStore.kt` (+ android/ios actuals) | snapshot persistence (iOS: App Group) |
| `shared/.../platform/WidgetRefreshScheduler.kt` (+ actuals) | keeps the Android Worker aligned (rate-alerts pattern) |
| `shared/.../platform/WidgetBridge.kt` (iosMain) | extension-process entry points |
| `shared/.../ui/screens/WidgetsScreen.kt` + `WidgetsViewModel.kt` | Account → Widgets management screen (shared) |
| `app/.../widgets/MarketRateWidget.kt`, `QuickConvertWidget.kt` | Glance UI + receivers |
| `app/.../widgets/WidgetConfigActivity.kt`, `WidgetUpdateWorker.kt` | config + refresh |
| `app/src/main/AndroidManifest.xml` | receiver + config-activity declarations, deep-link intent-filter |
| `iosApp/ExchangeWidgets/` | WidgetKit extension (MarketTimelineProvider, views, intents) |
| `gradle/libs.versions.toml` | `androidx.glance:glance-appwidget:1.2.x` (+ material3 bridge) |
| `iosApp/ExchangeConvertApp.xcodeproj` | extension target, App Group capability, framework link |

## Gotchas / risks (anticipated — confirm during impl)

- **iOS framework in two targets**: the Kotlin framework must be *linked* by
  the extension but *embedded* only once (app target) or signing/duplicate-
  symbol issues follow. Extend the existing SwiftPM pre-action setup (see
  `plans/ios-spm-migration.md`) — likely `linkFramework`-style task for the
  extension slice, `embedAndSign…` stays app-only.
- **App Group** `group.<bundleId>` must be added to *both* targets' entitlements
  and provisioning profiles; the snapshot store must fail soft when the group
  is missing (dev builds) so the app itself never crashes.
- **Glance `TextField`** support varies by launcher (Samsung et al.) — Quick
  Convert keeps the tap-to-open fallback; feature-detect nothing, just always
  show the deep-link affordance. Glance 1.2.x needed for current Compose BOM
  (2025.04.01) — verify compatibility with compileSdk 36 at impl.
- **`requestPinAppWidget`** needs no permission but requires launcher support —
  check `isRequestPinAppWidgetSupported()` and fall back to a manual
  "long-press Home → Widgets" instruction.
- **iOS min target**: `AppIntentConfiguration` needs iOS 17+; interactive
  Quick Convert needs iOS 17 (buttons) / iOS 26 (TextField in widgets).
  Availability-gate and fall back to static + tap-to-open on older OSes
  (current min is 15.0 — confirm whether the redesign has raised it).
- **Extension Koin**: `WidgetBridge` must lazily `initSharedKoin()` on first
  call in the extension process (idempotent already); pass the host/token via
  the same `ExchangeSecrets` source compiled into the extension.
- **Write discipline**: single snapshot-blob-writer per process is fine
  (app writes its fetches, extension writes its own timeline fetches — whole-
  blob atomic replace via the store, no partial state).
- **Budgets**: keep WidgetKit timelines to ≤ 1 fetch/hour; Android periodic
  floor is 15 min — we use 30 to stay battery-friendly.
- **Metals/crypto pairs** (XAU/XAG/BTC…) come free through `/latest` — the
  pair pickers should use the same catalog as the app (incl. `CurrenciesCatalog`).

## Tests

- `shared/src/commonTest/.../WidgetsRepositoryTest.kt`: distinct-base
  aggregation (N widgets, 1 fetch when same base), day-Δ math, cached-convert
  fallback + stale flagging, empty/offline behaviour.
- Snapshot store round-trip test (common + per-platform actual smoke).
- Android: `WidgetUpdateWorker` under `work-testing`; manual matrix
  (Pixel launcher, Samsung One UI) for Glance rendering + pin request.
- iOS: manual verification checklist (below) — extension snapshot tests are
  low ROI.

## Verification checklist (fill in during impl)

- [ ] Android: pin from Account with prefilled pair; reconfigure; remove
- [ ] Android: 30-min worker updates the widget without the app open
- [ ] Android: Quick Convert computes offline from cached rate, opens app
      pre-filled via deep link
- [ ] iOS: widget gallery shows both types with previews; pair config sticks
- [ ] iOS: timeline refreshes hourly for ~12 h without the app running
- [ ] iOS: app-group snapshot read in extension (fresh + stale states)
- [ ] Both: stale "delayed" state when offline; rates identical to app Home
