#!/bin/sh
# After a successful archive: save the built Kotlin framework (and, on the
# run that downloaded it, the Kotlin/Native toolchain) to the S3 cache so
# the NEXT Xcode Cloud build skips the Gradle build entirely.
#
# Best-effort by design — a failed save must never fail the release.
set -euo pipefail

if [[ "${CI_XCODE_CLOUD:-}" != "TRUE" ]]; then
  exit 0
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"

cd "$REPO_ROOT"
(. "$SCRIPT_DIR/ci_cache.sh"; xc_cache_save) \
  || echo "xc-cache: save failed — the next build just runs cold"
