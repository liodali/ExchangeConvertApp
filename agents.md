# agents.md — AI Agent Guide for ExchangeConvertApp

This document describes the project structure, conventions, and key patterns to help AI agents (and developers) navigate and modify this codebase efficiently.

---

## 1. Project Identity

| Property | Value |
|---|---|
| **Name** | ExchangeConvertApp (Exchange Rate Currency Converter) |
| **Root package** | `dali.hamza.echangecurrencyapp` (note: `e` not `x` — legacy typo) |
| **Description** | Android + iOS (KMP) app that converts amounts between currencies using live exchange rates |
| **API** | [exchangerate.host](https://api.exchangerate.host/) |
| **Language** | Kotlin (Android/KMP) + Swift (iOS) |
| **Min SDK** | 26 (Android), 15.0 (iOS) |
| **Target/Compile SDK** | 36 |

---

## 2. Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | Kotlin | 2.1.20 |
| **UI (Android)** | Jetpack Compose + BOM | 2025.04.01 / Compose 1.8.0 |
| **UI (iOS)** | SwiftUI + Compose Multiplatform (hybrid) | Compose MP 1.7.0 |
| **DI** | Koin (multiplatform) | 4.2.0 |
| **Networking (legacy)** | Retrofit + Moshi + OkHttp | — |
| **Networking (KMP)** | Ktor Client | 3.4.2 |
| **Database (legacy Android)** | Room | — |
| **Database (KMP)** | SQLDelight | 2.3.2 |
| **Serialization** | Kotlinx Serialization + Moshi (legacy) | — |
| **Coroutines** | Kotlinx Coroutines | 1.10.2 |
| **Image Loading** | Coil 3 | 3.0.0-alpha01 |
| **Build** | Gradle KTS + Version Catalog (`libs.versions.toml`) | — |

---

## 3. Module Architecture

### 3.1 Current State (Mid-Migration)

The project is **actively migrating** from a pure-Android Clean Architecture setup to a **Kotlin Multiplatform (KMP)** architecture. Both old and new modules coexist.

```
┌──────────────────────────────────────────────────────────────┐
│  LEGACY ANDROID MODULES          KMP SHARED MODULE           │
│  ┌──────────┐ ┌──────────┐     ┌─────────────────────────┐   │
│  │   app    │ │  domain  │     │       shared            │   │
│  │ (UI/VM)  │ │(models/  │     │ ┌─────────────────────┐ │   │
│  │          │ │ ifaces)  │     │ │   commonMain        │ │   │
│  └────┬─────┘ └────┬─────┘     │ │  domain/models      │ │   │
│       │            │           │ │  domain/repository  │ │   │
│  ┌────▼─────┐ ┌────▼─────┐     │ │  data/network       │ │   │
│  │   core   │ │ database │     │ │  data/repository    │ │   │
│  │(repo/   │ │ (Room)   │     │ │  database/SQLDelight │ │   │
│  │network) │ │          │     │ │  ui/screens          │ │   │
│  └──────────┘ └──────────┘     │ ├─────────────────────┤ │   │
│                                │ │   androidMain       │ │   │
│                                │ │   iosMain           │ │   │
│                                │ └─────────────────────┘ │   │
│                                └─────────────────────────┘   │
│                                                              │
│  ┌──────────────────┐                                       │
│  │     iosApp       │  ← SwiftUI + Compose MP hybrid        │
│  │  (Swift/iOS)     │                                       │
│  └──────────────────┘                                       │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Module Descriptions

| Module | Type | Status | Purpose |
|---|---|---|---|
| **app** | Android Application | ACTIVE | UI layer: Compose screens, ViewModels, DI module, resources |
| **domain** | Android Library | DEPRECATING | Domain models + repository interfaces (moving to `shared`) |
| **core** | Android Library | DEPRECATING | Repository implementations, network layer, SessionManager (moving to `shared`) |
| **database** | Android Library | DEPRECATING | Room database, DAOs, entities (Android: Room, iOS: SQLDelight via `shared`) |
| **shared** | KMP Module | ACTIVE (new) | Shared business logic: domain, data, network, database (SQLDelight), Compose MP UI |
| **iosApp** | iOS Application | ACTIVE (new) | SwiftUI shell hosting the shared Compose Multiplatform UI (Kotlin framework via local SwiftPM package) |

### 3.3 Dependency Graph

```
app → domain, core, database
shared (KMP) → commonMain shared across androidMain + iosMain
iosApp → SharedKMP (local SwiftPM package) → shared (Kotlin/Native framework)
```

**Key rule**: When adding shared business logic, put it in [`shared/src/commonMain/`](shared/src/commonMain/). Platform-specific implementations go in [`shared/src/androidMain/`](shared/src/androidMain/) or [`shared/src/iosMain/`](shared/src/iosMain/).

---

## 4. Key File Reference Map

### 4.1 Build System

| File | Purpose |
|---|---|
| [`build.gradle.kts`](build.gradle.kts) | Root build: plugin declarations, global `extra` properties (`compose_version`, `kotlin_version`) |
| [`settings.gradle.kts`](settings.gradle.kts) | Module includes + repository declarations |
| [`gradle.properties`](gradle.properties) | Gradle/Android build properties |
| [`app/build.gradle.kts`](app/build.gradle.kts) | App module: Compose, Retrofit, Room, Koin, Coil, Navigation |
| [`shared/build.gradle.kts`](shared/build.gradle.kts) | KMP module: Ktor, SQLDelight, Compose MP, Koin, iOS framework targets |
| [`domain/build.gradle.kts`](domain/build.gradle.kts) | Domain module (lightweight, no AndroidX deps) |
| [`core/build.gradle.kts`](core/build.gradle.kts) | Core module: Retrofit, Room, DataStore dependencies |

### 4.2 App Module Key Files

| File | Purpose |
|---|---|
| [`app/.../ExchangeApplication.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ExchangeApplication.kt) | Application class, Koin initialization |
| [`app/.../ui/MainActivity.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/MainActivity.kt) | Single Activity, Compose host, navigation setup |
| [`app/.../di/AppModule.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/di/AppModule.kt) | Koin DI: OkHttp, Moshi, Room DB, SessionManager |
| [`app/.../di/CurrencyModule.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/di/CurrencyModule.kt) | Koin DI: ViewModels, repository bindings |
| [`app/.../viewmodel/MainViewModel.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/MainViewModel.kt) | Main ViewModel: currency selection, rate calculation, `StateFlow<UIState>` |
| [`app/.../viewmodel/CurrencyConvertViewModel.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/CurrencyConvertViewModel.kt) | Converter screen ViewModel |
| [`app/.../viewmodel/DialogCurrencyViewModel.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/DialogCurrencyViewModel.kt) | Currency picker dialog ViewModel |
| [`app/.../models/UIState.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/models/UIState.kt) | Sealed class: `UIState`, `LoadingUIState`, `DataUIState`, etc. |
| [`app/.../models/AmountInput.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/models/AmountInput.kt) | Amount input data class |
| [`app/.../common/DecimalFormatter.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/common/DecimalFormatter.kt) | Number formatting utilities |
| [`app/.../common/Extension.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/common/Extension.kt) | Kotlin extension functions |

