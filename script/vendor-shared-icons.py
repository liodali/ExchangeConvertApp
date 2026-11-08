#!/usr/bin/env python3
"""Vendor only the shared UI's used Material icons; no network/build dependency.

Download the pinned org.jetbrains.compose.material 1.7.3 core/extended sources
JARs from Maven Central, then pass --core-sources and --extended-sources.
Use --check to verify the vendored files still match their upstream artwork.
"""

import argparse
import hashlib
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "shared/src/commonMain/kotlin/dali/hamza/shared/ui"
DEST = UI / "icons"
UPSTREAM = "androidx.compose.material.icons"
LOCAL = "dali.hamza.shared.ui.icons"
SOURCE_HASHES = {
    "core": "b3f1be7fda4d13a6d938bb918275ddc143e9d6a2007727f284778480526f5f16",
    "extended": "4e23237644f7da35c391cc4c0263b95248605f499ab46f6d2c92d4fe48793d5a",
}


def used_icons():
    icons = set()
    for source in UI.rglob("*.kt"):
        if DEST in source.parents:
            continue
        icons.update(re.findall(r"\bIcons\.((?:AutoMirrored\.)?Outlined\.\w+)", source.read_text()))
    return sorted(icons)


def namespace(source):
    return source.replace(UPSTREAM, LOCAL)


def generate(archives):
    core = archives[0].read("commonMain/androidx/compose/material/icons/Icons.kt").decode()
    header = core[:core.index("package ")]
    # Keep upstream's non-deprecated builders verbatim, including path defaults.
    helpers = core[core.index("inline fun materialIcon(\n    name: String,\n    autoMirror:"):]
    yield DEST / "Icons.kt", header + f"""package {LOCAL}

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Only the Material icon styles used by Dinarly. See README.md for provenance. */
object Icons {{
    object Outlined
    object AutoMirrored {{
        object Outlined
    }}
}}

""" + helpers
    for icon in used_icons():
        relative = icon.replace("AutoMirrored.", "automirrored.").replace("Outlined.", "outlined.").replace(".", "/") + ".kt"
        member = "commonMain/androidx/compose/material/icons/" + relative
        archive = next((z for z in archives if member in z.namelist()), None)
        if archive is None:
            raise ValueError(f"Icon not found in pinned sources: {icon}")
        yield DEST / relative, namespace(archive.read(member).decode())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--core-sources", type=Path, required=True)
    parser.add_argument("--extended-sources", type=Path, required=True)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    archives = []
    for name, path in (("core", args.core_sources), ("extended", args.extended_sources)):
        if hashlib.sha256(path.read_bytes()).hexdigest() != SOURCE_HASHES[name]:
            parser.error(f"{path}: expected the pinned {name} 1.7.3 sources JAR")
        archives.append(zipfile.ZipFile(path))
    try:
        expected = dict(generate(archives))
        errors = []
        for path, content in expected.items():
            if args.check:
                if not path.exists() or path.read_text() != content:
                    errors.append(f"Mismatch: {path.relative_to(ROOT)}")
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content)
        stale = set(DEST.rglob("*.kt")) - expected.keys()
        errors.extend(f"Unused vendored file: {p.relative_to(ROOT)}" for p in sorted(stale))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{'Verified' if args.check else 'Vendored'} {len(expected) - 1} icons + builders")
        return 0
    finally:
        for archive in archives:
            archive.close()


if __name__ == "__main__":
    sys.exit(main())
