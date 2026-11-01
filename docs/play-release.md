# Google Play release pipeline

`app-v*` tag → GitHub Actions → **signed release AAB** → **internal testing track**.

- Workflow: [`.github/workflows/android-release.yml`](../.github/workflows/android-release.yml)
- Store identity: `com.sovereignledger.app` · display name **Sovereign Ledger**
- Privacy policy (live after backend ≥ v0.4.3-dev.18): https://api.exchange.dev.adetify.com/privacy

## 1. Keystore — managed by `script/keystore.sh`

Configuration lives in **`script/key.properties`** (gitignored; bootstrapped
with random passwords on first run). The keystore lives in the central Android
keys folder, outside the repo:

```
KEYSTORE_PATH=/Users/dalihamza/Desktop/FlutterApps/androidKeys/exchangerate-app/sovereign-ledger-release.jks
```

```bash
./script/keystore.sh            # generate if missing + sync local.properties
./script/keystore.sh verify     # show the certificate
./script/keystore.sh secrets    # print the GitHub secret values to paste
```

`app/build.gradle.kts` reads signing straight from `key.properties`
(CI env vars take precedence) — Android Studio can wipe `local.properties` all
it wants, signing is unaffected. **Back up the keystore + key.properties
somewhere safe** (lose them → the app can never be updated on Play).

## 2. GitHub secrets (repo → Settings → Secrets → Actions)

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -i sovereign-ledger-release.jks` output |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `sovereign-ledger` |
| `ANDROID_KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | service-account JSON (step 3) |
| `GLITCHTIP_DSN` | GlitchTip client DSN (`https://…@glitchtip.dev.adetify.com/N`) — crash reporting; releases without it simply ship without telemetry |

Local builds read the same DSN from `local.properties` (`glitchtip.dsn=`); empty = disabled.

### Backend host (optional repo variable)

The app talks to our exchange-api backend. The host defaults to the shared
module's `DEFAULT_HOST` (`api.exchange.dev.adetify.com` — currently the only
deployment). To point a release at a different host without a code change:

- **CI**: repo variable `EXCHANGE_SERVER_HOST` (Settings → Secrets and
  variables → Actions → **Variables**, not Secrets) — unset = default host.
- **Local builds**: `local.properties` → `server.host=…` (or `-Pserver.host=…`).

When the production backend gets its own domain, set the variable and tag —
that's the whole cutover.

## 3. Play Console one-time setup (~15 min, needs your Google account)

1. [play.google.com/console](https://play.google.com/console) → pay the one-time $25 fee.
2. **Create app**: name `Sovereign Ledger`, default language, app, free.
3. **Play App Signing**: when first uploading choose *Export and upload a key from Java keystore* (or let Google manage — then the CI uploads stay identical; recommended: enroll OUR key).
4. **Service account** (needed for CI uploads):
   - Play Console → Setup → API access → *Create new service account* → follow the Google Cloud link → create a service account with **no project roles** → add a JSON key → download it.
   - Back in Play Console, *Grant access* to that service account with at least **Release apps to testing tracks** permission.
   - The JSON file becomes the `PLAY_SERVICE_ACCOUNT_JSON` secret.
5. **App content** (left menu, all required before first release):
   - Privacy policy URL → `https://api.exchange.dev.adetify.com/privacy`
   - Data safety → fill in (answer: collects **no data**; see the policy page)
   - Ads: no ads · Content rating questionnaire · Target audience 18+ · App access: all functionality available
6. **Internal testing** track → add testers (emails or a Google Group).

## 4. Ship it — shared version and build number

Edit [`version.xcconfig`](../version.xcconfig) in the repository root. It defines
both Android and iOS versions; see [Shared app version](app-versioning.md).

```xcconfig
MARKETING_VERSION = 0.7.3
CURRENT_PROJECT_VERSION = 54
```

Commit that file, then run the workflow manually or create a matching tag
(the tag message is the release notes — see "What's new" below):

```bash
git tag -a app-v0.7.3+54 -F whatsnew.xml
git push origin app-v0.7.3+54
```

Tags trigger releases; they are checked against the file and cannot override
it. A tag without `+BUILD` uses the shared build number. `-dev` markers are
accepted but do not alter the app's numeric version. Increment the build number
before every new upload. Testers get the opt-in link from Console → Internal
testing; promote internal → closed → production from the Console when ready.

## Listing assets (ready in `qa/store/`)

- Store icon `app-icon-512.png` ✅ (512×512 full-bleed, Play masks corners)
- Feature graphic `feature-graphic-1024x500.png` ✅ (bars mark + wordmark)
- App icon: the adaptive + Material You monochrome icon ships with the app
- Phone screenshots: capture from the device during the QA pass (min 2, 16:9/9:16)

## What's new — languages

- One local file: [`whatsnew.xml`](../whatsnew.xml) at the repo root —
  **git-ignored**, never committed. Play Console multi-language format:

  ```xml
  <en-US>
  English release notes …
  </en-US>
  <de-DE>
  Deutsche Hinweise …
  </de-DE>
  ```

- Make it the tag message (`git tag -a app-vX.Y.Z+N -F whatsnew.xml`).
  CI parses the blocks with [`script/whatsnew.py`](../script/whatsnew.py)
  into one file per language, capped at Play's 500 **bytes** (UTF-8-safe
  truncation, over-cap blocks warn in the log). The full en-US block also
  becomes the GitHub release body.