### 4.3 App Compose UI Files

| File | Purpose |
|---|---|
| [`app/.../ui/compose/page/Home.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Home.kt) | Home screen composable |
| [`app/.../ui/compose/page/Rates.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Rates.kt) | Exchange rates list screen |
| [`app/.../ui/compose/page/converter_currency.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/converter_currency.kt) | Currency converter screen |
| [`app/.../ui/compose/page/CurrenciesSelection.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/CurrenciesSelection.kt) | Currency selection screen |
| [`app/.../ui/compose/page/historics_convertion.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/historics_convertion.kt) | History screen (stub) |
| [`app/.../ui/compose/page/settings.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/settings.kt) | Settings screen (stub) |
| [`app/.../ui/compose/component/ExchangesRateCompose.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/ExchangesRateCompose.kt) | Exchange rates list component |
| [`app/.../ui/compose/component/InputAmountFormCompose.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/InputAmountFormCompose.kt) | Amount input form |
| [`app/.../ui/compose/component/QuickExchangeSection.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/QuickExchangeSection.kt) | Quick exchange rates section |
| [`app/.../ui/compose/component/HistoryGraphSection.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HistoryGraphSection.kt) | Historical rates graph |
| [`app/.../ui/compose/component/CurrencySelection.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/CurrencySelection.kt) | Currency selector component |
| [`app/.../ui/compose/component/HeaderHome.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/HeaderHome.kt) | Home screen header |
| [`app/.../ui/compose/component/NavigationBottom.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/NavigationBottom.kt) | Bottom navigation bar |
| [`app/.../ui/compose/component/RateCard.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/RateCard.kt) | Rate display card |
| [`app/.../ui/compose/component/LoadingCompose.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/LoadingCompose.kt) | Loading indicator |
| [`app/.../ui/compose/component/commons.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/commons.kt) | Shared composables/utilities |
| [`app/.../ui/compose/component/ExchangeRateItem.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/ExchangeRateItem.kt) | Single exchange rate row item |
| [`app/.../ui/compose/dialog/CurrenciesPicker.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/dialog/CurrenciesPicker.kt) | Currency picker bottom sheet dialog |
| [`app/.../ui/compose/theme/Color.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt) | Color definitions |
| [`app/.../ui/compose/theme/Theme.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Theme.kt) | Material3 theme |
| [`app/.../ui/compose/theme/Type.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Type.kt) | Typography |
| [`app/.../ui/compose/theme/Shape.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Shape.kt) | Shape definitions |

