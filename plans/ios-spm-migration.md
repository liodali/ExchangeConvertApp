# iOS SwiftPM Migration (CocoaPods → local Swift package)

**Status:** Done (2026-09-25)
**Method:** [JetBrains docs — "Using Kotlin from local Swift packages"](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-local-integration.html)
(Kotlin ≥ 2.0.0, project is on 2.3.20)

## Why

- Remove CocoaPods from the iOS build (`Podfile`, `Pods/`, `.xcworkspace`, `shared.podspec`).
- Let the iOS app consume the KMP `shared` framework through SwiftPM instead.
- Keep the monorepo "instant update" workflow: edit Kotlin → rebuild in Xcode → changes are live.

## How it works now

```
Xcode scheme Build pre-action
  └─ ./gradlew :shared:embedAndSignAppleFrameworkForXcode
       └─ builds shared.framework (static) for the requested SDK/arch
            └─ copies it to Xcode's BUILT_PRODUCTS_DIR
                 └─ local Swift package iosApp/SharedKMP imports it
                      └─ app imports SharedKMP (re-exports the Kotlin API)
```

Key files:

| File | Role |
|---|---|
| [`iosApp/SharedKMP/Package.swift`](iosApp/SharedKMP/Package.swift) | Local Swift package `SharedKMP` |
| [`iosApp/SharedKMP/Sources/SharedKMP/SharedKMP.swift`](iosApp/SharedKMP/Sources/SharedKMP/SharedKMP.swift) | `@_exported import shared` |
| [`shared/build.gradle.kts`](shared/build.gradle.kts) | `binaries.framework { baseName = "shared"; isStatic = true; binaryOption("bundleId", ...) }`, no `native.cocoapods` plugin |
| [`ExchangeConvertApp.xcscheme`](iosApp/ExchangeConvertApp.xcodeproj/xcshareddata/xcschemes/ExchangeConvertApp.xcscheme) | `<PreActions>` → Gradle task |
| [`project.pbxproj`](iosApp/ExchangeConvertApp.xcodeproj/project.pbxproj) | `XCLocalSwiftPackageReference` + `XCSwiftPackageProductDependency`, `ENABLE_USER_SCRIPT_SANDBOXING = NO`, `OTHER_LDFLAGS = -lsqlite3` |

## Gotchas discovered (do not regress)

1. **`<PreActions>` must be nested inside `<BuildAction>`** in the `.xcscheme`. At scheme root,
   Xcode silently ignores it (build then fails with `unable to resolve module dependency: 'shared'`).
2. **Open `ExchangeConvertApp.xcodeproj`, not the old `.xcworkspace`** (deleted).
3. **Link system SQLite: `OTHER_LDFLAGS = -lsqlite3`.**
   The removed `SQLCipher` pod was implicitly providing the `sqlite3_*` symbols used by
   SQLDelight's `native-driver` (kotlin `sqliter` cinterop). Without it the app fails to link.
   The shared DB was never actually encrypted (no `PRAGMA key` anywhere), so system SQLite
   preserves existing behaviour. If encryption is ever wanted, re-introduce SQLCipher explicitly
   (SwiftPM `binaryTarget` or Kotlin 2.4.20+ `swiftPMDependencies {}`).
4. **Stale user scheme:** an auto-generated user scheme in `xcuserdata` with the same name
   shadows the shared scheme and skips the pre-action. Delete it if the framework does not build.
5. **Kotlin/Native release linking needs heap:** `gradle.properties` now sets
   `kotlin.daemon.jvmargs=-Xmx4g` (was `-Xmx1224M`, which crashed the daemon during
   `linkReleaseFramework*`).
6. Killing/stopping the Gradle daemon while Xcode is idle is safe; the pre-action restarts it.

## Verification

```bash
# 1. Framework alone
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Full app (pre-action builds the framework)
cd iosApp
xcodebuild -project ExchangeConvertApp.xcodeproj -scheme ExchangeConvertApp \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO ARCHS=arm64 build

# 3. Release (validates the Kotlin release link + daemon memory)
xcodebuild -project ExchangeConvertApp.xcodeproj -scheme ExchangeConvertApp \
  -configuration Release -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO ARCHS=arm64 build
```

Both Debug and Release were verified, and the app was installed/launched on an iPhone 17 simulator.

## Not done (follow-ups)

- The iOS app still uses hand-written SwiftUI screens instead of the shared Compose Multiplatform UI
  — see [`plans/kmp-ui-unification-plan.md`](kmp-ui-unification-plan.md).
- The iOS REST error visible at launch (`"Not yet implemented - database access pending"`) is the
  pre-existing repository stub, not an integration problem — Phase 1 of the UI unification plan.
- Remote/distribution setup (XCFramework + `Package.swift` `binaryTarget`) is not needed for this
  monorepo; add it only if the shared module must be consumed by another repo.
