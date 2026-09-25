# KMP UI Unification & iOS Fix Plan

> ## Progress (2026-09-25)
>
> | Phase | Status | Notes |
> |---|---|---|
> | 1 — Foundation fixes (API/repository) | ✅ Done | `CurrencyRepositoryImpl` fully implemented (live rates + SQLDelight cache + session currency). Currency list comes from the static `CurrenciesCatalog` (from `app/assets/currencies.json`) because the API `/list` endpoint is rate-limited. Base API is `api.exchangerate.host` with `access_key` (`/live`, `/convert`, `/historical`). |
> | 2 — Theme unification | ✅ Done | `shared/.../ui/theme/` (Color/Shape/Type/Theme) ported; `ExchangeCurrencyAppTheme` used by the shared UI. Android `app` module not yet re-pointed to the shared theme. |
> | 3 — Port custom Compose views | ◐ Partial | `SharedBottomNavigation` + `CurrencyPickerSheet` + `RateCard`-style rows ported. `QuickExchangeSection`, `HistoryGraphSection`, `HeaderHome`, `LoadingCompose` not yet ported (Phase 7 redesign targets them). |
> | 4 — Shared logic/DI | ✅ Mostly done | `SharedViewModel` (StateFlow, KMP-safe) + `di/SharedKoin.kt` (`initSharedKoin(serverURL, accessKey)`) + `ISessionStorage` (iOS: NSUserDefaults; Android: SharedPreferences, needs `createSessionStorage(context)`). `UIState`/`AmountInput`/`DecimalFormatter` from the Android app were intentionally NOT ported — the shared `SharedUiState` replaces them. |
> | 5 — Replace iOS SwiftUI with shared Compose | ✅ Done | `MainViewController()` (`ComposeUIViewController`) hosted via `ComposeViewControllerProvider.swift`. SwiftUI duplicates + `CurrencyViewModel.swift` deleted. App bundle id `dali.hamza.ExchangeConvertApp`. |
> | 6 — Tests | ⬜ Not started | |
> | 7 — Redesign | ⬜ Not started | |
>
> Deviation from the original plan: Android keeps its current Compose UI (wiring `ExchangeCurrencyApp`
> into `MainActivity` is a separate follow-up); the iOS token is provided by a git-ignored
> `iosApp/ExchangeConvertApp/Secrets.swift` (template committed as `Secrets.swift.example`,
> same policy as `local.properties`).
>
> Gotchas fixed during Phase 5 (do not regress):
> - `shared` must apply `org.jetbrains.kotlin.plugin.serialization`
>   (`alias(libs.plugins.kotlinxSerialization)`). It was missing — Kotlin/Native then fails at
>   runtime with `Serializer for class '...' is not found` even though the HTTP call returns 200.
> - `Info.plist` must contain `CADisableMinimumFrameDurationOnPhone = true` or Compose MP's strict
>   plist check **crashes the app on launch** (enforced on iOS). Added via
>   `iosApp/ExchangeConvertApp/Info.plist` + `INFOPLIST_FILE` build setting (merged with
>   `GENERATE_INFOPLIST_FILE = YES`).
> - Koin qualifiers: `serverURL` and `accessKey` are both `String`s — resolve them with
>   `named("SERVER")` / `named("TOKEN")`, never bare `get<String>()`.

## Overview

This document outlines the complete plan to:
1. Unify the UI across Android and iOS using shared Compose Multiplatform (replacing SwiftUI duplicates)
2. Port all custom Android Compose views and custom logic to the shared KMP module
3. Fix iOS REST API failures when retrieving countries
4. Add comprehensive unit tests for the shared REST API layer and views

---