### 4.4 Shared (KMP) Module Key Files

| File | Purpose |
|---|---|
| [`shared/.../domain/models/Currency.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/domain/models/Currency.kt) | Currency domain model |
| [`shared/.../domain/models/Response.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/domain/models/Response.kt) | `MyResponse<T>` sealed class (Success/Error/NoResponse) |
| [`shared/.../domain/repository/IRepository.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/domain/repository/IRepository.kt) | Repository interface |
| [`shared/.../data/network/CurrencyApi.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/CurrencyApi.kt) | Ktor-based API client (`/live`, `/convert`, `/historical` + access key) |
| [`shared/.../data/network/models/CurrencyDataAPI.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/models/CurrencyDataAPI.kt) | Network DTOs |
| [`shared/.../data/network/HttpClientFactory.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/HttpClientFactory.kt) | `expect` declaration for HTTP client factory |
| [`shared/.../data/repository/CurrencyRepositoryImpl.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImpl.kt) | Repository implementation (API + SQLDelight cache) |
| [`shared/.../data/CurrenciesCatalog.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/CurrenciesCatalog.kt) | Static currency list (generated from `app/assets/currencies.json`) |
| [`shared/.../data/storage/ISessionStorage.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/storage/ISessionStorage.kt) | Session storage interface (currency + last update) |
| [`shared/.../database/DatabaseDriverFactory.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/database/DatabaseDriverFactory.kt) | `expect` for SQLDelight driver factory |
| [`shared/.../di/SharedKoin.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/di/SharedKoin.kt) | Shared Koin module + `initSharedKoin(serverURL, accessKey)` |
| [`shared/.../ui/viewmodel/SharedViewModel.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/viewmodel/SharedViewModel.kt) | Shared ViewModel (`StateFlow<SharedUiState>`, no platform base class) |
| [`shared/.../ui/ExchangeCurrencyApp.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/ExchangeCurrencyApp.kt) | Shared app root (Scaffold + bottom navigation) |
| [`shared/.../ui/theme/`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/theme/) | Shared Material3 theme (single design source) |
| [`shared/.../ui/screens/ConverterCurrencyScreen.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/screens/ConverterCurrencyScreen.kt) | Shared converter screen |
| [`shared/.../ui/screens/RatesScreen.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/screens/RatesScreen.kt) | Shared rates screen |
| [`shared/.../ui/components/`](shared/src/commonMain/kotlin/dali/hamza/shared/ui/components/) | Shared components (bottom nav, currency picker sheet) |
| [`shared/.../database/currencies.sq`](shared/src/commonMain/sqldelight/dali/hamza/shared/database/currencies.sq) | SQLDelight schema |
| [`shared/.../MainViewController.kt`](shared/src/iosMain/kotlin/dali/hamza/shared/MainViewController.kt) | iOS entry: `MainViewController()` → `ComposeUIViewController` |
| [`shared/.../data/network/HttpClientFactory.android.kt`](shared/src/androidMain/kotlin/dali/hamza/shared/data/network/HttpClientFactory.android.kt) | Android `actual` — OkHttp engine |
| [`shared/.../data/network/HttpClientFactory.ios.kt`](shared/src/iosMain/kotlin/dali/hamza/shared/data/network/HttpClientFactory.ios.kt) | iOS `actual` — Darwin engine |
| [`shared/.../database/DatabaseDriverFactory.android.kt`](shared/src/androidMain/kotlin/dali/hamza/shared/database/DatabaseDriverFactory.android.kt) | Android `actual` — AndroidSqliteDriver |
| [`shared/.../database/DatabaseDriverFactory.ios.kt`](shared/src/iosMain/kotlin/dali/hamza/shared/database/DatabaseDriverFactory.ios.kt) | iOS `actual` — NativeSqliteDriver |

### 4.5 iOS App Key Files

| File | Purpose |
|---|---|
| [`iosApp/ExchangeConvertApp/ExchangeConvertAppApp.swift`](iosApp/ExchangeConvertApp/ExchangeConvertAppApp.swift) | iOS app entry point |
| [`iosApp/ExchangeConvertApp/ContentView.swift`](iosApp/ExchangeConvertApp/ContentView.swift) | Root view — hosts the shared Compose Multiplatform UI |
| [`iosApp/ExchangeConvertApp/ComposeViewControllerProvider.swift`](iosApp/ExchangeConvertApp/ComposeViewControllerProvider.swift) | `UIViewControllerRepresentable` wrapping shared `MainViewController()` |
| [`iosApp/ExchangeConvertApp/Secrets.swift.example`](iosApp/ExchangeConvertApp/Secrets.swift.example) | Committed template; copy to `Secrets.swift` (git-ignored) and add the exchangerate.host access key |
| [`iosApp/SharedKMP/Package.swift`](iosApp/SharedKMP/Package.swift) | Local Swift package exposing the Kotlin `shared` framework to the app |
| [`iosApp/SharedKMP/Sources/SharedKMP/SharedKMP.swift`](iosApp/SharedKMP/Sources/SharedKMP/SharedKMP.swift) | Re-exports `shared` (`@_exported import shared`) so app code imports `SharedKMP` |

> **iOS integration (SwiftPM local package):** `iosApp` no longer uses CocoaPods.
> The Kotlin framework is produced by the `:shared:embedAndSignAppleFrameworkForXcode`
> Gradle task, wired as a **pre-action** in
> [`ExchangeConvertApp.xcscheme`](iosApp/ExchangeConvertApp.xcodeproj/xcshareddata/xcschemes/ExchangeConvertApp.xcscheme),
> and consumed by the local Swift package in [`iosApp/SharedKMP`](iosApp/SharedKMP).
> Open `ExchangeConvertApp.xcodeproj` (not the removed `.xcworkspace`) in Xcode.
> The app target links system SQLite (`OTHER_LDFLAGS = -lsqlite3`) because the SQLDelight
> `native-driver` needs `sqlite3_*` symbols at link time (previously supplied by the removed
> `SQLCipher` pod). Full details and gotchas: [`plans/ios-spm-migration.md`](plans/ios-spm-migration.md).

### 4.6 Legacy Modules (Being Deprecated)

| File | Purpose |
|---|---|
| [`domain/.../models/Currency.kt`](domain/src/main/java/dali/hamza/domain/models/Currency.kt) | Legacy currency model |
| [`domain/.../models/Response.kt`](domain/src/main/java/dali/hamza/domain/models/Response.kt) | Legacy `MyResponse<T>` |
| [`domain/.../repository/IRepository.kt`](domain/src/main/java/dali/hamza/domain/repository/IRepository.kt) | Legacy repository interface |
| [`core/.../repository/CurrencyRepository.kt`](core/src/main/java/dali/hamza/core/repository/CurrencyRepository.kt) | Legacy repository implementation |
| [`core/.../datasource/network/CurrencyApi.kt`](core/src/main/java/dali/hamza/core/datasource/network/CurrencyApi.kt) | Legacy Retrofit API interface |
| [`core/.../common/SessionManager.kt`](core/src/main/java/dali/hamza/core/common/SessionManager.kt) | DataStore preferences manager |
| [`database/.../AppDB.kt`](database/src/main/java/mohamedali/hamza/database/AppDB.kt) | Room database class |
| [`database/.../dao/RatesCurrencyDao.kt`](database/src/main/java/mohamedali/hamza/database/dao/RatesCurrencyDao.kt) | Room DAO for rates |
| [`database/.../entities/CurrencyEntity.kt`](database/src/main/java/mohamedali/hamza/database/entities/CurrencyEntity.kt) | Room entity |

---

## 5. Coding Conventions & Patterns

### 5.1 Dependency Injection (Koin)

Koin modules are defined as top-level `val` using `module { }` DSL in the `di` package.

```kotlin
// Pattern for declaring DI modules
val myModule = module {
    single { MyDependency() }
    single<IMyInterface> { MyImpl(get()) }
    viewModel { MyViewModel(get(), get()) }
}

// Nesting modules
val appModule = module {
    includes(coreModule, featureModule)
}
```

**Location**: [`app/.../di/`](app/src/main/java/dali/hamza/echangecurrencyapp/di/) for Android, use Koin KMP in [`shared/`](shared/src/commonMain/).

### 5.2 ViewModel Pattern

ViewModels extend `androidx.lifecycle.ViewModel` and use:
- `mutableStateOf` / `by mutableStateOf` for Compose state
- `MutableStateFlow` / `StateFlow` for reactive streams
- `viewModelScope.launch` for coroutines
- Constructor injection via Koin

```kotlin
class MyViewModel(
    private val repository: CurrencyRepository,
    private val sessionManager: ISessionManager,
) : ViewModel() {

    var uiState: UIState by mutableStateOf(NoDataUIState())
    private val _dataFlow = MutableStateFlow<MyData>(initialValue)
    val dataFlow: StateFlow<MyData> = _dataFlow

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // fetch data, update state
        }
    }
}
```

### 5.3 UI State Sealed Class

[`UIState.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/models/UIState.kt) uses a sealed class hierarchy:

