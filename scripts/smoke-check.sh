#!/usr/bin/env bash
# Post-install smoke check: version, foreground activity, crashes, memory, current Home.
set -uo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG=com.hunterbrewer.livingroomlauncher
a() { "$ADB" -s "$TV" "$@"; }
printf 'Version: '; a shell dumpsys package "$PKG" | grep -m1 versionName | tr -d ' '
printf 'Foreground: '; a shell dumpsys activity activities | grep -m1 mResumedActivity | sed 's/^ *//'
echo '--- crashes'
a logcat -d -b crash | grep -A8 "$PKG" | head -n 20
a logcat -d | grep -E 'FATAL|AndroidRuntime' | grep -i -B1 -A6 "$PKG" | head -n 20
echo '--- memory / start time'
a shell dumpsys meminfo "$PKG" | grep -m1 'TOTAL PSS'
a logcat -d | grep "Displayed $PKG" | tail -n 1
printf 'Home: '; a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
