# Shared app version

Edit [`version.xcconfig`](../version.xcconfig) in the repository root:

```xcconfig
MARKETING_VERSION = 0.7.2
CURRENT_PROJECT_VERSION = 53
```

| Shared setting | Android | iOS |
|---|---|---|
| `MARKETING_VERSION` | `versionName` | `CFBundleShortVersionString` |
| `CURRENT_PROJECT_VERSION` | `versionCode` | `CFBundleVersion` |

The Android Gradle build and the Xcode app target's Debug/Release configurations
read this file directly. Both release workflows also read it, so local builds,
manual workflow runs, and tagged releases use the same version and build number.
No framework regeneration is needed just to change the app version.

Use a numeric `X.Y.Z` version and an integer build number from 1 to 2100000000.
Increment the build number for every new store upload, even when keeping the
same app version. Start the next upload above the highest number already used
on either store; the initial shared value preserves iOS's existing build 53.
The existing Google Play preflight still requires a build 1–3 above the highest
active Play version code. No automatic run-number or App Store build-number
fallback replaces the committed build number.

Check the values without building:

```bash
python3 script/app-version.py
```

## Releases

1. Edit `version.xcconfig` (for example, version `0.7.3`, build `54`).
2. Commit the file with the release changes.
3. Run either workflow manually, or create the matching platform tags on that commit:

```bash
git tag -a app-v0.7.3+54 -m "Sovereign Ledger 0.7.3 (build 54)"
git tag -a ios-v0.7.3+54 -m "Sovereign Ledger 0.7.3 (build 54)"
git push origin app-v0.7.3+54 ios-v0.7.3+54
```

Tags trigger releases and provide Android release notes. Tags with `+BUILD`
must match both values in the shared file; tags without `+BUILD` must match the
version and use the file's build number. A `-dev` tag marker is accepted but does
not alter the numeric app version on either platform. To distribute an Android
development build, select the appropriate testing track on a manual run.

Explicit command-line overrides (`-PversionName`/`-PversionCode` for Gradle,
`MARKETING_VERSION`/`CURRENT_PROJECT_VERSION` for Xcode) remain available for ad hoc
builds. Neither `local.properties` nor the latest git tag controls the default
app version anymore. Change `version.xcconfig` for normal releases.
