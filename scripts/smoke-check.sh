#!/usr/bin/env bash
# Read-only post-install check: version, foreground activity, crashes, memory, Home.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
set +e
printf 'Version:    '; sh_ dumpsys package "$PKG" | grep -m1 versionName | tr -d ' '
printf 'Foreground: '; sh_ dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity' | sed 's/^ *//'
echo '--- crashes'
a logcat -d -b crash | grep -A8 "$PKG" | head -n 20
echo '--- memory / start time'
sh_ dumpsys meminfo "$PKG" | grep -m1 'TOTAL PSS'
a logcat -d | grep "Displayed $PKG" | tail -n 1
echo "Home:        $(home_resolved)"
by_us=0; [[ -f "$LEDGER" ]] && by_us="$(wc -l < "$LEDGER")"
echo "Disabled:    $(sh_ pm list packages -d | wc -l) packages ($by_us by these scripts)"
