#!/usr/bin/env bash
# Make the launcher the preferred Home and its clock the screensaver. Disables nothing.
#   TV=... scripts/activate-home.sh            (dry run)
#   TV=... APPLY=1 scripts/activate-home.sh
# Only run this after the launcher has passed the side-by-side checklist in the README.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
installed "$PKG" || die "Launcher is not installed. Run scripts/install.sh first."

blockers="$(home_blockers)"
echo "Will set preferred Home to $OURS and the screensaver to $OUR_DREAM."
[[ -z "$blockers" ]] || printf 'Out-ranking Home apps (handled later by the handover batch):\n%s\n' "$(sed 's/^/  /' <<<"$blockers")"
require_apply

record_stock
a shell cmd package set-home-activity --user 0 "$OURS"
a shell settings put secure screensaver_components "$OUR_DREAM"
a shell settings put secure screensaver_default_component "$OUR_DREAM"
touch "$STATE/home-activated"
sleep 2
a shell input keyevent KEYCODE_HOME
echo "HOME role:     $(home_role)"
echo "Resolved HOME: $(home_resolved)"
[[ -z "$blockers" ]] || echo "Home button reaches the launcher after: APPLY=1 scripts/debloat.sh handover"
