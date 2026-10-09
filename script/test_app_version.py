import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('app_version', Path(__file__).with_name('app-version.py'))
app_version = importlib.util.module_from_spec(spec)
spec.loader.exec_module(app_version)


class AppVersionTests(unittest.TestCase):
    def read(self, content):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'version.xcconfig'
            path.write_text(content)
            return app_version.read_version(path)

    def test_comments_and_whitespace(self):
        self.assertEqual(self.read('// Common version\nMARKETING_VERSION = 0.7.2 // version\nCURRENT_PROJECT_VERSION = 53\n'), ('0.7.2', 53))

    def test_invalid_versions(self):
        for version in ['0.7', '0.7.2-dev', 'abc', '0.7.2.1']:
            with self.subTest(version=version), self.assertRaises(ValueError):
                self.read(f'MARKETING_VERSION = {version}\nCURRENT_PROJECT_VERSION = 53\n')

    def test_invalid_build_numbers(self):
        for build in ['0', '-1', '1.2', '2100000001', '053']:
            with self.subTest(build=build), self.assertRaises(ValueError):
                self.read(f'MARKETING_VERSION = 0.7.2\nCURRENT_PROJECT_VERSION = {build}\n')

    def test_build_number_limit(self):
        self.assertEqual(self.read('MARKETING_VERSION = 1.0.0\nCURRENT_PROJECT_VERSION = 2100000000\n'), ('1.0.0', 2100000000))

    def test_duplicate_or_missing_settings(self):
        for content in ['MARKETING_VERSION = 0.7.2\n', 'MARKETING_VERSION = 0.7.2\nCURRENT_PROJECT_VERSION = 53\nCURRENT_PROJECT_VERSION = 54\n']:
            with self.subTest(content=content), self.assertRaises(ValueError):
                self.read(content)

    def test_matching_release_tags(self):
        for tag in ['app-v0.7.2+53', 'ios-v0.7.2+53', 'app-v0.7.2', 'ios-v0.7.2-dev+53', 'app-v0.7.2+53-dev']:
            with self.subTest(tag=tag):
                app_version.validate_tag(tag, '0.7.2', 53)

    def test_mismatched_or_malformed_release_tags(self):
        for tag in ['app-v0.7.3+53', 'ios-v0.7.2+54', 'app-v0.7.2+abc', 'ios-v0.7.2+0', 'v0.7.2+53']:
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                app_version.validate_tag(tag, '0.7.2', 53)


if __name__ == '__main__':
    unittest.main()
