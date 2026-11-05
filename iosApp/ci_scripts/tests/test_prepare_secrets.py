"""Test configuration selection using synthetic data, never real secrets."""
import base64
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "prepare_secrets.sh"
PRODUCTION = b'enum ExchangeSecrets { static let apiHost = "production.example" }\n'
STAGING = b'enum ExchangeSecrets { static let apiHost = "staging.example" }\n'


class PrepareSecretsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.target = self.root / "iosApp/ExchangeConvertApp/Secrets.swift"
        self.target.parent.mkdir(parents=True)
        self.target.write_bytes(b"existing local configuration\n")
        self.env = {
            "PATH": os.defpath,
            "CI_XCODE_CLOUD": "TRUE",
            "CI_PRIMARY_REPOSITORY_PATH": str(self.root),
            "IOS_SECRETS_SWIFT_BASE64": base64.b64encode(PRODUCTION).decode(),
            "IOS_SECRETS_SWIFT_BASE64_STG": base64.b64encode(STAGING).decode(),
        }

    def run_script(self):
        result = subprocess.run(
            ["/bin/sh", str(SCRIPT)], env=self.env, capture_output=True, text=True
        )
        # Neither raw source nor its encoded representation should enter logs.
        output = result.stdout + result.stderr
        for source in (PRODUCTION, STAGING):
            self.assertNotIn(source.decode().strip(), output)
            self.assertNotIn(base64.b64encode(source).decode(), output)
        self.assertEqual(list(self.target.parent.glob(".Secrets.swift.*")), [])
        return result

    def test_staging_tag(self):
        self.env["CI_TAG"] = "ios-v1.0.0-stg"
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.target.read_bytes(), STAGING)
        self.assertEqual(self.target.stat().st_mode & 0o777, 0o600)

    def test_staging_tag_with_build_number(self):
        for tag in ("ios-v1.0.0-stg+69", "ios-v1.0.0+69-stg"):
            with self.subTest(tag=tag):
                self.env["CI_TAG"] = tag
                result = self.run_script()
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(self.target.read_bytes(), STAGING)

    def test_production_tag(self):
        self.env["CI_TAG"] = "ios-v1.0.0"
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.target.read_bytes(), PRODUCTION)

    def test_untagged_main_build_uses_production(self):
        self.env["CI_BRANCH"] = "main"
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.target.read_bytes(), PRODUCTION)

    def test_local_run_leaves_configuration_untouched(self):
        self.env = {"PATH": os.defpath}
        before = self.target.read_bytes()
        self.assertEqual(self.run_script().returncode, 0)
        self.assertEqual(self.target.read_bytes(), before)

    def test_missing_selected_variable_never_falls_back(self):
        for tag, selected in (
            ("ios-v1.0.0-stg", "IOS_SECRETS_SWIFT_BASE64_STG"),
            ("ios-v1.0.0", "IOS_SECRETS_SWIFT_BASE64"),
        ):
            with self.subTest(tag=tag):
                self.env["CI_TAG"] = tag
                encoded = self.env.pop(selected)
                before = self.target.read_bytes()
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(selected, result.stderr)
                self.assertEqual(self.target.read_bytes(), before)
                self.env[selected] = encoded

    def test_invalid_base64_preserves_existing_file(self):
        self.env["IOS_SECRETS_SWIFT_BASE64"] = "%%%invalid-base64%%%"
        before = self.target.read_bytes()
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("not valid Base64", result.stderr)
        self.assertEqual(self.target.read_bytes(), before)

    def test_empty_decode_preserves_existing_file(self):
        self.env["IOS_SECRETS_SWIFT_BASE64"] = "\n"
        before = self.target.read_bytes()
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("empty Secrets.swift", result.stderr)
        self.assertEqual(self.target.read_bytes(), before)

    def test_generates_file_on_fresh_clone(self):
        self.target.unlink()
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.target.read_bytes(), PRODUCTION)


if __name__ == "__main__":
    unittest.main()
