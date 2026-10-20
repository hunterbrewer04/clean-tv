#!/usr/bin/env bash
# Read-only health check run after every debloat batch. Exit 1 if anything critical is missing.
set -uo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG=com.hunterbrewer.livingroomlauncher
a() { "$ADB" -s "$TV" "$@"; }
fail=0
ok() { printf '  ok    %s\n' "$1"; }
bad() { printf '  FAIL  %s\n' "$1"; fail=1; }
"$ADB" connect "$TV" >/dev/null 2>&1
a get-state >/dev/null 2>&1 && ok "adb reachable" || { bad "adb reachable"; exit 1; }

home=$(a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1 | tr -d '\r')
[[ "$home" == "$PKG/.MainActivity" ]] && ok "HOME = ours" || bad "HOME = $home"

a shell dumpsys wifi | grep -q 'mNetworkInfo.*CONNECTED\|curState=L2ConnectedState\|curState=ConnectedState' && ok "Wi-Fi connected" \
  || { a shell ip -4 addr show wlan0 | grep -q 'inet ' && ok "Wi-Fi has IP" || bad "Wi-Fi"; }

procs=$(a shell ps -A -o NAME | tr -d '\r')
for p in com.google.android.apps.mediashell com.google.android.tv.remote.service com.mediatek.AirplayAPK com.mediatek.tv.service; do
  if grep -q "^$p" <<<"$procs"; then ok "running $p"; else
    # Some services start on demand; at least confirm the package is enabled.
    if a shell pm list packages -e "$p" | tr -d '\r' | grep -qx "package:$p"; then ok "enabled (idle) $p"; else bad "missing/disabled $p"; fi
  fi
done
for p in com.android.vending com.google.android.gms com.android.tv.settings com.hisense.tv.hitvinput com.mediatek.wwtv.tvcenter com.vt.tv.input com.mediatek.autopair com.hisense.tv.smartsense com.vt.tv.aiot com.google.android.tungsten.setupwraith; do
  if [[ "$p" == com.google.android.tungsten.setupwraith ]]; then continue; fi
  a shell pm list packages -e "$p" | tr -d '\r' | grep -qx "package:$p" && ok "enabled $p" || bad "disabled $p"
done
printf '  screensaver = '; a shell settings get secure screensaver_components
a logcat -d -b crash 2>/dev/null | grep -E 'FATAL EXCEPTION|Process: ' | tail -n 6 | sed 's/^/  crash: /'
exit $fail
