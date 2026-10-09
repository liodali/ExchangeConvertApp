#!/usr/bin/env python3
"""Read the shared app version and validate optional release tag metadata."""
import argparse
import json
from pathlib import Path
import re

DEFAULT_FILE = Path(__file__).resolve().parents[1] / 'version.xcconfig'


def read_version(path=DEFAULT_FILE):
    values = {}
    for line in Path(path).read_text().splitlines():
        line = line.split('//', 1)[0].strip()
        if not line:
            continue
        key, separator, value = line.partition('=')
        key, value = key.strip(), value.strip()
        if not separator or key not in {'MARKETING_VERSION', 'CURRENT_PROJECT_VERSION'}:
            raise ValueError(f'Unexpected version setting: {line}')
        if key in values:
            raise ValueError(f'Duplicate version setting: {key}')
        values[key] = value
    version = values.get('MARKETING_VERSION', '')
    build = values.get('CURRENT_PROJECT_VERSION', '')
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', version):
        raise ValueError('MARKETING_VERSION must be X.Y.Z')
    if not re.fullmatch(r'[1-9][0-9]*', build) or int(build) > 2100000000:
        raise ValueError('CURRENT_PROJECT_VERSION must be an integer from 1 to 2100000000')
    return version, int(build)


def validate_tag(tag, version, build):
    match = re.fullmatch(r'(?:app|ios)-v([0-9]+\.[0-9]+\.[0-9]+)(?:-dev)?(?:\+([1-9][0-9]*)(?:-dev)?)?', tag)
    if not match:
        raise ValueError(f'Invalid app release tag: {tag}')
    if match[1] != version or (match[2] is not None and int(match[2]) != build):
        raise ValueError(f'Tag {tag} does not match version.xcconfig ({version}+{build})')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--file', type=Path, default=DEFAULT_FILE)
    parser.add_argument('--tag', help='Release tag to check against the shared version')
    parser.add_argument('--format', choices=['json', 'android-env', 'ios-output'], default='json')
    args = parser.parse_args()
    try:
        version, build = read_version(args.file)
        if args.tag:
            validate_tag(args.tag, version, build)
    except (OSError, ValueError) as error:
        parser.exit(1, f'App version error: {error}\n')
    if args.format == 'android-env':
        print(f'VERSION_NAME={version}\nVERSION_CODE={build}')
    elif args.format == 'ios-output':
        print(f'version={version}\nbuild_number={build}')
    else:
        print(json.dumps({'version': version, 'build_number': build}))


if __name__ == '__main__':
    main()
