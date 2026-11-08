#!/bin/sh
# After a successful archive: (1) STAGE the Compose resources (fonts!) into
# the archived .app — the cache fast-path restores the framework but NOT
# the resources; without them the theme throws MissingResourceException at
# launch (SIGABRT, GlitchTip Jan crash pattern). (2) ASSERT the build
# number came from version.xcconfig (Xcode Cloud's automatic-build-number
# override caught Nov 2026 as build 22). (3) Save the framework + toolchain
# to the S3 cache for the next build's fast path.
#
# The resource staging FAILS THE BUILD when it cannot find them (missing
# fonts = guaranteed crash), and the cache save stays best-effort.
set -euo pipefail

if [[ "${CI_XCODE_CLOUD:-}" != "TRUE" ]]; then
  exit 0
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

# ---- 1. stage Compose resources into the archived .app -----------------
# CI_ARCHIVE_PATH is the .xcarchive ROOT; the .app always lives at
# Products/Applications/<name>.app inside it. Resolving anything else
# silently stages resources next to the app instead of inside it (and
# the stray folder beside the .app also made xcodebuild skip the archive's
# ApplicationProperties — every export then rejects the archive: run 91,
# Nov 2026, "expected one {}").
ARCHIVE_DIR="${CI_ARCHIVE_PATH:-}"
if [[ -n "$ARCHIVE_DIR" && ! -d "$ARCHIVE_DIR" ]]; then
  echo "xc: CI_ARCHIVE_PATH=$ARCHIVE_DIR archive not found" >&2
  ARCHIVE_DIR=""
fi
APP_DIR="$ARCHIVE_DIR/Products/Applications/ExchangeConvertApp.app"
if [[ -z "$ARCHIVE_DIR" || ! -f "$APP_DIR/Info.plist" ]]; then
  echo "::error::CI_ARCHIVE_PATH did not locate the archived .app (path='${CI_ARCHIVE_PATH:-<unset>}') — refusing to guess: a font-less build crashes at launch" >&2
  exit 1
fi
echo "app archive: $APP_DIR"
echo "app archive: $APP_DIR"

if [[ -d "$APP_DIR/compose-resources" ]]; then
  echo "compose-resources already present (full Gradle ran)"
else
  RES="$(ls -d shared/build/generated/compose/resourceGenerator/assembledResources/iosArm64*/composeResources 2>/dev/null | head -1 || true)"
  if [[ -z "$RES" ]]; then
    echo "::error::compose resources missing — the app would crash at launch without fonts" >&2
    exit 1
  fi
  mkdir -p "$APP_DIR/compose-resources"
  cp -R "$RES" "$APP_DIR/compose-resources/"
  echo "staged compose resources (fonts) into the archive app"
fi

# ---- 2. build-number guard --------------------------------------------
EXPECTED="$(sed -n 's/^CURRENT_PROJECT_VERSION *= *//p' version.xcconfig | tr -d '[:space:]')"
if [[ -n "$EXPECTED" && -f "$APP_DIR/Info.plist" ]]; then
  ACTUAL="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$APP_DIR/Info.plist" 2>/dev/null || true)"
  echo "build number check: xcconfig=$EXPECTED archive=$ACTUAL"
  # An EMPTY CFBundleVersion is as broken as a mismatched one — the upload
  # would be rejected (or worse, archived without export eligibility).
  if [[ -z "$ACTUAL" ]]; then
    echo "::error::Build number MISSING from the archived .app Info.plist — nothing would upload" >&2
    exit 1
  fi
  if [[ "$ACTUAL" != "$EXPECTED" ]]; then
    echo "::error::Build number mismatch — the archive is $ACTUAL but version.xcconfig says $EXPECTED (automatic build number is probably ON: workflow → Actions → Archive → Build Number)." >&2
    exit 1
  fi
fi

# ---- 3. cache save ------------------------------------------------------
(. "$SCRIPT_DIR/ci_cache.sh"; xc_cache_save) \
  || echo "xc-cache: save failed — the next build just runs cold"
