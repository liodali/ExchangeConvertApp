#!/usr/bin/env bash
#
# Sovereign Ledger — local build & debug helper (CLI)
#
#   script/dev.sh connect    adb-connect the phone (WiFi; scans the subnet
#                            if the default IP fails)
#   script/dev.sh build      assembleDebug
#   script/dev.sh install    build + install on the connected device
#   script/dev.sh run        install + launch the app
#   script/dev.sh release    signed local release (key.properties) + verify
#   script/dev.sh logs       live logcat for the running app
#   script/dev.sh all        connect → run → logs
#
# Override the target with env vars:
#   DEVICE=192.168.178.85:5555 script/dev.sh run
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DEVICE="${DEVICE:-192.168.178.85:5555}"
APP_ID="com.sovereignledger.app.debug"
ACTIVITY="dali.hamza.echangecurrencyapp.ui.MainActivity"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
AAPT="$HOME/Desktop/Android/sdk/build-tools/37.0.0/aapt"
APKSIGNER="$HOME/Desktop/Android/sdk/build-tools/37.0.0/apksigner"

say() { printf '\033[0;36m▸\033[0m %s\n' "$*"; }
fail() { printf '\033[0;31m✗\033[0m %s\n' "$*" >&2; exit 1; }

serial() {
  if adb devices | grep -q "^${DEVICE%%:*}.*device$"; then
    echo "$DEVICE"; return
  fi
  adb devices | awk '/device$/{print $1; exit}'
}

do_connect() {
  say "connecting to $DEVICE …"
  if adb connect "$DEVICE" 2>/dev/null | grep -q "connected"; then
    say "connected: $DEVICE"; return
  fi
  say "default device unreachable — scanning the local subnet"
  local found
  found=$(python3 - <<'EOF'
import socket, concurrent.futures
def check(ip):
    s = socket.socket(); s.settimeout(0.5)
    try: s.connect((ip, 5555)); s.close(); return ip
    except Exception: return None
hosts = [f"192.168.178.{i}" for i in range(2, 255)]
with concurrent.futures.ThreadPoolExecutor(max_workers=100) as ex:
    hits = [r for r in ex.map(check, hosts) if r]
print(hits[0] if hits else "")
EOF
)
  if [ -n "$found" ]; then
    adb connect "$found:5555" >/dev/null 2>&1 || true
    say "connected: $found:5555"
  else
    fail "phone not found — check WiFi / Wireless debugging"
  fi
}

require_device() {
  local s; s="$(serial)"
  if [ -z "$s" ]; then fail "no device — run: script/dev.sh connect"; fi
  echo "$s"
}

do_build() {
  say "building debug…"
  ./gradlew :app:assembleDebug -q
  say "built: $DEBUG_APK"
}

do_install() {
  local s; s="$(require_device)"
  do_build
  adb -s "$s" install -r -t "$DEBUG_APK" >/dev/null
  say "installed on $s"
}

do_run() {
  local s; s="$(require_device)"
  do_install
  adb -s "$s" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
  adb -s "$s" shell am start -W -n "$APP_ID/$ACTIVITY" >/dev/null
  say "launched $APP_ID — streaming logs (Ctrl-C to detach; app keeps running)"
  stream_logs "$s"
}

# pid + logcat attach (waits briefly for the process to come up)
stream_logs() {
  local s="$1" pid=""
  local i
  for i in 1 2 3 4 5; do
    pid="$(adb -s "$s" shell pidof "$APP_ID" | tr -d '')"
    if [ -n "$pid" ]; then break; fi
    sleep 1
  done
  if [ -z "$pid" ]; then fail "app process not found"; fi
  adb -s "$s" logcat --pid="$pid"
}

do_release() {
  local version_name="${VERSION_NAME:-0.1.0-local}"
  local version_code="${VERSION_CODE:-900001}"
  say "building signed release ($version_name / $version_code)…"
  ./gradlew :app:assembleRelease -q -PversionCode="$version_code" -PversionName="$version_name"
  if [ -x "$AAPT" ]; then
    "$AAPT" dump badging "$RELEASE_APK" 2>/dev/null | grep -E "^package|targetSdkVersion" || true
  fi
  if [ -x "$APKSIGNER" ]; then
    "$APKSIGNER" verify "$RELEASE_APK" 2>/dev/null && say "signature verified"
  fi
  say "built: $RELEASE_APK ($(shasum -a 256 "$RELEASE_APK" | cut -d' ' -f1))"
}

do_logs() {
  local s; s="$(require_device)"
  say "streaming logcat for $APP_ID — Ctrl-C to stop"
  stream_logs "$s"
}

case "${1:-}" in
  connect) do_connect ;;
  build)   do_build ;;
  install) do_install ;;
  run)     do_run ;;
  release) do_release ;;
  logs)    do_logs ;;
  all)     do_connect; do_run; do_logs ;;
  *)       grep '^#   ' "$0" | sed 's/^#   //; s/^#//'; exit 1 ;;
esac