## Current State Analysis

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│ Currently — Three Separate UI Implementations                         │
│                                                                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────┐ │
│  │  Android App     │  │   Shared KMP     │  │     iOS App        │ │
│  │  (Compose Views) │  │   (Partial)      │  │   (SwiftUI Views)  │ │
│  │                  │  │                  │  │                    │ │
│  │  RateCard        │  │  CurrencyScreen  │  │  RateCardView      │ │
│  │  QuickExchange   │  │  (simple only)   │  │  QuickExchangeSec  │ │
│  │  HistoryGraph    │  │                  │  │  HistoryGraphSec   │ │
│  │  CurrenciesPick  │  │                  │  │  CurrencyPicker    │ │
│  │  HeaderHome      │  │                  │  │  HeaderSection    │ │
│  │  NavBottom       │  │                  │  │  (missing)         │ │
│  │  ConverterPage   │  │                  │  │  ConverterView     │ │
│  │  RatesPage       │  │                  │  │  RatesView         │ │
│  │  Theme/Design    │  │                  │  │  DesignColors      │ │
│  │  MainViewModel   │  │                  │  │  CurrencyViewModel │ │
│  └──────────────────┘  └──────────────────┘  └────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Coverage Matrix

| Component | Android (Compose) | iOS (SwiftUI) | Shared KMP | Action |
|---|---|---|---|---|
| RateCard | [RateCard.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/RateCard.kt) | [RateCardView.swift](iosApp/ExchangeConvertApp/RateCardView.swift) | Missing | Port to shared |
| QuickExchangeSection | [QuickExchangeSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/QuickExchangeSection.kt) | [QuickExchangeSection.swift](iosApp/ExchangeConvertApp/QuickExchangeSection.swift) | Missing | Port to shared |
| HistoryGraphSection | [HistoryGraphSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HistoryGraphSection.kt) | [HistoryGraphSection.swift](iosApp/ExchangeConvertApp/HistoryGraphSection.swift) | Missing | Port to shared |
| CurrencyPicker | [CurrenciesPicker.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/dialog/CurrenciesPicker.kt) | Inside [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift:224) | Missing | Port to shared |
| HeaderHome | [HeaderHome.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HeaderHome.kt) | Inside [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift:110) | Missing | Port to shared |
| NavigationBottom | [NavigationBottom.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/NavigationBottom.kt) | Missing | Missing | Port to shared |
| ConverterPage | [converter_currency.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/converter_currency.kt) | [CurrencyConverterView.swift](iosApp/ExchangeConvertApp/CurrencyConverterView.swift) | Partial ([CurrencyConverterScreen.kt](shared/src/commonMain/kotlin/dali/hamza/shared/ui/screens/CurrencyConverterScreen.kt)) | Complete in shared |
| RatesPage | [Rates.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Rates.kt) | [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift) | Missing | Port to shared |
| Theme/Design | [Color.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt) / [Theme.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Theme.kt) | [DesignColors](iosApp/ExchangeConvertApp/RateCardView.swift:4) (duplicated inline) | Missing | Port to shared |
| ViewModel | [MainViewModel.kt](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/MainViewModel.kt) | [CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift) | Missing | Create shared VM |
| UIState models | [UIState.kt](app/src/main/java/dali/hamza/echangecurrencyapp/models/UIState.kt) | N/A | Missing | Port to shared |
| AmountInput | [AmountInput.kt](app/src/main/java/dali/hamza/echangecurrencyapp/models/AmountInput.kt) | N/A | Missing | Port to shared |
| DecimalFormatter | [DecimalFormatter.kt](app/src/main/java/dali/hamza/echangecurrencyapp/common/DecimalFormatter.kt) | N/A | Missing | Port to shared |
| Extensions | [Extension.kt](app/src/main/java/dali/hamza/echangecurrencyapp/common/Extension.kt) | N/A | Missing | Port to shared |

### Existing Tests Status

| Test | Platform | Status |
|---|---|---|
| [CurrencyRequestUnitTest.kt](core/src/test/java/dali/hamza/core/CurrencyRequestUnitTest.kt) | JVM (Retrofit) | Android-only |
| [RateRequestUnitTest.kt](core/src/test/java/dali/hamza/core/RateRequestUnitTest.kt) | JVM (Retrofit) | Android-only |
| [CurrencyRepoTesting.kt](core/src/androidTest/java/dali/hamza/core/CurrencyRepoTesting.kt) | Android Instrumented | Android-only |
| [MainViewModelUnitTest.kt](app/src/androidTest/java/dali/hamza/echangecurrencyapp/MainViewModelUnitTest.kt) | Android Instrumented | Android-only |
| Shared KMP tests | N/A | **None** |

---

## Root Cause: iOS REST API Failure

### Problem

In [CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift:31):