```kotlin
sealed class UIState
class LoadingUIState : UIState()
class NoDataUIState : UIState()
data class DataUIState<T>(val data: T) : UIState()
data class ErrorUIState(val message: String) : UIState()
```

Screen composables pattern-match on `UIState` to render loading/data/error states.

### 5.4 Compose Component Conventions

- **Pages** go in [`ui/compose/page/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/)
- **Reusable components** go in [`ui/compose/component/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/)
- **Dialogs** go in [`ui/compose/dialog/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/dialog/)
- **Theme** in [`ui/compose/theme/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/)
- Components receive ViewModel via `koinViewModel()` or `hiltViewModel()` (Koin used here)
- State hoisting: pass state down, events up via lambdas

### 5.5 Repository Pattern

```kotlin
// Interface in domain layer
interface IRepository {
    suspend fun getCurrencies(): MyResponse<List<Currency>>
    suspend fun getRates(currency: String, amount: Double): MyResponse<RatesData>
}

// Implementation in core/shared data layer
class CurrencyRepository(
    private val api: CurrencyApi,
    private val dao: RatesCurrencyDao,
) : IRepository { ... }
```

### 5.6 `expect`/`actual` Pattern (KMP)

Used in `shared` for platform-specific implementations:

```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, context, "app.db")
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(AppDatabase.Schema, "app.db")
}
```

