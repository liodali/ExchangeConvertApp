#!/bin/sh
# Xcode Cloud prerequisites: generate Secrets.swift, resolve JDK 21, restore
# the S3 build cache (konan toolchain + shared framework).
#
# Xcode Cloud starts clean. The ExchangeConvertApp scheme pre-action builds
# `:shared:embedAndSignAppleFrameworkForXcode` inside the normal
# `xcodebuild` action; everything here prepares that build.
set -euo pipefail

if [[ "${CI_XCODE_CLOUD:-}" != "TRUE" ]]; then
  # Local runs never need this expensive setup.
  exit 0
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
/bin/sh "$SCRIPT_DIR/prepare_secrets.sh"

# Prefer an image-provisioned JDK 21 (instant) over a Homebrew install
# (1-3 min on a fresh VM). Homebrew stays the fallback.
resolve_jdk21() {
  local home
  home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [[ -n "$home" && -d "$home" ]]; then
    echo "$home"
    return 0
  fi
  home="$(brew --prefix openjdk@21 2>/dev/null || true)/libexec/openjdk.jdk/Contents/Home"
  if [[ -n "$home" && -d "$home" ]]; then
    echo "$home"
    return 0
  fi
  return 1
}

if ! JDK_HOME="$(resolve_jdk21)"; then
  echo "Installing JDK 21 for Kotlin/Native..."
  brew install openjdk@21
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi

if [[ ! -d "$JDK_HOME" ]]; then
  echo "error: JDK 21 not found at $JDK_HOME after installation" >&2
  exit 1
fi
echo "JDK 21 ready: $JDK_HOME"

# Best-effort cache restore — never blocks the build on a cache problem.
cd "$REPO_ROOT"
(. "$SCRIPT_DIR/ci_cache.sh"; xc_cache_restore) \
  || echo "xc-cache: restore failed — continuing with a cold build"
