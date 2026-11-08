"""Exercise the workflow's actual cache shell scripts against a local fake S3.

Run: python3 .github/scripts/test_ios_build_cache.py
No AWS credentials, network, third-party packages, or Apple build tools required.
"""

import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tarfile
import tempfile
import textwrap
import unittest


WORKFLOW = Path(os.environ.get(
    "IOS_CACHE_WORKFLOW",
    Path(__file__).resolve().parents[1] / "workflows/ios-testflight.yml",
))
BUCKET = "test-cache"
KONAN_HASH = "a" * 64
OLD_HASH = "b" * 64
FW_HASH = "c" * 64
KONAN_KEY = f"konan2/macOS/{KONAN_HASH}"
OLD_KEY = f"konan2/macOS/{OLD_HASH}"
FW_KEY = f"fw3/macOS/{FW_HASH}"


def fake_aws(args):
    """Model AWS's PRE child-prefix output and Garage's missing-prefix exit 1."""
    with open(os.environ["FAKE_AWS_LOG"], "a") as log:
        log.write(json.dumps(args) + "\n")
    if args[:2] == ["configure", "set"]:
        return 0
    if os.environ.get("FAKE_AWS_UNAVAILABLE") == "1":
        return 1
    root = Path(os.environ["FAKE_AWS_ROOT"])

    def local(uri):
        return root / uri.removeprefix("s3://")

    if args[:2] == ["s3", "ls"]:
        path = local(args[2])
        if not path.exists():
            return 1
        if path.is_file():
            print(f"2026-11-08 10:00:00 {path.stat().st_size} {path.name}")
        else:
            for child in sorted(path.iterdir()):
                if child.is_dir():
                    print(f"                           PRE {child.name}/")
                else:
                    print(f"2026-11-08 10:00:00 {child.stat().st_size} {child.name}")
        return 0
    if args[:2] == ["s3", "cp"]:
        source, destination = args[2:4]
        if source.startswith("s3://"):
            path = local(source)
            if not path.is_file() or os.environ.get("FAKE_AWS_DOWNLOAD_FAIL") == "1":
                print(f"404: object not found: {source}", file=sys.stderr)
                return 1
            shutil.copyfile(path, destination)
        else:
            path = local(destination)
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, path)
        return 0
    raise AssertionError(f"Unexpected AWS call: {args}")


class IosBuildCacheTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.home = self.root / "home"
        self.work = self.root / "work"
        self.runner_temp = self.root / "runner-temp"
        self.bin = self.root / "bin"
        for directory in (self.home, self.work, self.runner_temp, self.bin):
            directory.mkdir()
        self.output = self.root / "outputs"
        self.output.touch()
        self.log = self.root / "aws.jsonl"
        self.store = self.root / "s3"
        self.env = dict(os.environ,
            HOME=str(self.home), RUNNER_TEMP=str(self.runner_temp), RUNNER_OS="macOS",
            S3_CACHE_BUCKET=BUCKET, GITHUB_OUTPUT=str(self.output), CONFIGURATION="Release",
            KONAN_KEY=KONAN_KEY, FW_KEY=FW_KEY, S3_CACHE_ENABLED="true",
            FAKE_AWS_ROOT=str(self.store), FAKE_AWS_LOG=str(self.log),
            PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}",
        )
        aws = self.bin / "aws"
        aws.write_text(
            f"#!/bin/sh\nexec {shlex.quote(sys.executable)} "
            f"{shlex.quote(str(Path(__file__).resolve()))} --fake-aws \"$@\"\n"
        )
        aws.chmod(0o755)

    def step_block(self, name):
        workflow = WORKFLOW.read_text()
        block = workflow.split(f"      - name: {name}\n", 1)[1]
        return block.split("\n      - name:", 1)[0]

    def run_step(self, name, restored=None):
        restored = restored or {}
        block = self.step_block(name)
        script = textwrap.dedent(block.split("        run: |\n", 1)[1])

        def expression(match):
            expression = match[1].strip()
            if expression.startswith("hashFiles("):
                return KONAN_HASH if expression == "hashFiles('gradle/libs.versions.toml')" else FW_HASH
            if expression == "runner.os":
                return "macOS"
            prefix = "steps.s3cache.outputs."
            if expression.startswith(prefix):
                return restored.get(expression.removeprefix(prefix), "")
            self.fail(f"Unexpected workflow expression: {expression}")

        script = re.sub(r"\$\{\{(.*?)\}\}", expression, script)
        path = self.root / "step.sh"
        path.write_text(script)
        result = subprocess.run(
            ["bash", "-e", "-o", "pipefail", str(path)], cwd=self.work,
            env=self.env, text=True, capture_output=True, timeout=20,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return result

    def restore(self):
        return self.run_step("Restore build caches (S3/Garage)")

    def save(self, restored):
        return self.run_step("Save build caches (S3/Garage)", restored)

    def outputs(self):
        return dict(line.split("=", 1) for line in self.output.read_text().splitlines())

    def copies(self):
        return [args for args in map(json.loads, self.log.read_text().splitlines())
                if args[:2] == ["s3", "cp"]]

    def archive(self, key, filename, files):
        tree = self.root / "archive-input"
        tree.mkdir(exist_ok=True)
        target = self.store / BUCKET / key / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        with tarfile.open(target, "w:gz") as archive:
            for name, content in files.items():
                source = tree / name
                source.parent.mkdir(parents=True, exist_ok=True)
                source.write_text(content)
                archive.add(source, arcname=name)
        return target

    def toolchain(self, key=KONAN_KEY):
        return self.archive(key, "konan.tgz", {".konan/toolchain.txt": key})

    def framework(self, key=FW_KEY):
        return self.archive(key, "fw.tgz", {
            "shared/build/xcode-frameworks/Release/iphoneos/shared.framework/binary": key,
            "shared/build/generated/compose/resourceGenerator/resources.txt": "fonts",
        })

    def test_exact_hits_restore_both_archives(self):
        self.toolchain()
        self.framework()
        result = self.restore()
        self.assertEqual(self.outputs()["konan"], "hit")
        self.assertEqual(self.outputs()["konan_match"], "exact")
        self.assertEqual(self.outputs()["fw"], "hit")
        self.assertEqual((self.home / ".konan/toolchain.txt").read_text(), KONAN_KEY)
        self.assertIn("restore summary: konan=hit fw=hit", result.stdout)
        self.assertNotIn("falling back", result.stdout)
        self.assertEqual(self.copies()[0][2], f"s3://{BUCKET}/{KONAN_KEY}/konan.tgz")

    def test_fallback_restores_namespaced_toolchain(self):
        self.toolchain(OLD_KEY)
        result = self.restore()
        self.assertEqual(self.outputs().get("konan"), "hit", result.stdout + result.stderr)
        self.assertEqual(self.outputs()["konan_match"], "fallback")
        self.assertEqual((self.home / ".konan/toolchain.txt").read_text(), OLD_KEY)
        self.assertEqual(self.copies()[0][2], f"s3://{BUCKET}/{OLD_KEY}/konan.tgz")
        self.assertNotIn("fw", self.outputs())
        self.assertIn(f"falling back to {OLD_KEY}", result.stdout)

    def test_fallback_is_saved_under_current_key_for_the_next_run(self):
        self.toolchain(OLD_KEY)
        self.restore()
        self.save(self.outputs())
        self.assertTrue((self.store / BUCKET / KONAN_KEY / "konan.tgz").is_file())
        self.output.write_text("")
        self.restore()
        self.assertEqual(self.outputs()["konan_match"], "exact")

    def test_exact_restored_archives_are_not_uploaded_again(self):
        self.toolchain()
        self.framework()
        self.restore()
        copies_before = self.copies()
        self.save(self.outputs())
        self.assertEqual(self.copies(), copies_before)

    def test_framework_hit_does_not_prevent_fallback_toolchain_save(self):
        self.toolchain(OLD_KEY)
        self.framework()
        self.restore()
        before = len(self.copies())
        self.save(self.outputs())
        self.assertEqual(len(self.copies()), before + 1)
        self.assertEqual(self.copies()[-1][3], f"s3://{BUCKET}/{KONAN_KEY}/konan.tgz")
        condition = self.step_block("Save build caches (S3/Garage)").split("\n", 1)[0]
        self.assertIn("|| steps.s3cache.outputs.konan_match != 'exact'", condition)

    def test_empty_bucket_is_a_nonfatal_cold_miss(self):
        result = self.restore()
        self.assertNotIn("konan", self.outputs())
        self.assertNotIn("fw", self.outputs())
        self.assertEqual(self.copies(), [])
        self.assertIn("restore summary: konan=miss fw=miss", result.stdout)

    def test_loose_objects_are_not_treated_as_fallback_prefixes(self):
        loose = self.store / BUCKET / "konan2/macOS/not-a-prefix.txt"
        loose.parent.mkdir(parents=True)
        loose.write_text("ignore me")
        self.restore()
        self.assertEqual(self.copies(), [])
        self.assertNotIn("konan", self.outputs())

    def test_failed_download_never_reports_a_hit(self):
        self.toolchain()
        self.env["FAKE_AWS_DOWNLOAD_FAIL"] = "1"
        result = self.restore()
        self.assertNotIn("konan", self.outputs())
        self.assertNotIn("konan_match", self.outputs())
        self.assertIn("konan restore failed", result.stdout)

    def test_corrupt_toolchain_never_reports_a_hit(self):
        self.toolchain().write_bytes(b"broken tarball")
        self.restore()
        self.assertNotIn("konan", self.outputs())
        self.assertNotIn("konan_match", self.outputs())
        self.assertFalse((self.runner_temp / "konan.tgz").exists())

    def test_framework_never_falls_back_to_a_different_hash(self):
        self.framework("fw3/macOS/" + OLD_HASH)
        self.restore()
        self.assertNotIn("fw", self.outputs())
        self.assertEqual(self.copies(), [])

    def test_corrupt_framework_never_reports_a_hit(self):
        self.framework().write_bytes(b"broken tarball")
        self.restore()
        self.assertNotIn("fw", self.outputs())

    def test_unavailable_s3_is_nonfatal(self):
        self.toolchain()
        self.framework()
        self.env["FAKE_AWS_UNAVAILABLE"] = "1"
        result = self.restore()
        self.assertNotIn("konan", self.outputs())
        self.assertNotIn("fw", self.outputs())
        self.assertIn("restore summary: konan=miss fw=miss", result.stdout)


if __name__ == "__main__":
    if sys.argv[1:2] == ["--fake-aws"]:
        sys.exit(fake_aws(sys.argv[2:]))
    unittest.main()
