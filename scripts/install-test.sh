#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
APK="${APK:-$ROOT/dist/living-room-launcher-v0.3.0.apk}"
[[ -f "$APK" ]] || { echo "Missing APK: $APK" >&2; exit 1; }
"$ADB" connect "$TV" >/dev/null
"$ADB" -s "$TV" get-state >/dev/null
printf 'Current Home before install: '
"$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
"$ADB" -s "$TV" install -r "$APK"
"$ADB" -s "$TV" shell am start -n com.hunterbrewer.livingroomlauncher/.MainActivity
printf '
Installed alongside stock Home. No package was disabled and the Home default was not changed.
'
