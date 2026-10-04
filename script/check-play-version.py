"""Reject Play releases unless their version code increases by 1–3,
and unless the target track already exists in Play Console."""

import json
import os

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession


def validate_version(current, previous):
    if not 1 <= current <= 2_100_000_000:
        raise ValueError("versionCode must be between 1 and 2100000000")
    # An app with no existing releases starts at 1, 2, or 3.
    if not 1 <= current - previous <= 3:
        raise ValueError(
            f"versionCode {current} must be 1, 2, or 3 higher than "
            f"Google Play's highest versionCode {previous}"
        )


def main():
    current = int(os.environ["VERSION_CODE"])
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    session = AuthorizedSession(credentials)
    url = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.sovereignledger.app/edits"
    response = session.post(url, json={}, timeout=60)
    response.raise_for_status()
    try:
        edit_url = f"{url}/{response.json()['id']}"
        response = session.get(f"{edit_url}/tracks", timeout=60)
        response.raise_for_status()
        tracks = response.json().get("tracks", [])
        codes = [
            int(code)
            for track in tracks
            for release in track.get("releases", [])
            for code in release.get("versionCodes", [])
        ]
        previous = max(codes, default=0)
        validate_version(current, previous)
        # The upload can only target a track Play already knows — anything
        # else is a typo or a track not created in Console yet.
        play_track = os.environ.get("PLAY_TRACK", "internal")
        available = sorted(t.get("track") for t in tracks if t.get("track"))
        if play_track not in available:
            raise ValueError(
                f"track '{play_track}' does not exist in Play Console — "
                f"available tracks: {', '.join(available) or '(none)'}"
            )
        print(f"Version check passed: {previous} -> {current} on '{play_track}'")
    finally:
        # This edit is only used to read tracks; never commit it. Best-effort
        # cleanup — never let it mask the original exception.
        try:
            session.delete(edit_url, timeout=60).raise_for_status()
        except Exception as cleanup_error:  # noqa: BLE001
            print(f"warning: could not discard Play edit: {cleanup_error}")


if __name__ == "__main__":
    main()
