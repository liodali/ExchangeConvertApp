# Google Play release pipeline

`app-v*` tag → GitHub Actions → **signed release AAB** → **internal testing track**.

- Workflow: [`.github/workflows/android-release.yml`](../.github/workflows/android-release.yml)
- Store identity: `com.sovereignledger.app` · display name **Sovereign Ledger**
- Privacy policy (live after backend ≥ v0.4.3-dev.18): https://api.exchange.dev.adetify.com/privacy

## 1. One-time keystore (local, ~2 min)

```bash
keytool -genkeypair -v \
  -keystore sovereign-ledger-release.jks \
  -alias sovereign-ledger \
  -keyalg RSA -keysize 4096 -validity 10000
```

**Back this file + passwords up somewhere safe** (lose it → you can never update
the app). It is never committed. For local release builds add to
`local.properties`:

```properties
signing.keystore.path=/absolute/path/to/sovereign-ledger-release.jks
signing.store.password=…
signing.key.alias=sovereign-ledger
signing.key.password=…
```

## 2. GitHub secrets (repo → Settings → Secrets → Actions)

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -i sovereign-ledger-release.jks` output |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `sovereign-ledger` |
| `ANDROID_KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | service-account JSON (step 3) |

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

## 4. Ship it

```bash
GIT_EDITOR=true git tag -a app-v1.0.0 -m "Sovereign Ledger 1.0.0"
git push origin app-v1.0.0
```

GitHub Actions builds the signed AAB (`versionCode` = run number, `versionName`
from the tag) and uploads it to the internal track. Testers get the opt-in link
from Console → Internal testing. Promote internal → closed → production from the
Console when ready.

## Listing assets (ready in `qa/store/`)

- Feature graphic `feature-graphic-1024x500.png` ✅ (logo mark + wordmark)
- App icon: the adaptive icon ships with the app (dark tile, gold rate-line)
- Phone screenshots: capture from the device during the QA pass (min 2, 16:9/9:16)

## Notes

- Debug builds install as `com.sovereignledger.app.debug` (separate from release).
- The AAB artifact is attached to every workflow run even without Play secrets.
- `docs:` the version scheme is `app-vMAJOR.MINOR.PATCH`; `versionCode` always
  comes from the CI run number so it strictly increases.