```swift
let serverURL = "api.openexchangerate.com"
```

**Issue 1**: `api.openexchangerate.com` is not a valid API host. The original Android app used `api.exchangerate.host` which provides free currency exchange endpoints at `/latest`, `/currencies`, and `/historic/{date}`.

**Issue 2**: The shared [CurrencyApi.kt](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/CurrencyApi.kt:18) makes requests like `httpClient.get("currencies")` which resolves to `https://api.openexchangerate.com/currencies` — a non-existent endpoint.

**Issue 3**: The shared [CurrencyRepositoryImpl.kt](shared/src/commonMain/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImpl.kt:42-50) has stub implementations returning hardcoded errors:
```kotlin
return MyResponse.Error("Not yet implemented - database access pending")
throw NotImplementedError("Implementation pending - platform-specific storage required")
```

**Issue 4**: The `CurrencyViewModel.swift` observes `MyResponseSuccess<NSArray>` but the Kotlin sealed class `MyResponse.Success<T>` maps differently through Kotlin/Native interop, causing type casting mismatches in [CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift:53-56):
```swift
if let successResponse = response as? MyResponseSuccess<NSArray> {
    self.currencies = (successResponse.data as? [Any])?.compactMap { $0 as? Currency } ?? []
}
```

### Fix Strategy

1. **Correct the API base URL** to `api.exchangerate.host` (the original API used by the Android app, which is free and working)
2. **Add proper path prefix handling** in HttpClient factory for endpoints
3. **Complete all stub implementations** in `CurrencyRepositoryImpl`
4. **Add proper Swift-Kotlin interop handling** for the sealed class responses
5. **Add comprehensive error logging** for iOS network calls

---

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│ Target — Single Shared UI Implementation via Compose Multiplatform    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                    Shared KMP Module (commonMain)                 │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │ │
│  │  │   Domain Layer   │  │   Data Layer     │  │   UI Layer     │ │ │
│  │  │  Currency.kt     │  │  CurrencyApi.kt  │  │  All Screens   │ │ │
│  │  │  ExchangeRate.kt │  │  RepositoryImpl  │  │  All Components│ │ │
│  │  │  Response.kt     │  │  (fully impl)    │  │  Theme/Design  │ │ │
│  │  │  IRepository.kt  │  │                  │  │  SharedVM      │ │ │
│  │  └──────────────────┘  └──────┬───────────┘  └────────────────┘ │ │
│  │                               │                                   │ │
│  │  ┌────────────────────────────┴──────────────────────────────┐  │ │
│  │  │              Platform expect/actual                        │  │ │
│  │  │  HttpClientFactory (OkHttp / Darwin)                      │  │ │
│  │  │  DatabaseDriverFactory (Room / SQLDelight)                │  │ │
│  │  └───────────────────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                               │                                      │
│        ┌──────────────────────┴──────────────────────┐              │
│        │                                             │              │
│  ┌─────▼──────┐                              ┌──────▼─────┐        │
│  │ Android App│                              │  iOS App   │        │
│  │ (thin)     │                              │  (thin)    │        │
│  │ Activity + │                              │ SwiftUI    │        │
│  │ NavHost    │                              │ wrapper    │        │
│  │ using      │                              │ using      │        │
│  │ shared UI  │                              │ UIViewController │  │
│  └────────────┘                              │ Representable│   │
│                                              └────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Migration Plan

### Phase 1: Foundation Fixes (Critical Path)

#### 1.1 Fix iOS REST API
- **File**: [shared/src/commonMain/kotlin/dali/hamza/shared/data/network/CurrencyApi.kt](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/CurrencyApi.kt)
  - Add proper base URL configuration (use `api.exchangerate.host`)
  - Add path prefix handling
- **File**: [shared/src/iosMain/kotlin/dali/hamza/shared/data/network/HttpClientFactory.ios.kt](shared/src/iosMain/kotlin/dali/hamza/shared/data/network/HttpClientFactory.ios.kt)
  - Fix `defaultRequest` host configuration
  - Add proper timeout and error handling for Darwin engine
  - Add `expectSuccess = false` to avoid Ktor throwing on non-2xx responses
- **File**: [iosApp/ExchangeConvertApp/CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift)
  - Fix server URL to correct API host
  - Fix Kotlin/Native sealed class interop parsing
  - Add proper error handling with descriptive messages

