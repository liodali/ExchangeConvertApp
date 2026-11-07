#!/bin/sh
# After a successful archive: (1) ASSERT the build number came from
# version.xcconfig — Xcode Cloud's "automatic build number" passes its own
# counter as an xcodebuild argument, which silently overrides
# CURRENT_PROJECT_VERSION from the lower-precedence xcconfig (uploads then
# appear as build 22 instead of 74+, Nov 2026). (2) Save the built Kotlin
# framework (and, on the run that downloaded it, the Kotlin/Native
# toolchain) to the S3 cache so the next build skips the Gradle build.
#
# The assertion fails the build on a REAL number mismatch; if the archive
# can't be located the guard logs and continues (never blocks on its own
# plumbing). The cache save is best-effort regardless.
set -euo pipefail

if [[ "${CI_XCODE_CLOUD:-}" != "TRUE" ]]; then
  exit 0
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

# ---- 1. build-number guard --------------------------------------------
EXPECTED="$(sed -n 's/^CURRENT_PROJECT_VERSION *= *//p' version.xcconfig | tr -d '[:space:]')"
APP_PATH=""
for CANDIDATE in \
  "${CI_XCODEBUILD_ARCHIVE:-}/Products/Applications"/*.app \
  "${CI_XCODEBUILD_DERIVED_DATA_PATH:-}"/Build/Products/*/*-iphoneos/*.app; do
  if [[ -d "$CANDIDATE" ]]; then
    APP_PATH="$CANDIDATE"
    break
  fi
done

if [[ -n "$APP_PATH" && -n "$EXPECTED" ]]; then
  ACTUAL="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$APP_PATH/Info.plist" 2>/dev/null || true)"
  echo "build number check: xcconfig=$EXPECTED archive=$ACTUAL"
  if [[ -n "$ACTUAL" && "$ACTUAL" != "$EXPECTED" ]]; then
    echo "::error::Build number mismatch — the archive is $ACTUAL but version.xcconfig says $EXPECTED. Xcode Cloud's automatic build number is probably ON (workflow → Actions → Archive → Build Number). Turn it OFF so version.xcconfig rules." >&2
    exit 1
  fi
else
  echo "build number check: archive not found (guard skipped) expected=$EXPECTED"
fi

# ---- 2. cache save ------------------------------------------------------
(. "$SCRIPT_DIR/ci_cache.sh"; xc_cache_save) \
  || echo "xc-cache: save failed — the next build just runs cold"
