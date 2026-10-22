#!/usr/bin/env bash
# Run after a firmware or Play Store update brings the stock home or screensaver back.
# Re-runs the same gates as the original debloat, then re-disables whatever in the ledger got re-enabled.
#   TV=... scripts/reapply.sh            (dry run)
#   TV=... APPLY=1 scripts/reapply.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
SLEEP="${DISABLE_SLEEP:-15}"
[[ -f "$LEDGER" ]] || die "No ledger at $LEDGER; nothing to reapply on this TV."
installed "$PKG" || die "Launcher is not installed. Run scripts/install.sh first."

todo=()
while read -r p; do
  [[ -n "$p" ]] || continue
  is_protected "$p" && die "Refusing: ledger entry $p is now protected by the profile. Edit the ledger or the profile first."
  if installed "$p" && enabled "$p"; then echo "  DISABLE   $p"; todo+=("$p"); fi
done < "$LEDGER"
echo "Will re-assert Home ($OURS) and the screensaver ($OUR_DREAM)."
[[ ${#todo[@]} -gt 0 ]] || echo "Every ledger package is still disabled."
require_apply

# Same gates as debloat.sh: Wi-Fi ownership can change with an update, so check it again before any disable.
[[ ${#todo[@]} -eq 0 ]] || wifi_gate "${todo[@]}"
a shell pm enable --user 0 "$PKG" >/dev/null
a shell cmd package set-home-activity --user 0 "$OURS" >/dev/null
a shell settings put secure screensaver_components "$OUR_DREAM"
a shell settings put secure screensaver_default_component "$OUR_DREAM"
if [[ "$SDK" -ge 29 ]]; then
  [[ "$(home_role)" == "$PKG" ]] || die "Refusing: couldn't give our launcher the HOME role (holder: $(home_role)). Nothing was disabled."
fi

for p in "${todo[@]}"; do
  a shell pm disable-user --user 0 "$p"
  sleep "$SLEEP"
done
a shell cmd package set-home-activity --user 0 "$OURS" >/dev/null
a shell input keyevent KEYCODE_HOME
sleep 2
echo "Resolved HOME: $(home_resolved)"
[[ "$(home_resolved)" == "$OURS" ]] || echo "Home isn't ours yet. Run scripts/survey.sh: something new may out-rank the launcher."