#### 1.2 Unify Response/Model Types
- **Files**: [shared/src/commonMain/kotlin/dali/hamza/shared/domain/models/Response.kt](shared/src/commonMain/kotlin/dali/hamza/shared/domain/models/Response.kt) vs [domain/src/main/java/dali/hamza/domain/models/Response.kt](domain/src/main/java/dali/hamza/domain/models/Response.kt)
  - These have incompatible sealed class structures
  - Unify to single KMP-compatible `MyResponse` in shared module
  - Update all consumers (Android app, iOS app, repository)

#### 1.3 Complete Repository Implementation
- **File**: [shared/src/commonMain/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImpl.kt](shared/src/commonMain/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImpl.kt)
  - Implement `getListRatesCurrencies()` with real API calls
  - Implement `getCurrentCurrency()` with platform storage
  - Implement `saveExchangeRatesOfCurrentCurrency()` with API + database
  - Add expect/actual for database access

### Phase 2: Theme & Design System Unification

#### 2.1 Port Design Theme to Shared
- Move [Color.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt) to `shared/src/commonMain/kotlin/dali/hamza/shared/ui/theme/Color.kt`
- Move [Theme.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Theme.kt) to `shared/src/commonMain/kotlin/dali/hamza/shared/ui/theme/Theme.kt`
- Move [Shape.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Shape.kt) to `shared/src/commonMain/kotlin/dali/hamza/shared/ui/theme/Shape.kt`
- Move [Type.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Type.kt) to `shared/src/commonMain/kotlin/dali/hamza/shared/ui/theme/Type.kt`
- Remove duplicate `DesignColors` from [RateCardView.swift](iosApp/ExchangeConvertApp/RateCardView.swift:4-16)
- Update Android app to reference shared theme module

### Phase 3: Port Custom Compose Views to Shared

Each component will be moved from the Android `app` module to `shared/src/commonMain/kotlin/dali/hamza/shared/ui/` with minor adjustments for KMP compatibility (replacing Android-specific imports like `R.string` with `stringResource` or composable parameters):

| Priority | Component | Source | Destination |
|---|---|---|---|
| P0 | Theme files | [theme/](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/) | `shared/.../ui/theme/` |
| P0 | RateCard | [RateCard.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/RateCard.kt) | `shared/.../ui/components/RateCard.kt` |
| P0 | QuickExchangeSection | [QuickExchangeSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/QuickExchangeSection.kt) | `shared/.../ui/components/QuickExchangeSection.kt` |
| P1 | HistoryGraphSection | [HistoryGraphSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HistoryGraphSection.kt) | `shared/.../ui/components/HistoryGraphSection.kt` |
| P1 | HeaderHome | [HeaderHome.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HeaderHome.kt) | `shared/.../ui/components/HeaderHome.kt` |
| P1 | CurrencyPicker dialog | [CurrenciesPicker.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/dialog/CurrenciesPicker.kt) | `shared/.../ui/components/CurrenciesPicker.kt` |
| P2 | NavigationBottom | [NavigationBottom.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/NavigationBottom.kt) | `shared/.../ui/components/NavigationBottom.kt` |
| P2 | LoadingCompose | [LoadingCompose.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/LoadingCompose.kt) | `shared/.../ui/components/LoadingCompose.kt` |
| P2 | ExchangeRateItem | [ExchangeRateItem.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/ExchangeRateItem.kt) | `shared/.../ui/components/ExchangeRateItem.kt` |
| P2 | InputAmountForm | [InputAmountFormCompose.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/InputAmountFormCompose.kt) | `shared/.../ui/components/InputAmountFormCompose.kt` |
| P2 | ExchangesRateCompose | [ExchangesRateCompose.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/ExchangesRateCompose.kt) | `shared/.../ui/components/ExchangesRateCompose.kt` |
| P3 | ConverterCurrency Page | [converter_currency.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/converter_currency.kt) | `shared/.../ui/screens/ConverterCurrencyScreen.kt` (enhance existing) |
| P3 | Rates Page | [Rates.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Rates.kt) | `shared/.../ui/screens/RatesScreen.kt` |
| P4 | Home Page | [Home.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Home.kt) | `shared/.../ui/screens/HomeScreen.kt` |