- Only include languages configured in the Console listing (`de-DE`,
  `it-IT`, `es-419`, `pt-BR`, `pt-PT`, `ar`, …) — unlisted languages fail
  the upload. `es-ES` vs `es-419`: keep only what the Console lists; the
  other is for manual Console pastes.
- Legacy plain-text tag messages still work (→ en-US only). Manual
  workflow runs fall back to "Internal testing build."
- Preview/validate locally before tagging:
  `python3 script/whatsnew.py whatsnew.xml --out whatsnew`

## Native crash symbols

Release builds set `ndk.debugSymbolLevel = "FULL"` in `app/build.gradle.kts`.
AGP embeds available native debug metadata in the AAB, so the existing Play
upload also uploads symbols; no separate CI upload is needed.

Build with `./gradlew :app:bundleRelease --info`. To check the resulting bundle:

```bash
unzip -l app/build/outputs/bundle/release/app-release.aab | grep debugsymbols
```

Only use symbols from the exact build uploaded to Play. This does not replace
the R8 `mapping.txt` used for Java/Kotlin stack traces.

Some dependencies ship stripped native libraries: AGP cannot reconstruct their
symbols. Local verification extracted DataStore's `libdatastore_shared_counter.so`
symbols for all four ABIs, but reported `libsentry.so`, `libsentry-android.so`,
and `libandroidx.graphics.path.so` as already stripped. Full symbolication for
those libraries requires matching symbols from their providers; enabling FULL
does not recover them. Do not disable native library stripping just to hide the
Play warning.

## Notes

- Debug builds install as `com.sovereignledger.app.debug` (separate from release).
- The AAB artifact is attached to every workflow run even without Play secrets.
- When the Play upload runs, an upload or rollout failure fails the workflow
  and prevents publication of the GitHub release. The AAB remains available
  as a workflow artifact for inspection.
- Before building for a Play upload, the workflow reads all Play tracks and
  requires the new version code to exceed the highest current code by 1–3.
  Equal, lower, and larger jumps fail, as do authentication/API errors.
  With no existing releases, only codes 1–3 are accepted. Artifact-only runs
  without signing or Play credentials skip this check.
  Limitation: the check reads *active* releases only — codes consumed by
  since-deleted releases are invisible to it, so a green check does not
  guarantee Play will accept the rollout; the upload step is the backstop.
- Version and build number come from root `version.xcconfig` for both platforms.
- Manual runs (`workflow_dispatch`) use the shared version/build and an optional
  `track` (default `internal`; any closed-testing track must already exist in Play
  Console — the check fails with the list of available tracks otherwise).
