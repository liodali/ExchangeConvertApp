#!/bin/sh
# Xcode Cloud Kotlin Multiplatform prerequisite: install JDK 21.
#
# Xcode Cloud starts clean, has Homebrew, but does not prime the JDK needed
# by Gradle to run the shared framework task. The ExchangeConvertApp scheme
# pre-action builds `:shared:embedAndSignAppleFrameworkForXcode` inside the
# normal `xcodebuild` action, so this script only adds the prerequisite.
set -euo pipefail

if [[ "$CI_XCODE_CLOUD" != "TRUE" ]]; then
  # Local runs never need this expensive setup.
  exit 0
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "error: Homebrew is unavailable in this Xcode Cloud image" >&2
  exit 1
fi

JDK_PREFIX="$(brew --prefix openjdk@21 2>/dev/null || true)"
JDK_HOME="${JDK_PREFIX}/libexec/openjdk.jdk/Contents/Home"

if [[ -z "$JDK_HOME" || ! -d "$JDK_HOME" ]]; then
  echo "Installing JDK 21 for Kotlin/Native..."
  brew install openjdk@21
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi

if [[ ! -d "$JDK_HOME" ]]; then
  echo "error: JDK 21 not found at $JDK_HOME after installation" >&2
  exit 1
fi

echo "JDK 21 ready: $JDK_HOME"
