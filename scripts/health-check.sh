#!/usr/bin/env bash
# Read-only. Run after every batch. Exits 1 if anything critical is off.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
set +e
fail=0
ok() { printf '  ok    %s\n' "$1"; }
bad() { printf '  FAIL  %s\n' "$1"; fail=1; }

ok "adb reachable ($TV, $PROFILE profile)"
home="$(home_resolved)"
[[ "$home" == "$OURS" ]] && ok "HOME = ours" || bad "HOME = $home"

if sh_ dumpsys wifi | grep -E 'mNetworkInfo.*CONNECTED|curState=(L2Connected|Connected)State' >/dev/null; then ok "Wi-Fi connected"
elif sh_ ip -4 addr show wlan0 | grep 'inet ' >/dev/null; then ok "Wi-Fi has an IP"
elif sh_ ip -4 addr show eth0 2>/dev/null | grep 'inet ' >/dev/null; then ok "Ethernet has an IP"
else bad "no network"; fi

# Nothing protected may be disabled (exact names and whole protected namespaces).
prot=0
while read -r p; do
  [[ -n "$p" ]] && is_protected "$p" && { bad "protected package disabled: $p"; prot=1; }
done <<<"$(sh_ pm list packages -d | sed 's/^package://')"
[[ $prot == 0 ]] && ok "no protected package is disabled"

for p in com.google.android.apps.mediashell com.google.android.tv.remote.service com.android.vending com.google.android.gms; do
  if ! installed "$p"; then echo "  -     $p (not on this device)"
  elif enabled "$p"; then ok "enabled $p"
  else bad "disabled $p"; fi
done
printf '  screensaver = %s\n' "$(screensaver)"
a logcat -d -b crash 2>/dev/null | grep -E 'FATAL EXCEPTION|Process: ' | tail -n 6 | sed 's/^/  crash: /'
exit $fail
