#!/usr/bin/env bash
#
# Sovereign Ledger — release keystore (JKS) manager
#
#   script/keystore.sh            generate the keystore if missing + sync local.properties
#   script/keystore.sh verify     show the keystore certificate
#   script/keystore.sh secrets    print the GitHub Actions secret values to paste
#
# Configuration lives in .env at the repo root (see .env.example).
# The keystore itself is NEVER committed (.gitignore blocks .env and keystores/).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/script/key.properties"
LOCAL_PROPERTIES="$ROOT/local.properties"

# ---- defaults -------------------------------------------------------------
KEYSTORE_PATH="${KEYSTORE_PATH:-$ROOT/keystores/sovereign-ledger-release.jks}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}"
KEY_ALIAS="${KEY_ALIAS:-sovereign-ledger}"
KEY_PASSWORD="${KEY_PASSWORD:-}"

# ---- load .env (KEY=VALUE lines, # comments) ------------------------------
if [[ -f "$ENV_FILE" ]]; then
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue
    case "$key" in
      KEYSTORE_PATH) KEYSTORE_PATH="$value" ;;
      KEYSTORE_PASSWORD) KEYSTORE_PASSWORD="$value" ;;
      KEY_ALIAS) KEY_ALIAS="$value" ;;
      KEY_PASSWORD) KEY_PASSWORD="$value" ;;
    esac
  done < <(grep -v '^[[:space:]]*$' "$ENV_FILE")
fi

random_password() {
  LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 24 || true
}

# bootstrap key.properties with generated secrets on first run
ensure_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-$(random_password)}"
    KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
    cat > "$ENV_FILE" <<EOF
# Sovereign Ledger release signing — LOCAL ONLY (gitignored).
# Back this file AND the keystore up somewhere safe: losing them means
# the app can never be updated on Google Play.
KEYSTORE_PATH=$KEYSTORE_PATH
KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$KEY_PASSWORD
EOF
    chmod 600 "$ENV_FILE"
    echo "created $ENV_FILE with generated passwords — BACK IT UP"
  fi
  # fill anything still missing (if-form: `[[ ]] && x` trips set -e)
  if [[ -z "$KEYSTORE_PASSWORD" ]]; then KEYSTORE_PASSWORD="$(random_password)"; fi
  if [[ -z "$KEY_ALIAS" ]]; then KEY_ALIAS="sovereign-ledger"; fi
  if [[ -z "$KEY_PASSWORD" ]]; then KEY_PASSWORD="$KEYSTORE_PASSWORD"; fi
}

keytool_bin() {
  if command -v keytool >/dev/null 2>&1; then echo keytool
  elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/keytool" ]]; then echo "$JAVA_HOME/bin/keytool"
  else echo "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
  fi
}

generate() {
  ensure_env
  if [[ -f "$KEYSTORE_PATH" ]]; then
    echo "keystore already exists: $KEYSTORE_PATH"
  else
    mkdir -p "$(dirname "$KEYSTORE_PATH")"
    "$(keytool_bin)" -genkeypair -v \
      -keystore "$KEYSTORE_PATH" \
      -alias "$KEY_ALIAS" \
      -keyalg RSA -keysize 4096 -validity 10000 \
      -storepass "$KEYSTORE_PASSWORD" \
      -keypass "$KEY_PASSWORD" \
      -dname "CN=Sovereign Ledger, OU=Mobile, O=Sovereign Ledger, L=Zurich, C=CH"
    chmod 600 "$KEYSTORE_PATH"
    echo "generated $KEYSTORE_PATH (alias: $KEY_ALIAS)"
  fi
  sync_local_properties
}

# write signing.* entries into local.properties (app/build.gradle.kts reads them)
sync_local_properties() {
  [[ -f "$LOCAL_PROPERTIES" ]] || touch "$LOCAL_PROPERTIES"
  local abs_path="$KEYSTORE_PATH"
  if [[ "$abs_path" != /* ]]; then abs_path="$ROOT/$abs_path"; fi
  local props=("signing.keystore.path=$abs_path"
               "signing.store.password=$KEYSTORE_PASSWORD"
               "signing.key.alias=$KEY_ALIAS"
               "signing.key.password=$KEY_PASSWORD")
  local tmp; tmp="$(mktemp)"
  cp "$LOCAL_PROPERTIES" "$tmp"
  for prop in "${props[@]}"; do
    local key="${prop%%=*}"
    grep -v "^${key}=" "$tmp" > "$tmp.new" || true
    mv "$tmp.new" "$tmp"
  done
  printf '%s\n' "${props[@]}" >> "$tmp"
  mv "$tmp" "$LOCAL_PROPERTIES"
  echo "local.properties updated with signing.* entries"
}

verify() {
  [[ -f "$KEYSTORE_PATH" ]] || { echo "no keystore at $KEYSTORE_PATH — run: script/keystore.sh"; exit 1; }
  "$(keytool_bin)" -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null \
    | sed -n '1,12p'
}

secrets() {
  [[ -f "$KEYSTORE_PATH" ]] || { echo "no keystore at $KEYSTORE_PATH — run: script/keystore.sh"; exit 1; }
  echo "Paste these into GitHub → Settings → Secrets → Actions:"
  echo
  echo "ANDROID_KEYSTORE_BASE64:"
  base64 -i "$KEYSTORE_PATH"
  echo
  echo "ANDROID_KEYSTORE_PASSWORD: $KEYSTORE_PASSWORD"
  echo "ANDROID_KEY_ALIAS:         $KEY_ALIAS"
  echo "ANDROID_KEY_PASSWORD:      $KEY_PASSWORD"
  echo "PLAY_SERVICE_ACCOUNT_JSON: <contents of the Play service-account JSON>"
}

case "${1:-generate}" in
  generate) generate ;;
  verify)   verify ;;
  secrets)  secrets ;;
  *) echo "usage: script/keystore.sh [generate|verify|secrets]"; exit 1 ;;
esac
