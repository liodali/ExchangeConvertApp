# Play "What's new" — committed translations

The Android Release workflow injects these files into the Play upload for
**every `app-v*` tag release** (one file per store-listing language).

Rules — breaking any of them fails the Play upload:

1. **Filename = Play Console listing language code**: `de-DE`, `it-IT`,
   `es-419`, `pt-BR`, `ar`, … Verbatim, no extra files.
2. **Each file states THIS release's what's-new, ≤ 500 chars** (Play's hard
   cap — the pipeline truncates to 500 bytes; keep well under to avoid
   cutting multibyte text mid-glyph).
3. **`en-US` is reserved** — it always comes from the git **tag message**
   (GitHub release body + en-US What's-new). Never add `en-US` here.
4. **Only languages configured in Play Console may appear here.** A file for
   a language the Console listing does not have is rejected by the upload
   (currently: also `es-ES` vs `es-419` — keep only the one(s) the Console
   actually lists; the other is a paste-in for manual console edits).

Per release: rewrite every file's text for that release, one commit together
with the `version.xcconfig` bump, then tag.

Workflow wiring: `.github/workflows/android-release.yml`
("Prepare release notes (whatsnew)" step).