### Phase 4: Port Custom Logic to Shared

#### 4.1 Create SharedViewModel
- **New file**: `shared/src/commonMain/kotlin/dali/hamza/shared/ui/viewmodel/SharedViewModel.kt`
  - Port Flow-based state management from [MainViewModel.kt](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/MainViewModel.kt)
  - Use KMP-compatible `StateFlow` (no `collectAsStateWithLifecycle` — use `collectAsState` directly)
  - Handle currency selection, amount input, exchange rate calculation
  - Use `MutableStateFlow` for reactive UI updates

#### 4.2 Port UI State Models
- Move [UIState.kt](app/src/main/java/dali/hamza/echangecurrencyapp/models/UIState.kt) to shared
- Move [AmountInput.kt](app/src/main/java/dali/hamza/echangecurrencyapp/models/AmountInput.kt) to shared
- Remove `IResponse.toUIState()` dependency on Android-specific `MyResponse` types

#### 4.3 Port Utility Classes
- Move [DecimalFormatter.kt](app/src/main/java/dali/hamza/echangecurrencyapp/common/DecimalFormatter.kt) to shared/common/
- Move [Extension.kt](app/src/main/java/dali/hamza/echangecurrencyapp/common/Extension.kt) to shared/common/
- Move [DateManager.kt](domain/src/main/java/dali/hamza/domain/common/DateManager.kt) to shared — remove `java.util.Date` dependency, use `kotlinx.datetime` or `Long` timestamps

#### 4.4 KMP-Compatible Dependency Injection
- Create shared Koin module in `shared/src/commonMain/kotlin/dali/hamza/shared/di/SharedModule.kt`
- Port [AppModule.kt](app/src/main/java/dali/hamza/echangecurrencyapp/di/AppModule.kt) and [CurrencyModule.kt](app/src/main/java/dali/hamza/echangecurrencyapp/di/CurrencyModule.kt) to KMP Koin

### Phase 5: Replace iOS SwiftUI with Shared Compose

#### 5.1 Create iOS Compose Hosting Wrappers
- **New file**: `iosApp/ExchangeConvertApp/ComposeViewControllerProvider.swift`
  - Create `UIViewControllerRepresentable` wrappers for each shared Compose screen
  - Handle state observation bridge between `SharedViewModel` and SwiftUI lifecycle

#### 5.2 Replace Individual Views
- Replace [ContentView.swift](iosApp/ExchangeConvertApp/ContentView.swift) → use shared `HomeScreen`
- Replace [CurrencyConverterView.swift](iosApp/ExchangeConvertApp/CurrencyConverterView.swift) → use shared `ConverterCurrencyScreen`
- Replace [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift) → use shared `RatesScreen`

#### 5.3 Remove SwiftUI Duplicates
- Delete [RateCardView.swift](iosApp/ExchangeConvertApp/RateCardView.swift) (including `DesignColors`)
- Delete [QuickExchangeSection.swift](iosApp/ExchangeConvertApp/QuickExchangeSection.swift)
- Delete [HistoryGraphSection.swift](iosApp/ExchangeConvertApp/HistoryGraphSection.swift)

#### 5.4 Update CurrencyViewModel
- Refactor [CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift) to be a thin bridge:
  - Remove duplicate business logic (conversion calculations, currency loading)
  - Delegate to shared `SharedViewModel` via KMP interop
  - Keep `@Published` properties for SwiftUI binding where needed

### Phase 6: Testing

#### 6.1 Shared API Unit Tests
- **New file**: `shared/src/commonTest/kotlin/dali/hamza/shared/data/network/CurrencyApiTest.kt`
  - Use Ktor `MockEngine` to mock HTTP responses
  - Test `getCurrencies()` — verify parsing of currency list
  - Test `getLatestRates()` — verify rate calculation
  - Test `getHistoricRates()` — verify historic data parsing
  - Test error handling (network failures, malformed JSON, HTTP errors)

#### 6.2 Shared Repository Unit Tests
- **New file**: `shared/src/commonTest/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImplTest.kt`
  - Test `getListCurrencies()` with mocked API responses
  - Test `getListRatesCurrencies()` with various amounts
  - Test error propagation through the repository layer

