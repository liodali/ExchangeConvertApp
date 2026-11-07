#!/bin/sh
# Xcode Cloud ↔ GarageHQ (S3) build cache — ports the GitHub Actions cache
# design (.github/workflows/ios-testflight.yml) so Xcode Cloud builds skip
# the two biggest cold-start costs: the Kotlin/Native toolchain download
# (~1-2 min) and the whole Gradle framework build (~3-5 min).
#
# Every operation is BEST-EFFORT: any failure degrades to a cold build and
# never fails the release.
#
# Required Xcode Cloud environment (workflow → Environment):
#   XC_S3_ENDPOINT  https://…   (plain variable)
#   XC_S3_BUCKET    name       (plain variable)
#   XC_S3_KEY_ID    access key (secret)
#   XC_S3_SECRET    secret key (secret)
# Missing any of them silently disables the cache.
#
# Object layout (namespaced: Xcode Cloud keys are content-hashed with our
# own digest, independent of GitHub's hashFiles):
#   xc-konan/macos/<sha(libs.versions.toml)>/konan.tgz   → ~/.konan
#   xc-fw/macos/<sha(shared sources + build files)>/fw.tgz
#     → shared/build/xcode-frameworks
#       shared/build/generated/compose/resourceGenerator
#       + .xc-fw-hit marker (the scheme pre-action skips Gradle on it)

MC_BIN="${HOME}/.local/bin/mc"
FW_MARKER="shared/build/xcode-frameworks/.xc-fw-hit"
KONAN_HIT_MARKER="/tmp/.xc-konan-hit"

xc_cache_enabled() {
  [ -n "${XC_S3_ENDPOINT:-}" ] && [ -n "${XC_S3_BUCKET:-}" ] &&
    [ -n "${XC_S3_KEY_ID:-}" ] && [ -n "${XC_S3_SECRET:-}" ]
}

mc_boot() {
  [ -x "$MC_BIN" ] || {
    mkdir -p "$(dirname "$MC_BIN")" || return 1
    curl -fsSL "https://dl.min.io/client/mc/release/darwin-arm64/mc" \
      -o "$MC_BIN" 2>/dev/null || return 1
    chmod +x "$MC_BIN" || return 1
  }
  "$MC_BIN" alias set garage "$XC_S3_ENDPOINT" "$XC_S3_KEY_ID" "$XC_S3_SECRET" \
    >/dev/null 2>&1
}

# Content digest over exactly the inputs that define the framework output:
# Kotlin sources, SQLDelight schemas, module build script, version catalog.
fw_key() {
  {
    find shared/src \( -name '*.kt' -o -name '*.sq' \) | LC_ALL=C sort
    printf '%s\n' shared/build.gradle.kts gradle/libs.versions.toml
  } | while IFS= read -r f; do
    [ -f "$f" ] && cat "$f"
  done | shasum -a 256 | cut -d' ' -f1
}

konan_key() {
  shasum -a 256 gradle/libs.versions.toml | cut -d' ' -f1
}

# Called from ci_post_clone.sh, with the repo root as CWD.
xc_cache_restore() {
  xc_cache_enabled || { echo "xc-cache: disabled (environment incomplete)"; return 0; }
  mc_boot || { echo "xc-cache: mc bootstrap failed — cold build"; return 0; }

  KONAN_OBJ="garage/${XC_S3_BUCKET}/xc-konan/macos/$(konan_key)/konan.tgz"
  if "$MC_BIN" cp "$KONAN_OBJ" /tmp/konan.tgz >/dev/null 2>&1 &&
    tar -xzf /tmp/konan.tgz -C "$HOME" 2>/dev/null; then
    : > "$KONAN_HIT_MARKER"
    echo "xc-cache: konan toolchain restored"
  else
    echo "xc-cache: konan miss (cold toolchain download)"
  fi
  rm -f /tmp/konan.tgz

  FW_OBJ="garage/${XC_S3_BUCKET}/xc-fw/macos/$(fw_key)/fw.tgz"
  if "$MC_BIN" cp "$FW_OBJ" /tmp/fw.tgz >/dev/null 2>&1 &&
    tar -xzf /tmp/fw.tgz -C . 2>/dev/null &&
    ls -d shared/build/xcode-frameworks/*/*/shared.framework >/dev/null 2>&1; then
    mkdir -p shared/build/xcode-frameworks
    : > "$FW_MARKER"
    echo "xc-cache: shared framework restored — Gradle will be skipped"
  else
    echo "xc-cache: framework miss — Gradle will build it"
  fi
  rm -f /tmp/fw.tgz
  return 0
}

# Called from ci_post_xcodebuild.sh (after a successful archive), repo root CWD.
xc_cache_save() {
  xc_cache_enabled || return 0
  [ -d shared/build/xcode-frameworks ] || { echo "xc-cache: nothing to save"; return 0; }
  mc_boot || return 0

  if [ ! -f "$FW_MARKER" ]; then
    FW_OBJ="garage/${XC_S3_BUCKET}/xc-fw/macos/$(fw_key)/fw.tgz"
    if tar -czf /tmp/fw.tgz shared/build/xcode-frameworks \
      shared/build/generated/compose/resourceGenerator 2>/dev/null ||
      tar -czf /tmp/fw.tgz shared/build/xcode-frameworks; then
      "$MC_BIN" cp /tmp/fw.tgz "$FW_OBJ" >/dev/null 2>&1 &&
        echo "xc-cache: framework saved for the next build"
    fi
    rm -f /tmp/fw.tgz
  else
    echo "xc-cache: framework was cache-restored, not saving"
  fi

  # konan only on runs that downloaded the toolchain cold (~1-2 GB upload
  # is worth it exactly once per Kotlin/toolchain bump)
  if [ ! -f "$KONAN_HIT_MARKER" ] && [ -d "${HOME}/.konan" ]; then
    KONAN_OBJ="garage/${XC_S3_BUCKET}/xc-konan/macos/$(konan_key)/konan.tgz"
    if ! "$MC_BIN" ls "$KONAN_OBJ" >/dev/null 2>&1; then
      if tar -czf /tmp/konan.tgz -C "$HOME" .konan 2>/dev/null; then
        "$MC_BIN" cp /tmp/konan.tgz "$KONAN_OBJ" >/dev/null 2>&1 &&
          echo "xc-cache: konan toolchain saved"
      fi
      rm -f /tmp/konan.tgz
    fi
  fi
  return 0
}
