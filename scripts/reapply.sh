#!/usr/bin/env bash
# Run after a TV/app update if the stock home or Google screensaver comes back.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG=com.hunterbrewer.livingroomlauncher
LEDGER="$ROOT/state/disabled-by-us.txt"
a() { "$ADB" -s "$TV" "$@"; }
"$ADB" connect "$TV" >/dev/null
a shell pm enable --user 0 "$PKG" >/dev/null
a shell cmd package set-home-activity --user 0 "$PKG/.MainActivity"
a shell settings put secure screensaver_components "$PKG/.ClockDreamService"
a shell settings put secure screensaver_default_component "$PKG/.ClockDreamService"
if [[ -f "$LEDGER" ]]; then
  while read -r p; do
    [[ -n "$p" ]] || continue
    a shell pm disable-user --user 0 "$p" >/dev/null 2>&1 && sleep 15
  done < "$LEDGER"
fi
a shell cmd package set-home-activity --user 0 "$PKG/.MainActivity" >/dev/null
a shell input keyevent KEYCODE_HOME
printf 'Resolved Home: '
a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
