import importlib.util
import json
import os
from pathlib import Path
import plistlib
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('signing', Path(__file__).with_name('ios-signing-state.py'))
signing = importlib.util.module_from_spec(spec)
spec.loader.exec_module(signing)

class SigningStateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name) / 'home'
        self.root = Path(self.temp.name) / 'ci'
        self.root.mkdir()
        self.env = patch.dict(os.environ, HOME=str(self.home), TEAM_ID='TEAM', BUNDLE_ID='com.example.app')
        self.env.start(); self.addCleanup(self.env.stop)
        self.uuid = 'ABCD-1234'
        self.metadata = {'UUID': self.uuid, 'TeamIdentifier':['TEAM'], 'Entitlements':{'application-identifier':'TEAM.com.example.app'}}
        (self.root/'profile.plist').write_bytes(plistlib.dumps(self.metadata))
        self.profile = self.root/'AppStore.mobileprovision'
        self.profile.write_bytes(b'CI profile content')
        self.dirs = [self.home/'Library/MobileDevice/Provisioning Profiles']

    def test_only_ci_profiles_removed(self):
        for folder in self.dirs:
            folder.mkdir(parents=True)
            (folder/'personal.mobileprovision').write_bytes(b'personal')
        signing.install_profiles(self.root, self.profile)
        signing.cleanup(self.root)
        for folder in self.dirs:
            self.assertEqual((folder/'personal.mobileprovision').read_bytes(), b'personal')
            self.assertFalse((folder/f'{self.uuid}.mobileprovision').exists())
        signing.cleanup(self.root)  # idempotent

    def test_existing_uuid_never_overwritten_or_deleted_on_partial_failure(self):
        folder = self.dirs[0]; folder.mkdir(parents=True)
        existing = folder/f'{self.uuid}.mobileprovision'; existing.write_bytes(b'existing')
        # A pre-existing profile with the same UUID is the same profile:
        # keep it untouched and carry on instead of failing the build.
        signing.install_profiles(self.root, self.profile)
        signing.cleanup(self.root)
        self.assertEqual(existing.read_bytes(), b'existing')

    def test_modified_profile_is_preserved(self):
        signing.install_profiles(self.root, self.profile)
        changed = self.dirs[0]/f'{self.uuid}.mobileprovision'
        changed.write_bytes(b'user replacement')
        signing.cleanup(self.root)
        self.assertEqual(changed.read_bytes(), b'user replacement')

    def test_wrong_team_rejected_before_install(self):
        self.metadata['TeamIdentifier']=['OTHER']
        (self.root/'profile.plist').write_bytes(plistlib.dumps(self.metadata))
        with self.assertRaises(ValueError): signing.install_profiles(self.root, self.profile)
        self.assertFalse((self.root/'installed-profiles.json').exists())

    def test_keychains_with_spaces_and_user_additions_preserved(self):
        original = ['/Users/me/Library/Keychains/login.keychain-db','/Users/me/My Keychain.keychain-db']
        ci = str(self.root/'ci.keychain-db')
        (self.root/'ci.keychain-db').touch()
        with patch.object(signing, 'keychains', return_value=[ci,*original]), patch.object(signing, 'set_keychains') as setter:
            signing.add_keychain(self.root)
            setter.assert_called_once_with([ci,*original])
        addition = '/Users/me/Added While Building.keychain-db'
        with patch.object(signing, 'keychains', return_value=[ci,*original,addition]), patch.object(signing,'set_keychains') as setter, patch.object(signing.subprocess,'run') as run:
            signing.cleanup(self.root)
            setter.assert_called_once_with([*original,addition])
            run.assert_called_once_with(['security','delete-keychain',ci],check=True)

unittest.main()
