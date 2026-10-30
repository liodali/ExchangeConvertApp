#!/usr/bin/env python3
"""Split Play Console multi-language release notes into per-language files.

Source text uses the Google Play Console paste format:

    <en-US>
    English release notes …
    </en-US>
    <de-DE>
    Deutsche Hinweise …
    </de-DE>

Locally, keep the notes in the git-ignored ``whatsnew.xml`` at the repo root
and make the file the tag message — nothing is committed per release:

    git tag -a app-vX.Y.Z+N -F whatsnew.xml
    git push origin app-vX.Y.Z+N

CI exports the annotated tag's message and runs this script the same way.
Output is one file per language in ``--out``, each capped at Play's 500-byte
What's-new limit (truncated on a UTF-8 boundary, trailing whitespace
stripped). Only include languages configured in the Play Console listing —
the upload rejects files for unlisted languages.

Fallbacks:
- No ``<en-US>`` block: the plain text outside any <lang> blocks becomes
  en-US (legacy single-language tag messages keep working).
- No text at all (manual workflow runs): en-US = "Internal testing build."

``--github-body PATH`` additionally writes the full, untruncated en-US text
for the GitHub release body.

Usage:
    python3 script/whatsnew.py whatsnew.xml --out whatsnew \
        --github-body github-release-notes.md
"""

import argparse
import re
import sys
from pathlib import Path

PLAY_LIMIT_BYTES = 500
FALLBACK = "Internal testing build."

# Play listing language codes: en-US, de-DE, es-419, pt-BR, ar, …
BLOCK_RE = re.compile(r"<([a-z]{2}(?:-[A-Za-z0-9]+)?)>(.*?)</\1>", re.DOTALL)


def play_clip(text: str) -> str:
    """Cap at Play's 500 bytes without splitting a multibyte glyph."""
    return text.encode("utf-8")[:PLAY_LIMIT_BYTES].decode("utf-8", "ignore").rstrip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("source", type=Path, help="Combined notes (whatsnew.xml or exported tag message)")
    parser.add_argument("--out", type=Path, required=True, help="Output directory for per-language files")
    parser.add_argument("--github-body", type=Path, help="Also write the full en-US text here (GitHub release body)")
    args = parser.parse_args()

    text = args.source.read_text(encoding="utf-8") if args.source.exists() else ""
    blocks = {code: body.strip() for code, body in BLOCK_RE.findall(text)}
    # Text outside any <lang> block — the legacy plain-tag-message path.
    plain = BLOCK_RE.sub("", text).strip()

    en_us = blocks.get("en-US") or plain
    if not en_us:
        en_us = FALLBACK
    blocks["en-US"] = en_us

    args.out.mkdir(parents=True, exist_ok=True)
    for code, body in sorted(blocks.items()):
        size = len(body.encode("utf-8"))
        if size > PLAY_LIMIT_BYTES:
            print(f"::warning::{code} is {size} bytes — over Play's {PLAY_LIMIT_BYTES}-byte cap, truncated")
        clipped = play_clip(body) or FALLBACK
        (args.out / code).write_text(clipped, encoding="utf-8")
        print(f"{code}: {len(clipped.encode('utf-8'))} bytes")

    if args.github_body:
        args.github_body.write_text(en_us + "\n", encoding="utf-8")

    print(f"languages written: {', '.join(sorted(blocks))}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
