#!/bin/sh
# Last chance to install JDK 21 before Xcode Cloud runs xcodebuild. The
# scheme pre-action resolves the Homebrew JDK path itself, because variables
# exported here do not normally survive into a later script process.
set -euo pipefail

if [[ "$CI_XCODE_CLOUD" != "TRUE" ]]; then
  exit 0
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "error: Homebrew is unavailable in this Xcode Cloud image" >&2
  exit 1
fi

JDK_PREFIX="$(brew --prefix openjdk@21 2>/dev/null || true)"
JDK_HOME="${JDK_PREFIX}/libexec/openjdk.jdk/Contents/Home"

if [[ -z "$JDK_HOME" || ! -d "$JDK_HOME" ]]; then
  echo "Installing JDK 21 before the framework build..."
  brew install openjdk@21
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi

if [[ -d "$JDK_HOME" ]]; then
  echo "JDK 21 ready: $JDK_HOME"
else
  echo "error: JDK 21 mold at $JDK_HOME before xcodebuild" >&2
  exit 1
fi
