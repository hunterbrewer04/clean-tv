#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG=com.hunterbrewer.livingroomlauncher
"$ADB" connect "$TV" >/dev/null
"$ADB" -s "$TV" shell pm enable --user 0 "$PKG" >/dev/null
"$ADB" -s "$TV" shell cmd package set-home-activity --user 0 "$PKG/.MainActivity"
"$ADB" -s "$TV" shell settings put secure screensaver_components "$PKG/.ClockDreamService"
"$ADB" -s "$TV" shell settings put secure screensaver_default_component "$PKG/.ClockDreamService"
"$ADB" -s "$TV" shell input keyevent KEYCODE_HOME