### 5.7 Error Handling

The domain layer defines [`MyResponse<T>`](domain/src/main/java/dali/hamza/domain/models/Response.kt):

```kotlin
sealed class MyResponse<out T> {
    data class Success<T>(val data: T) : MyResponse<T>()
    data class Error(val message: String, val code: Int = -1) : MyResponse<Nothing>()
    class NoResponse<T> : MyResponse<T>()
}
```

`toUIState()` extension in [`UIState.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/models/UIState.kt) maps `MyResponse` → `UIState`.

---

## 6. Navigation

Bottom navigation with 4 tabs managed by [`NavigationBottom.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/NavigationBottom.kt):

1. **Home** — [`Home.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Home.kt)
2. **Rates** — [`Rates.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/Rates.kt)
3. **Converter** — [`converter_currency.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/converter_currency.kt)
4. **Settings** — [`settings.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/settings.kt)

Uses Jetpack Navigation Compose (`NavHost`, `NavController`). Single-Activity architecture in [`MainActivity.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/MainActivity.kt).

---

## 7. Build & Run

### 7.1 Android
```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:installDebug           # Install on connected device
./gradlew :app:test                   # Run unit tests
./gradlew :app:connectedAndroidTest   # Run instrumented tests
```

### 7.2 iOS
```bash
# No pod install needed. The shared Kotlin framework is built automatically by the
# ExchangeConvertApp scheme pre-action (:shared:embedAndSignAppleFrameworkForXcode).
open iosApp/ExchangeConvertApp.xcodeproj

# Optional: build/prepare the framework manually
./gradlew :shared:embedAndSignAppleFrameworkForXcode   # used by Xcode (needs Xcode env vars)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # plain simulator framework
```

### 7.3 Shared KMP Module
```bash
./gradlew :shared:build
./gradlew :shared:iosSimulatorArm64Test  # iOS simulator tests
```

---

## 8. Testing

### 8.1 Test Locations

| Type | Location |
|---|---|
| **Android Unit Tests** | `app/src/test/`, `core/src/test/`, `domain/src/test/` |
| **Android Instrumented Tests** | `app/src/androidTest/`, `core/src/androidTest/` |
| **KMP Common Tests** | `shared/src/commonTest/` (future) |
| **iOS Tests** | Xcode test targets |

### 8.2 Key Test Files

| File | What it tests |
|---|---|
| [`MainViewModelUnitTest.kt`](app/src/androidTest/java/dali/hamza/echangecurrencyapp/MainViewModelUnitTest.kt) | MainViewModel with mocked dependencies |
| [`UIComposeInstrumentedTest.kt`](app/src/androidTest/java/dali/hamza/echangecurrencyapp/UIComposeInstrumentedTest.kt) | Compose UI tests |
| [`CurrencyRepoTesting.kt`](core/src/androidTest/java/dali/hamza/core/CurrencyRepoTesting.kt) | CurrencyRepository integration tests |
| [`SessionManagerTest.kt`](core/src/androidTest/java/dali/hamza/core/SessionManagerTest.kt) | DataStore session tests |
| [`CurrencyRequestUnitTest.kt`](core/src/test/java/dali/hamza/core/CurrencyRequestUnitTest.kt) | Network request unit tests |
| [`RateRequestUnitTest.kt`](core/src/test/java/dali/hamza/core/RateRequestUnitTest.kt) | Rate request unit tests |

### 8.3 Testing Conventions
- Use `MockWebServer` for network mocking
- Use `Mockito` for Kotlin mocking
- Compose tests use `ComposeTestRule` and `createComposeRule()`

---

## 9. KMP Migration Status & Rules

### 9.1 Migration Context

The project is in an **active, incomplete migration** from Android-only to KMP. See the plans:

- [`plans/kmp-migration-architecture.md`](plans/kmp-migration-architecture.md) — Architecture decisions
- [`plans/kmp-ui-unification-plan.md`](plans/kmp-ui-unification-plan.md) — UI unification strategy

### 9.2 Migration Rules for AI Agents

1. **New shared code goes in `shared/src/commonMain/`**, NOT in `domain/` or `core/`.
2. **Platform-specific actuals** go in `shared/src/androidMain/` or `shared/src/iosMain/`.
3. **Do not delete legacy modules** (`domain`, `core`, `database`) yet — they're still referenced by `app`.
4. **When implementing a feature for both platforms**, implement in `shared` first, then wire up in `app` (Android) and `iosApp` (iOS).
5. **Database changes**: Update both the Room entities in [`database/`](database/) AND the SQLDelight `.sq` files in [`shared/.../database/`](shared/src/commonMain/sqldelight/dali/hamza/shared/database/).
6. **Networking**: Use Ktor (not Retrofit) for new code. Legacy Retrofit code in `core` will eventually be removed.

### 9.3 Package Naming

| Scope | Package |
|---|---|
| **Android (legacy)** | `dali.hamza.echangecurrencyapp.*` |
| **Core (legacy)** | `dali.hamza.core.*` |
| **Domain (legacy)** | `dali.hamza.domain.*` |
| **Database (legacy)** | `mohamedali.hamza.database.*` |
| **Shared KMP** | `dali.hamza.shared.*` |

---

## 10. Resources

| Resource | Location |
|---|---|
| **Strings** | [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml) |
| **Colors** | [`app/src/main/res/values/colors.xml`](app/src/main/res/values/colors.xml) + [`Color.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt) |
| **Themes (XML)** | [`app/src/main/res/values/themes.xml`](app/src/main/res/values/themes.xml) |
| **Drawables** | [`app/src/main/res/drawable/`](app/src/main/res/drawable/) |
| **Currencies JSON** | [`app/src/main/assets/currencies.json`](app/src/main/assets/currencies.json) |
| **API Token** | Injected via `local.properties` → `R.string.token` |

