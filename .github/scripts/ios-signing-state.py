#!/usr/bin/env python3
"""Install and remove only a CI run's local macOS signing resources."""
import hashlib
import json
import os
from pathlib import Path
import plistlib
import shlex
import subprocess
import sys


def keychains():
    result = subprocess.run(
        ['security', 'list-keychains', '-d', 'user'],
        check=True, capture_output=True, text=True,
    )
    return shlex.split(result.stdout)


def set_keychains(paths):
    subprocess.run(['security', 'list-keychains', '-d', 'user', '-s', *paths], check=True)


def add_keychain(root):
    ci_keychain = root / 'ci.keychain-db'
    # security create-keychain may already have added it to the search list.
    original = [p for p in keychains() if Path(p).resolve() != ci_keychain.resolve()]
    (root / 'keychains.json').write_text(json.dumps(original))
    set_keychains([str(root / 'ci.keychain-db'), *original])


def install_profiles(root, profile):
    content = profile.read_bytes()
    decoded = subprocess.run(
        ['security', 'cms', '-D', '-i', str(profile)],
        check=True, capture_output=True,
    ).stdout
    metadata = plistlib.loads(decoded)
    if os.environ['TEAM_ID'] not in metadata.get('TeamIdentifier', []):
        raise ValueError('Profile team does not match TEAM_ID')
    app_id = metadata.get('Entitlements', {}).get('application-identifier', '')
    expected = os.environ.get('EXPECTED_BUNDLE_ID', os.environ['BUNDLE_ID'])
    if app_id.split('.', 1)[-1] != expected:
        raise ValueError(f'Profile bundle ID {app_id} does not match {expected}')
    uuid = metadata['UUID']
    if not uuid or any(c not in '0123456789abcdefABCDEF-' for c in uuid):
        raise ValueError('Invalid provisioning profile UUID')
    home = Path.home()
    # ONLY the legacy directory: xcodebuild scans it, but the Xcode GUI
    # ignores it (16+ reads UserData instead) — the runner stays invisible
    # to the developer's own Xcode.
    destination = home / 'Library/MobileDevice/Provisioning Profiles' / f'{uuid}.mobileprovision'
    destination.parent.mkdir(parents=True, exist_ok=True)
    manifest_path = root / 'installed-profiles.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else []
    if destination.exists():
        # Same UUID = same profile; never overwrite, never fail.
        print(f'::warning::Profile {uuid} already installed — leaving it untouched')
    else:
        # Exclusive creation leaves any existing local profile untouched.
        with destination.open('xb') as target:
            manifest.append({'path': str(destination), 'sha256': hashlib.sha256(content).hexdigest()})
            target.write(content)
    manifest_path.write_text(json.dumps(manifest))
    print(f'Installed CI profile {uuid} for {expected} in the legacy profiles directory')


def cleanup(root):
    errors = []
    manifest = root / 'installed-profiles.json'
    if manifest.exists():
        for item in json.loads(manifest.read_text()):
            path = Path(item['path'])
            if path.exists():
                if hashlib.sha256(path.read_bytes()).hexdigest() == item['sha256']:
                    path.unlink()
                else:
                    print(f'::warning::Leaving modified profile untouched: {path}')
    ci_keychain = root / 'ci.keychain-db'
    snapshot = root / 'keychains.json'
    if snapshot.exists():
        try:
            # Preserve keychains added by the user during the job, too.
            current = [p for p in keychains() if Path(p).resolve() != ci_keychain.resolve()]
            original = [p for p in json.loads(snapshot.read_text()) if Path(p).resolve() != ci_keychain.resolve()]
            set_keychains(list(dict.fromkeys([*original, *current])))
        except subprocess.CalledProcessError as error:
            errors.append(error)
    if ci_keychain.exists() and not errors:
        try:
            subprocess.run(['security', 'delete-keychain', str(ci_keychain)], check=True)
        except subprocess.CalledProcessError as error:
            errors.append(error)
    if errors:
        raise RuntimeError('Could not clean up the CI keychain') from errors[0]


if __name__ == '__main__':
    signing_root = Path(os.environ['CI_SIGNING_DIR'])
    command = sys.argv[1]
    if command == 'keychain-add':
        add_keychain(signing_root)
    elif command == 'profiles-install':
        install_profiles(signing_root, Path(sys.argv[2]))
    elif command == 'cleanup':
        cleanup(signing_root)
    else:
        raise SystemExit(f'Unknown command: {command}')
