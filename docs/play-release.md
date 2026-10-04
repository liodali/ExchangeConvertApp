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

## 4. Ship it — version & build number come from the tag

Tag scheme: **`app-vX.Y.Z+BUILD[-dev]`** — the tag is the single source of truth:

| Tag | versionName | versionCode |
|---|---|---|
| `app-v1.0.1+42` | `1.0.1` | `42` |
| `app-v1.0.1+43-dev` | `1.0.1-dev` | `43` |
| `app-v1.0.1-dev+44` | `1.0.1-dev` | `44` (dev before `+` also accepted) |
| `app-v1.0.1` (no `+BUILD`) | `1.0.1` | run number (warning logged) |

```bash
GIT_EDITOR=true git tag -a app-v1.0.1+42 -m "Sovereign Ledger 1.0.1 (build 42)"
git push origin app-v1.0.1+42
```

Keep `BUILD` strictly increasing (Play requires it for every upload) — a simple
`+1` per release is all it takes. The workflow validates the Play limit
(2 100 000 000) and falls back to the run number with a warning when `+BUILD`
is missing. Testers get the opt-in link from Console → Internal testing;
promote internal → closed → production from the Console when ready.

## Listing assets (ready in `qa/store/`)

- Store icon `app-icon-512.png` ✅ (512×512 full-bleed, Play masks corners)
- Feature graphic `feature-graphic-1024x500.png` ✅ (bars mark + wordmark)
- App icon: the adaptive + Material You monochrome icon ships with the app
- Phone screenshots: capture from the device during the QA pass (min 2, 16:9/9:16)

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
- Version scheme: `app-vX.Y.Z+BUILD[-dev]` — both versionName and versionCode
  are defined by the tag; the workflow derives and validates them.
- Manual runs (`workflow_dispatch`): the "Run workflow" form takes a required
  `version_code` (same 1–3-above-Play rule applies) and an optional `track`
  (default `internal`; any closed-testing track must already exist in Play
  Console — the check fails with the list of available tracks otherwise).
  Manual builds are versioned `dev.<versionCode>`.
