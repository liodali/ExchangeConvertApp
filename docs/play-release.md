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

The script writes the `signing.*` entries into `local.properties` —
`app/build.gradle.kts` picks them up (env vars take precedence, which is what
CI uses). **Back up the keystore + key.properties somewhere safe** (lose them →
the app can never be updated on Play).

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

- Store icon `app-icon-512.png` ✅ (512×512 full-bleed, Play masks corners)
- Feature graphic `feature-graphic-1024x500.png` ✅ (bars mark + wordmark)
- App icon: the adaptive + Material You monochrome icon ships with the app
- Phone screenshots: capture from the device during the QA pass (min 2, 16:9/9:16)

## Notes

- Debug builds install as `com.sovereignledger.app.debug` (separate from release).
- The AAB artifact is attached to every workflow run even without Play secrets.
- `docs:` the version scheme is `app-vMAJOR.MINOR.PATCH`; `versionCode` always
  comes from the CI run number so it strictly increases.