#### 6.3 Shared ViewModel Unit Tests
- **New file**: `shared/src/commonTest/kotlin/dali/hamza/shared/ui/viewmodel/SharedViewModelTest.kt`
  - Test currency selection state management
  - Test amount input validation and formatting
  - Test exchange rate calculation logic
  - Test loading/error/no-data state transitions

#### 6.4 Shared Compose UI Tests
- **New file**: `shared/src/commonTest/kotlin/dali/hamza/shared/ui/screens/CurrencyConverterScreenTest.kt`
  - Use `createComposeRule()` for Compose Multiplatform testing
  - Test amount input field interaction
  - Test currency selector dropdown
  - Test swap button functionality
  - Test rate display rendering
- **New file**: `shared/src/commonTest/kotlin/dali/hamza/shared/ui/screens/RatesScreenTest.kt`
  - Test rates list rendering
  - Test loading/error/empty states
  - Test RateCard display with various values

---

## File Structure After Migration

```
shared/src/
├── commonMain/kotlin/dali/hamza/shared/
│   ├── common/
│   │   ├── DecimalFormatter.kt          (ported from app)
│   │   ├── Extension.kt                 (ported from app)
│   │   ├── DateManager.kt              (ported from domain, KMP-ified)
│   │   └── commons.kt                   (existing)
│   ├── domain/
│   │   ├── models/
│   │   │   ├── Currency.kt              (existing, enhanced)
│   │   │   ├── ExchangeRate.kt          (existing)
│   │   │   ├── Response.kt              (existing, enhanced)
│   │   │   ├── UIState.kt              (new, ported from app)
│   │   │   └── AmountInput.kt          (new, ported from app)
│   │   └── repository/
│   │       └── IRepository.kt           (existing, enhanced)
│   ├── data/
│   │   ├── network/
│   │   │   ├── CurrencyApi.kt           (existing, FIXED)
│   │   │   ├── HttpClientFactory.kt     (existing expect)
│   │   │   └── models/
│   │   │       └── CurrencyDataAPI.kt   (existing)
│   │   └── repository/
│   │       └── CurrencyRepositoryImpl.kt (existing, COMPLETED)
│   ├── database/
│   │   └── DatabaseDriverFactory.kt     (existing expect)
│   ├── di/
│   │   └── SharedModule.kt             (new)
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt                 (new, ported from app)
│       │   ├── Theme.kt                 (new, ported from app)
│       │   ├── Shape.kt                 (new, ported from app)
│       │   └── Type.kt                  (new, ported from app)
│       ├── components/
│       │   ├── RateCard.kt              (new, ported from app)
│       │   ├── QuickExchangeSection.kt  (new, ported from app)
│       │   ├── HistoryGraphSection.kt   (new, ported from app)
│       │   ├── HeaderHome.kt            (new, ported from app)
│       │   ├── CurrencySelection.kt     (new, ported from app)
│       │   ├── CurrenciesPicker.kt      (new, ported from app)
│       │   ├── NavigationBottom.kt      (new, ported from app)
│       │   ├── ExchangeRateItem.kt      (new, ported from app)
│       │   ├── InputAmountForm.kt       (new, ported from app)
│       │   ├── ExchangesRateCompose.kt  (new, ported from app)
│       │   └── LoadingCompose.kt        (new, ported from app)
│       ├── screens/
│       │   ├── HomeScreen.kt            (new, ported from app)
│       │   ├── CurrencyConverterScreen.kt (existing, ENHANCED)
│       │   ├── RatesScreen.kt           (new, ported from app)
│       │   ├── HistoricsScreen.kt       (new)
│       │   └── SettingsScreen.kt        (new)
│       └── viewmodel/
│           └── SharedViewModel.kt       (new)
├── commonTest/kotlin/dali/hamza/shared/
│   ├── data/network/
│   │   └── CurrencyApiTest.kt           (new)
│   ├── data/repository/
│   │   └── CurrencyRepositoryImplTest.kt(new)
│   ├── ui/viewmodel/
│   │   └── SharedViewModelTest.kt       (new)
│   └── ui/screens/
│       ├── CurrencyConverterScreenTest.kt (new)
│       └── RatesScreenTest.kt           (new)
├── androidMain/kotlin/dali/hamza/shared/
│   └── (platform implementations - existing)
└── iosMain/kotlin/dali/hamza/shared/
    └── (platform implementations - existing, FIXED)
```

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| iOS native interop issues with Compose MP | Medium | Medium | Test on real device early; keep SwiftUI wrapper thin |
| State management mismatch between Compose and SwiftUI | Medium | Low | Use `SharedViewModel` as single source of truth |
| API endpoint changes break existing Android | High | Low | Keep both APIs configurable during migration |
| Database migration complexity (Room → SQLDelight) | Medium | Medium | Keep hybrid approach; share only repository interface |
| Build time increase from Compose MP | Low | High | Optimize Gradle; use incremental compilation |