---

## 11. Common Tasks for AI Agents

### 11.1 Adding a New Screen (Android)

1. Create a new Composable in [`ui/compose/page/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/page/)
2. Create a ViewModel in [`viewmodel/`](app/src/main/java/dali/hamza/echangecurrencyapp/viewmodel/)
3. Register ViewModel in [`CurrencyModule.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/di/CurrencyModule.kt)
4. Add navigation route/destination in [`MainActivity.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/MainActivity.kt)
5. If needed, add bottom nav item in [`NavigationBottom.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/NavigationBottom.kt)

### 11.2 Adding a New API Endpoint

1. **Ktor (new)**: Add to [`shared/.../CurrencyApi.kt`](shared/src/commonMain/kotlin/dali/hamza/shared/data/network/CurrencyApi.kt)
2. **Retrofit (legacy)**: Add to [`core/.../CurrencyApi.kt`](core/src/main/java/dali/hamza/core/datasource/network/CurrencyApi.kt)
3. Create DTO in the corresponding `models/` package
4. Add repository method in both [`CurrencyRepository`](core/src/main/java/dali/hamza/core/repository/CurrencyRepository.kt) (legacy) and [`CurrencyRepositoryImpl`](shared/src/commonMain/kotlin/dali/hamza/shared/data/repository/CurrencyRepositoryImpl.kt) (KMP)

