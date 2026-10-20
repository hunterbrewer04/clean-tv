#!/usr/bin/env bash
# Read-only: HOME candidates, screensaver config, remote-key routing, and status of debloat candidates.
set -uo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
a() { "$ADB" -s "$TV" "$@"; }
"$ADB" connect "$TV" >/dev/null
echo '=== HOME candidates'
a shell pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep -E '/|priority'
printf 'Resolved HOME: '; a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
echo '=== Screensaver'
for k in screensaver_enabled screensaver_components screensaver_default_component screensaver_activate_on_sleep; do printf '%s=' "$k"; a shell settings get secure "$k"; done
printf 'sleep_timeout='; a shell settings get secure sleep_timeout
printf 'screen_off_timeout='; a shell settings get system screen_off_timeout
echo '=== Already disabled'
a shell pm list packages -d
echo '=== Candidates (installed? enabled?)'
for p in "$@"; do
  path=$(a shell pm path "$p" 2>/dev/null | head -n 1 | tr -d '\r')
  if [[ -z "$path" ]]; then echo "MISSING   $p"; continue; fi
  if a shell pm list packages -d | tr -d '\r' | grep -qx "package:$p"; then st=disabled; else st=enabled; fi
  sys=user; [[ "$path" == *"/data/app/"* ]] || sys=system
  echo "$st  $sys  $p"
done
