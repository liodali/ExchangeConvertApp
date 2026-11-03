# Xcode Cloud for Dinarly iOS

Xcode Cloud is supported through a local Swift package that hosts the shared
KMP framework. This is a source-build (not CocoaPods) integration:

```text
Apple Developer / ASC account
  → Xcode Cloud workflow for ExchangeConvertApp
    → Archive
      → ci_pre_xcodebuild.sh installs JDK 21
        → scheme pre-action runs :shared:embedAndSignAppleFrameworkForXcode
          (brew prefix openjdk@21)
            → Swift package resolves the framework for the actual Xcode
              destination
              → automatic signing / TestFlight distribution by Xcode Cloud
```

## Setup prerequisites

1. Use the same Apple Developer program and team configured on the iOS app:
   `Team id 47CTNRU75`.
2. Use the existing App Store Connect app record:
   `dali.hamza.ExchangeConvertApp`.
3. Use Xcode signed into the same Apple Account.
4. Xcode Cloud has access to the repository:
   `liodali/ExchangeConvertApp`.

The App Store Connect app must already have the needed capabilities because
this app ships a widget extension and rate alerts:
`App Groups` and `Push Notifications`. The existing GitHub iOS workflow
already provisions both, so no extra portal change is expected.

## Xcode Cloud scripts

Xcode Cloud does not ship a Java runtime for Kotlin Multiplatform, so the
repo adds two scripts next to the Xcode project:

```text
iosApp/ci_scripts/ci_post_clone.sh
iosApp/ci_scripts/ci_pre_xcodebuild.sh
```

Both install Homebrew's `openjdk@21`, and the pre-build resolves the
standard Homebrew prefix for JDK 21. The scheme pre-action then builds the
KMP framework with the custom `embedAndSignAppleFrameworkForXcode` task,
which also stays source-destintion aware.

## Creating the first workflow

In Xcode:

1. Open `iosApp/ExchangeConvertApp.xcodeproj`.
2. Product → Cloud → **Create Xcode Cloud Workflow**.
3. Apple signs in and connects the repository.
4. If App Store Connect already has the app, keep the existing product.
5. Name the workflow: `Dinarly iOS Archive`.
6. Start Condition: on a push to `main` and tags matching:
   - `ios-v*`
   - `ios-v*-stg`
7. Action: **Archive** (Release only).
8. Destination: generic/iOS.
9. Post-Action: **Internal TestFlight** (and optionally App Store later).

Then manually run the workflow for the first build. On success, keep
`App Store Connect → Xcode Cloud → Settings` alive for the next build
number.

## Build number note

At present, `version.xcconfig` is intentionally shared between Android and
iOS as the source of truth. Apple lets Xcode Cloud define its own next iOS
build number in App Store Connect. If you want Xcode Cloud builds to use
Apple's auto-increment, pin that next value once (e.g. 70 or higher). Future
Android build numbers can then move independently from iOS.

## Build failure hints

- `openjdk@21` not found → run `brew install openjdk@21` in the Xcode Cloud
  `ci_pre_xcodebuild.sh` script.
- Gradle build uses a JDK older than 21 → CI environment `JAVA_HOME`
  missing/not infant. Scheme pre-action sets it from the Homebrew JDK path.
- SharedKMP framework missing → confirm the scheme has `Build Kotlin
  Framework` and the pre-action executed.
- App Store Connect app not found → create the record or choose the existing
  product.
- Sharing failed → Apple-managed signing uses the team; check the developer
  account / project team and App Store Connect capabilities.
