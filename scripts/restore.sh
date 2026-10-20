#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
"$ADB" connect "$TV" >/dev/null
# Only the packages phase2-debloat.sh can disable. setupwraith is never touched by our scripts.
for pkg in com.google.android.apps.tv.launcherx com.google.android.apps.tv.dreamx com.google.android.katniss com.hisense.alexa; do
  "$ADB" -s "$TV" shell pm enable --user 0 "$pkg" >/dev/null 2>&1 || true
done
"$ADB" -s "$TV" shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
"$ADB" -s "$TV" shell settings put secure screensaver_default_component com.google.android.apps.tv.dreamx/.service.Backdrop
"$ADB" -s "$TV" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
sleep 2
"$ADB" -s "$TV" shell input keyevent KEYCODE_HOME
printf 'Resolved Home: '
"$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
