#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG="com.hunterbrewer.livingroomlauncher"
[[ "${APPLY:-}" == "1" ]] || { echo 'Dry run. Re-run with APPLY=1 only after the alongside-stock test checklist passes.'; exit 0; }
"$ADB" connect "$TV" >/dev/null
"$ADB" -s "$TV" shell pm path "$PKG" >/dev/null || { echo 'Launcher is not installed.' >&2; exit 1; }
# This phase changes only the preferred Home and dream. It does not disable stock packages.
"$ADB" -s "$TV" shell cmd package set-home-activity --user 0 "$PKG/.MainActivity"
"$ADB" -s "$TV" shell settings put secure screensaver_components "$PKG/.ClockDreamService"
"$ADB" -s "$TV" shell settings put secure screensaver_default_component "$PKG/.ClockDreamService"
sleep 2
"$ADB" -s "$TV" shell input keyevent KEYCODE_HOME
printf 'Resolved Home: '
"$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
