# AppFunctions — agent integration (Android intelligence system)

**Status:** shipped (Oct 2026) · **Library:** `androidx.appfunctions:1.0.0-alpha10` · **Docs:** <https://developer.android.com/ai/appfunctions>

## What this gives us

Sovereign Ledger acts as an **on-device MCP server**: the OS indexes our
tools and system agents (Gemini, any app holding `EXECUTE_APP_FUNCTIONS`)
can discover and execute them to fulfil user prompts like *"convert 250 EUR
to MAD"*, *"how did gold move this month?"*, *"alert me when EUR/USD moves
2%"* — without opening the app.

Runtime gating: Android 16+ (API 36). The service manifest entry is inert on
older devices (never bound); the Jetpack library no-ops caller-side.

## Surface (8 functions)

All in [`app/.../appfunctions/CurrencyAppFunctions.kt`](../app/src/main/java/dali/hamza/echangecurrencyapp/appfunctions/CurrencyAppFunctions.kt),
DTOs in `CurrencyAppFunctionTypes.kt`. KDocs **are** the tool descriptions
agents see — edit them carefully.

| Function | What it does | Notes |
|---|---|---|
| `convertCurrency(amount, from, to)` | Live conversion via `CurrencyApi.convert`; `amount=1` answers rate questions | Metals `XAU`/`XAG` + `BTC` supported |
| `getCurrencies(query?)` | Resolves names → ISO codes from the catalog | Agents must call this before pair functions |
| `getExchangeRates(base?, symbols?)` | Live rates; cache-first when `base` = user's base (respects guest hourly window), API otherwise | capped at 40 rows |
| `getHistoricalRates(base, quote, days?)` | Daily series → min/max/change% + ≤20 downsampled points | `days` clamped 1..365 |
| `addRateAlert(base, quote, mode, …)` | Creates a **LOCAL** alert via `IRepository.addRateAlert`, then re-arms the WorkManager scheduler | free cap 2; PERIODIC = 60/120 min only |
| `listRateAlerts()` | LOCAL + SERVER (best-effort) alerts | ids feed `deleteRateAlert` |
| `deleteRateAlert(alertId, source?)` | Deletes one alert + scheduler re-align | |
| `openAppScreen(screen, base?, quote?, amount?)` | **Returns a `PendingIntent`** opening the app UI deep-linked | see below |

### "Can the agent show a view from the app?"

Agents cannot embed our Compose UI in their own chat — AppFunctions exchange
structured data only. The supported pattern (same as the official sample's
`makeCall`) is: **return a `PendingIntent`** the agent launches, opening our
`MainActivity` deep-linked through the existing [`DeepLinkNav`](../shared/src/commonMain/kotlin/dali/hamza/shared/ui/DeepLinkNav.kt)
mechanism (also used by widgets):

- `converter` / `full-converter` accept `base`/`quote`/`amount` extras — the
  shared NavHost prefills the pair and **auto-converts** the amount
- `history` accepts a pair preselect
- screens: `home`, `converter`, `full-converter`, `history`, `rate-alerts`,
  `widgets`, `account`

(A2UI / `AppFunctionTextResource` response cards are the other direction —
agent-rendered UI from our data — not needed for v1.)

## Wiring

- **DI:** the service is a `KoinComponent`; `Application.onCreate` (Koin +
  `sharedModule`) always precedes any bound service in the process, so
  `by inject()` resolves `IRepository`, `CurrencyApi`, `RateAlertScheduler`
  lazily at call time.
- **KSP** generates `CurrencyAppFunctionService` (concrete service),
  `$…AppFunctionInventory.kt`, `BaseCurrencyAppFunctionServiceIds` (runtime
  enable/disable constants), `assets/currency_app_function_service.xml`
  (schema index) and `assets/app_functions_schema.xsd`.
- **Manifest** (`app/src/main/AndroidManifest.xml`): generated service under
  `BIND_APP_FUNCTION_SERVICE` + the two `android.app.appfunctions.*`
  properties; `<application>` carries `appfn:description` /
  `appfn:displayDescription`; `res/xml/app_metadata.xml` holds the
  cross-function **operational patterns + constraints** text agents use for
  orchestration (keep in sync with the KDocs!).
- **Version** pinned in `gradle/libs.versions.toml` → `appfunctions`.
  ⚠️ alpha13 renames `@AppFunction` → `@AppFunctionDeclaration` — a bump is
  a small migration, not a version-only change.

### API-shape gotchas learned

- Parameters with **default values must be nullable** (`days: Int? = 30`,
  not `Int = 30`) — KSP: "Type kotlin.String cannot be optional".
- `\"` escaping is invalid inside Android XML *attributes* (broke
  `aapt`); use unquoted words in `app_metadata.xml`.
- Functions run on the main thread by default → everything is `suspend`
  (repository/Ktor already dispatch internally).

## Verification

Build check (this repo, done): `./gradlew :app:assembleDebug` then confirm
`CurrencyAppFunctionService` + both assets appear in the APK.

On-device (needs an Android 16+ device/emulator, app installed):

```bash
# discover our indexed tools (looks for descriptions)
adb shell cmd app_function list-app-functions | grep --after-context 10 com.sovereignledger.app

# execute one directly (id = "<enclosingClass>#<method>")
adb shell "cmd app_function execute-app-function \
  --package com.sovereignledger.app \
  --function 'dali.hamza.echangecurrencyapp.appfunctions.BaseCurrencyAppFunctionService#convertCurrency' \
  --parameters '{\"amount\": 100.0, \"from\": \"EUR\", \"to\": \"MAD\"}'"

# open the converter with prefill (PendingIntent path)
adb shell "cmd app_function execute-app-function \
  --package com.sovereignledger.app \
  --function 'dali.hamza.echangecurrencyapp.appfunctions.BaseCurrencyAppFunctionService#openAppScreen' \
  --parameters '{\"screen\": \"full-converter\", \"base\": \"EUR\", \"quote\": \"USD\", \"amount\": \"250\"}'"
```

End-to-end without prompts: install the **AppFunctions testing agent**
(<https://github.com/android/appfunctions>, `agent/` project) — a chat app
that discovers and executes device AppFunctions with a pluggable LLM.

## Gemini / production caveats

- AppFunctions ↔ Gemini is **private preview / EAP** (register interest:
  <https://forms.gle/GN5ybjQFhzHRCguM7>). Implementing + indexing now is the
  recommended prep; discovery by the system agent comes with GA/EAP.
- Library is **experimental alpha** — expect API churn (see gotchas above).
- Security posture kept narrow: read-only market data, LOCAL alert CRUD
  (server-push alerts are never *created* by agents — no anonymous-session
  writes), no ledger/transaction writes, no destructive ops beyond deleting
  an alert by id.
- Release/minify: R8 issues were fixed by alpha04; the sample ships without
  extra keep rules — revisit if a release build strips generated classes.

## Follow-ups (not done)

- `setBaseCurrency` / watchlist tools when the agent UX demands them
- A2UI confirmation cards for `addRateAlert` (agent-rendered pre-confirm UI)
- Instrumented/Robolectric tests via `appfunctions-testing`
  (`AppFunctionTestRule`) once the surface stabilises
