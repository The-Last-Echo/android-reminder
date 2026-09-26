#!/usr/bin/env python3
"""Fail if a packaged Offline manifest declares Android network permissions."""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
FORBIDDEN = {
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
}


def check_manifest(path: Path) -> list[str]:
    if not path.is_file():
        raise FileNotFoundError(f"Offline merged manifest not found: {path}")

    root = ET.parse(path).getroot()
    declared = {
        element.get(ANDROID_NS + "name")
        for element in root
        if element.tag in {"uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m"}
    }
    present = sorted(FORBIDDEN & declared)
    if present:
        return [f"{path}: forbidden network permission(s): {', '.join(present)}"]
    print(f"OK: no network permissions in {path}")
    return []


def main() -> int:
    if len(sys.argv) < 2:
        print(f"Usage: {Path(sys.argv[0]).name} <offline-merged-manifest> [...]", file=sys.stderr)
        return 2

    errors = []
    for argument in sys.argv[1:]:
        try:
            errors.extend(check_manifest(Path(argument)))
        except (OSError, ET.ParseError) as error:
            errors.append(str(error))

    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
