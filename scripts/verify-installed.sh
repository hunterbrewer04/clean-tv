#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG="com.hunterbrewer.livingroomlauncher"
"$ADB" connect "$TV" >/dev/null
"$ADB" -s "$TV" shell dumpsys package "$PKG" | grep -E 'versionName=|versionCode=|MainActivity|ClockDreamService' | head -n 12
printf 'Resolved Home: '
"$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
printf 'Disabled packages: '
"$ADB" -s "$TV" shell pm list packages -d | wc -l
