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
standard Homebrew prefix for JDK 21. The post-clone script first calls
`iosApp/ci_scripts/prepare_secrets.sh` to generate the ignored
`iosApp/ExchangeConvertApp/Secrets.swift` before Xcode plans its build.
The scheme pre-action then builds the KMP framework with the custom
`embedAndSignAppleFrameworkForXcode` task, which also stays source-destination aware.

## Staging and production secrets in one workflow

In the workflow's **Environment** settings, add both variables and mark each
as **Secret**:

| Variable | Contents |
| --- | --- |
| `IOS_SECRETS_SWIFT_BASE64_STG` | Base64 of the staging `Secrets.swift` |
| `IOS_SECRETS_SWIFT_BASE64` | Base64 of the production `Secrets.swift` |

The script selects by Xcode Cloud's `CI_TAG`:

- A `-stg` tag selects staging: `ios-v1.0.0-stg`,
  `ios-v1.0.0-stg+69`, or `ios-v1.0.0+69-stg`.
- All other tags (for example, `ios-v1.0.0` or `ios-v1.0.0+69`) select production.
- Untagged builds, including pushes to `main`, select production. Branch names
  do not select staging; use a `-stg` tag for staging builds.

A missing, invalid, or empty selected value fails the build; it never falls
back to the other environment or the example file. Production uses
`api.exchange.dev.adetify.com`; staging uses
`exchange-api-staging.op.dev.kahuna.adetify.com`.

To copy the Base64 of the current local file without printing it:

```bash
base64 < iosApp/ExchangeConvertApp/Secrets.swift | tr -d '\n' | pbcopy
```

Prepare each environment's contents separately and paste the corresponding
value into its variable. Keep `Secrets.swift` git-ignored, do not log its
contents, and do not enable shell tracing for secrets generation. Base64 is
not encryption. This keeps configuration out of Git, but values embedded in
the compiled app are still extractable; privileged credentials belong on the
backend.

Run the secrets-selection regression tests locally with:

```bash
python3 -m unittest discover -s iosApp/ci_scripts/tests -v
```

## GitHub Actions uses separate secrets

Xcode Cloud cannot read GitHub Actions repository secrets. The workflow at
`.github/workflows/ios-testflight.yml` has the same tag-selection policy, but
uses the existing GitHub naming convention:

| Build | GitHub Actions repository secret | Xcode Cloud secret variable |
| --- | --- | --- |
| `-stg` tag (with optional `+BUILD`) | `IOS_SECRETS_SWIFT_B64_STG` | `IOS_SECRETS_SWIFT_BASE64_STG` |
| Other tags or untagged builds | `IOS_SECRETS_SWIFT_B64` | `IOS_SECRETS_SWIFT_BASE64` |

In GitHub, open **Settings → Secrets and variables → Actions**. Add
`IOS_SECRETS_SWIFT_B64_STG` with the staging file's Base64, and ensure
`IOS_SECRETS_SWIFT_B64` contains the production file's Base64. If the existing
secret currently contains staging configuration, copy that value from your
local staging file into the new staging secret before replacing production.
GitHub does not allow reading an existing secret's value back from its UI.
The selected secret is required; the workflow no longer falls back to the
example file.

## Creating the first workflow

In Xcode:

1. Open `iosApp/ExchangeConvertApp.xcodeproj`.
2. Product → Cloud → **Create Xcode Cloud Workflow**.
3. Apple signs in and connects the repository.
4. If App Store Connect already has the app, keep the existing product.
5. Name the workflow: `Dinarly iOS Archive`.
6. Start Condition: on a push to `main` and tags matching `ios-v*`.
   This single pattern includes both production and `-stg` tags; the secrets
   script selects the appropriate environment.
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
