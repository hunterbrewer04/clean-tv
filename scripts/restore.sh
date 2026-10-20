#!/usr/bin/env bash
# Undo everything: re-enable every package our scripts disabled, restore Google's screensaver and stock HOME.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/batches.sh"
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
LEDGER="$ROOT/state/disabled-by-us.txt"
a() { "$ADB" -s "$TV" "$@"; }
"$ADB" connect "$TV" >/dev/null
# Ledger is authoritative; batch list is a fallback. Baseline (snapshots/disabled.before.txt) had zero disabled packages.
{ [[ -f "$LEDGER" ]] && cat "$LEDGER"; for b in "${BATCH_ORDER[@]}"; do batch_packages "$b"; done; } | sort -u | while read -r p; do
  [[ -n "$p" ]] || continue
  a shell pm enable --user 0 "$p" >/dev/null 2>&1 && echo "enabled $p" || true
done
a shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
a shell settings put secure screensaver_default_component com.google.android.apps.tv.dreamx/.service.Backdrop
a shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
sleep 2
a shell input keyevent KEYCODE_HOME
rm -f "$LEDGER"
printf 'Resolved Home: '
a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