---

## Pre-Migration Audit: Current Changes

**CRITICAL CONSTRAINT**: Before executing any migration step, audit the current state of all files that will be modified. This identifies:
1. Uncommitted changes that could be lost
2. Files that differ between Android and iOS implementations that need special attention
3. Shared code that is already partially migrated

### Files with Divergent Implementations (Require Alignment)

| Layer | Android File | iOS File | Divergence Type | Alignment Priority |
|---|---|---|---|---|
| RateCard | [RateCard.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/RateCard.kt:32-98) | [RateCardView.swift](iosApp/ExchangeConvertApp/RateCardView.swift:46-106) | Different layout (Row vs VStack wrapping HStack), different color system, different font system | P0 |
| QuickExchange | [QuickExchangeSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/QuickExchangeSection.kt:46-218) | [QuickExchangeSection.swift](iosApp/ExchangeConvertApp/QuickExchangeSection.swift:6-144) | Same logic but rendered differently; iOS missing swap button visual; Android has richer interaction states | P0 |
| HistoryGraph | [HistoryGraphSection.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HistoryGraphSection.kt:35-111) | [HistoryGraphSection.swift](iosApp/ExchangeConvertApp/HistoryGraphSection.swift:5-73) | Nearly identical structure; minor padding/font differences | P1 |
| Converter Page | [converter_currency.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/converter_currency.kt) | [CurrencyConverterView.swift](iosApp/ExchangeConvertApp/CurrencyConverterView.swift:5-120) | Significantly different: Android uses sophisticated input form with keypad; iOS is bare TextField | P1 |
| Rates Page | [Rates.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Rates.kt:42-171) | [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift:10-222) | Similar structure but iOS has inline picker/view definitions; Android has proper component separation | P2 |
| Currency Picker | [CurrenciesPicker.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/dialog/CurrenciesPicker.kt) | [RatesView.swift](iosApp/ExchangeConvertApp/RatesView.swift:225-290) (inline PickerView) | Completely different: Android has dialog with search + flag placeholders + sections; iOS is simple list | P2 |
| Theme/Colors | [Color.kt](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt) | [RateCardView.swift](iosApp/ExchangeConvertApp/RateCardView.swift:4-16) (DesignColors) | Duplicated: same hex values in both; need single source of truth | P0 |
| ViewModel | [MainViewModel.kt](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/MainViewModel.kt:36-134) | [CurrencyViewModel.swift](iosApp/ExchangeConvertApp/CurrencyViewModel.swift:5-182) | Completely different architecture: Flow-based vs ObservableObject; shared VM needed | P0 |
| Response Models | [Response.kt](domain/src/main/java/dali/hamza/domain/models/Response.kt:1-22) | [Response.kt](shared/src/commonMain/kotlin/dali/hamza/shared/domain/models/Response.kt:1-17) | Incompatible sealed class structures: `MyResponse<T>(data, error)` vs `MyResponse<T>` with inner classes | P0 |

### Design Reference Files

The project has design mockups that should serve as the single source of truth for the redesign:

| File | Description |
|---|---|
| [ui-design.pen](ui-design.pen) | Main UI design file (811 KB) — specifies exact colors, spacing, typography |
| [design/ui-design.pen](design/ui-design.pen) | Duplicate design file (777 KB) |
| [design/images/rate exchange.fig](design/images/rate%20exchange.fig) | Figma design file for rate exchange (360 KB) |
| design/images/image-import*.jpg | Design import images (5 files) |

---

