#!/bin/sh
# Generate the ignored Swift configuration before Xcode plans its build.
set -eu

if [ "${CI_XCODE_CLOUD:-}" != "TRUE" ]; then
  exit 0
fi

# One workflow handles both tag types. Untagged builds (including main)
# use production; staging tags may put -stg before or after +BUILD.
case "${CI_TAG:-}" in
  *-stg|*-stg+*)
    environment="staging"
    variable="IOS_SECRETS_SWIFT_BASE64_STG"
    encoded="${IOS_SECRETS_SWIFT_BASE64_STG:-}"
    ;;
  *)
    environment="production"
    variable="IOS_SECRETS_SWIFT_BASE64"
    encoded="${IOS_SECRETS_SWIFT_BASE64:-}"
    ;;
esac

if [ -z "$encoded" ]; then
  echo "error: Missing secret environment variable $variable for $environment" >&2
  exit 1
fi

: "${CI_PRIMARY_REPOSITORY_PATH:?Missing Xcode Cloud repository path}"
secrets_dir="$CI_PRIMARY_REPOSITORY_PATH/iosApp/ExchangeConvertApp"

# Decode atomically so failures never replace an existing file. Never log
# the encoded or decoded contents: masking the variable does not mask Swift.
umask 077
temporary_file="$(mktemp "$secrets_dir/.Secrets.swift.XXXXXX")"
trap 'rm -f "$temporary_file"' EXIT
trap 'exit 1' HUP INT TERM

if ! printf '%s' "$encoded" | /usr/bin/base64 --decode > "$temporary_file"; then
  echo "error: $variable is not valid Base64" >&2
  exit 1
fi

if [ ! -s "$temporary_file" ]; then
  echo "error: $variable decoded to an empty Secrets.swift" >&2
  exit 1
fi

mv -f "$temporary_file" "$secrets_dir/Secrets.swift"
echo "Secrets.swift prepared for $environment."