### 11.3 Adding a Database Table

1. **Room (legacy Android)**: Create entity in [`database/.../entities/`](database/src/main/java/mohamedali/hamza/database/entities/), DAO method in [`database/.../dao/`](database/src/main/java/mohamedali/hamza/database/dao/), update [`AppDB.kt`](database/src/main/java/mohamedali/hamza/database/AppDB.kt)
2. **SQLDelight (KMP)**: Add table to [`currencies.sq`](shared/src/commonMain/sqldelight/dali/hamza/shared/database/currencies.sq)

### 11.4 Adding a Compose Component

1. Create file in [`ui/compose/component/`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/component/)
2. Use `@Composable` annotation
3. Follow parameter convention: state in, lambdas out
4. Use Material3 components and project theme colors from [`Color.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/compose/theme/Color.kt)

---

## 12. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| **Files** | PascalCase for classes, snake_case for non-class files | `MainViewModel.kt`, `input_form_compose.kt` |
| **Composables** | PascalCase | `fun HomeScreen(...)` |
| **ViewModels** | `*ViewModel` suffix | `MainViewModel`, `DialogCurrencyViewModel` |
| **Interfaces** | `I` prefix | `IRepository`, `ISessionManager` |
| **DI Modules** | `camelCase` + `Module` suffix | `appModule`, `coreModule` |
| **Resources** | snake_case | `ic_exchange.xml`, `currencies.json` |
| **Sealed Classes** | PascalCase, descriptive | `UIState`, `MyResponse<T>` |

---

## 13. Key Architectural Rules

1. **Domain layer has zero Android dependencies** — pure Kotlin.
2. **Data flows unidirectionally**: UI observes ViewModel → ViewModel calls Repository → Repository calls API/DB.
3. **StateFlow over LiveData** for reactive streams in ViewModels.
4. **Single Activity** architecture — [`MainActivity.kt`](app/src/main/java/dali/hamza/echangecurrencyapp/ui/MainActivity.kt) is the only Activity.
5. **No wildcard imports** in Kotlin files.
6. **API keys/tokens** go in `local.properties`, never committed.

---

## 14. Current Technical Debt / Known Issues

- Typo in root package name: `echangecurrencyapp` (missing `x`)
- Hybrid network layer (both Retrofit and Ktor coexist)
- Hybrid database (Room + SQLDelight coexist)
- Some screens are stubs (`settings.kt`, `historics_convertion.kt`); the shared UI has only Converter + Rates tabs
- The KMP migration is incomplete — `domain`, `core`, and `database` modules still referenced but targeted for deprecation
- Android app is NOT yet wired to the shared UI — `MainActivity` still uses the app-local Compose UI and the legacy Retrofit repository; the legacy `strings.xml` server (`api.openexchangerate.com`) is a dead host
- Android `actual`s of the shared storage/driver need a `Context` (`createSessionStorage(context)`, `createDatabaseDriver(context)`) until the shared module is initialized from `ExchangeApplication`
- Shared UI icons are text glyphs until the Phase 7 redesign ships real icons

---

*Last updated: 2026-09-25. Update this file when significant architectural changes are made.*