## Alignment Constraints

**Principle**: Every UI component MUST render identically on all platforms (Android, iOS, Desktop if applicable) with zero visual divergence.

### Mandatory Alignment Rules

| Rule | Description | Enforcement |
|---|---|---|
| **Single Theme Source** | All colors, typography, shapes, and spacing defined once in `shared/ui/theme/` | Compile-time: theme files only in commonMain |
| **Identical Component API** | Every shared Composable has the same parameter signature regardless of platform | Compile-time: single source in commonMain |
| **ViewState as Single Source** | All UI state flows through `SharedViewModel.state: StateFlow<ViewState>` | Code review: no platform-specific state |
| **Design Token Reference** | All visual values (sizes, colors, fonts) reference design tokens, never hardcoded | Lint rule or code review |
| **Platform-Adaptive, Not Platform-Specific** | Differences handled via `expect/actual` only where truly necessary (e.g., navigation patterns) | Code review |
| **Delete Duplicates After Migration** | Once a shared component is verified, delete the platform-specific SwiftUI/Compose duplicate immediately | CI check: no duplicate component names |

### UI Alignment Checklist (per component)

For each component ported to shared, verify:
- [ ] Spacing/padding matches design spec exactly (use `dp` values from theme)
- [ ] Typography matches (font size, weight, line height from theme)
- [ ] Colors match (reference design tokens, never hardcoded hex)
- [ ] Dark theme support is consistent (use MaterialTheme or custom theme)
- [ ] Touch/click targets meet platform minimums (48dp Android, 44pt iOS)
- [ ] Loading/error/empty states render identically
- [ ] Animations are platform-consistent or gracefully degraded

---

## Dependencies

- [shared/build.gradle.kts](shared/build.gradle.kts) — already configured with Compose MP, Ktor, SQLDelight, Koin
- [`iosApp/SharedKMP`](iosApp/SharedKMP) — local Swift package exposing the shared KMP framework (no CocoaPods; framework built by the scheme pre-action)
- Platform engines: OkHttp (Android) + Darwin (iOS) for Ktor

---

## Phase 7: Complete UI Redesign (Follow-Up)

After Phase 1-6 completes the technical migration and unification, Phase 7 executes a **complete visual redesign** of all app views using the design mockups as specifications.

### 7.1 Design Audit
- Extract all design tokens from [ui-design.pen](ui-design.pen) and [design/images/rate exchange.fig](design/images/rate%20exchange.fig)
- Catalog all screens, components, and interaction patterns from the design files
- Identify gaps between current implementation and design spec

### 7.2 Redesign Scope
| Screen | Current Status | Redesign Goal |
|---|---|---|
| Home/Dashboard | Android has Home.kt with bottom nav; iOS missing | Full redesign with wallet-style summary, quick actions, recent transactions |
| Converter | Both platforms have different implementations | Unified rich converter with animated currency swap, real-time rate ticker, amount keypad |
| Rates | Both platforms have similar structure | Card-based rate list with sparkline charts, pull-to-refresh, sort/filter |
| History/Charts | Placeholder "coming soon" on both | Interactive line/area chart with date range picker, comparison mode |
| Settings | Placeholder "coming soon" on both | Theme toggle, default currencies, notification preferences |
| Currency Picker | Different implementations | Searchable grid with country flags, favorites, recently used |

### 7.3 Design System Enhancement
- Define comprehensive spacing scale (4dp increments: 4, 8, 12, 16, 20, 24, 32, 48, 64)
- Define full typography scale (display, headline, title, body, label, caption)
- Define elevation/shadow system for cards and surfaces
- Define motion/animation tokens (duration, easing curves)
- Create component variants (primary/secondary/outline/ghost buttons, filled/outlined inputs)

### 7.4 Interaction Design
- Add haptic feedback for key interactions (iOS haptics, Android vibration)
- Add micro-animations for state transitions (loading skeletons, success confirmations)
- Add pull-to-refresh across all data-driven screens
- Add swipe actions where appropriate (e.g., favorite a currency)

### 7.5 Accessibility
- Ensure minimum contrast ratios (WCAG AA: 4.5:1 for text)
- Add content descriptions for all interactive elements
- Support dynamic type/font scaling
- Support screen reader navigation order
