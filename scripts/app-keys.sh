#!/usr/bin/env bash
# Turn on the launcher's "Remote app buttons" accessibility service, which makes the remote's Netflix / YouTube /
# Prime Video / Apple TV buttons work without the stock home screen (needed on TCL; harmless elsewhere).
# Same as Settings > Accessibility > Remote app buttons > On.
#   TV=... scripts/app-keys.sh            (dry run)
#   TV=... APPLY=1 scripts/app-keys.sh
#   TV=... APPLY=1 OFF=1 scripts/app-keys.sh   (turn it off)
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
SVC="$PKG/.AppKeyService"
installed "$PKG" || die "Launcher is not installed."
cur="$(sh_ settings get secure enabled_accessibility_services)"; [[ "$cur" == null ]] && cur=""
if [[ -n "${OFF:-}" ]]; then
  new="$(tr ':' '\n' <<<"$cur" | grep -vxF "$SVC" | paste -sd: -)"
else
  new="$(printf '%s\n%s\n' "$cur" "$SVC" | awk 'NF && !seen[$0]++' | paste -sd: -)"
fi
echo "enabled_accessibility_services: '${cur}' -> '${new}'"
require_apply
# TCL resets this app-op at boot; allow it so the service can be bound right now.
a shell appops set "$PKG" APP_AUTO_START allow >/dev/null 2>&1 || true
if [[ -n "$new" ]]; then
  a shell settings put secure enabled_accessibility_services "$new"
  a shell settings put secure accessibility_enabled 1
else
  a shell settings delete secure enabled_accessibility_services >/dev/null
  a shell settings put secure accessibility_enabled 0
fi
sleep 2
sh_ dumpsys accessibility | grep -E 'Bound services|Enabled services' | head -n 2
