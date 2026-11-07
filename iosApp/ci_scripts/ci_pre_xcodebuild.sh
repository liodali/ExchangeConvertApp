#!/bin/sh
# Last chance to resolve JDK 21 before Xcode Cloud runs xcodebuild. The
# scheme pre-action resolves the JDK path itself (image JVM first, then
# Homebrew), because variables exported here do not survive into a later
# script process.
set -euo pipefail

if [[ "$CI_XCODE_CLOUD" != "TRUE" ]]; then
  exit 0
fi

JDK_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
if [[ -z "$JDK_HOME" || ! -d "$JDK_HOME" ]]; then
  JDK_HOME="$(brew --prefix openjdk@21 2>/dev/null || true)/libexec/openjdk.jdk/Contents/Home"
fi

if [[ -z "$JDK_HOME" || ! -d "$JDK_HOME" ]]; then
  echo "Installing JDK 21 before the framework build..."
  brew install openjdk@21
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi

if [[ -d "$JDK_HOME" ]]; then
  echo "JDK 21 ready: $JDK_HOME"
else
  echo "error: JDK 21 missing at $JDK_HOME before xcodebuild" >&2
  exit 1
fi
