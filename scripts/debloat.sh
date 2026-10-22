#!/usr/bin/env bash
# Disable one batch from the TV's profile, or specific packages. Dry run unless APPLY=1.
#   TV=... scripts/debloat.sh handover                 (always first, after activate-home.sh)
#   TV=... APPLY=1 scripts/debloat.sh ambient
#   TV=... APPLY=1 scripts/debloat.sh extra com.example.bloat [more...]
# Run one batch, use the TV for a bit (casting, remote, inputs, apps), then do the next.
# Every package goes into state/<serial>/disabled-by-us.txt so restore.sh undoes exactly what we changed.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
BATCH="${1:-}"; shift || true
SLEEP="${DISABLE_SLEEP:-15}"   # some firmware (Hisense) drops a disable that's followed too quickly by a reboot

if [[ "$BATCH" == extra ]]; then
  [[ $# -gt 0 ]] || die "Usage: $0 extra <package> [package...]"
  PKGS=("$@")
elif [[ -n "$BATCH" && " ${BATCH_ORDER[*]} " == *" $BATCH "* ]]; then
  mapfile -t PKGS < <(batch_packages "$BATCH")
else
  die "Usage: $0 <$(IFS='|'; echo "${BATCH_ORDER[*]}")|extra pkg...>   (profile: $PROFILE)"
fi

if [[ "$BATCH" == handover ]]; then
  # Android picks the highest-priority HOME activity before it looks at the HOME role, so everything that
  # out-ranks us has to go in this batch. Google's own packages (setup wizard, stock home) are added
  # automatically; a vendor package is a surprise worth a human look.
  while read -r b; do
    [[ -n "$b" ]] || continue
    if [[ "$b" == com.google.* || "${HOME_BLOCKERS_OK:-}" == 1 ]]; then PKGS+=("$b")
    else die "Refusing: vendor package $b out-ranks our launcher for HOME. Check what it is; re-run with HOME_BLOCKERS_OK=1 to include it."
    fi
  done <<<"$(home_blockers)"
fi
mapfile -t PKGS < <(printf '%s\n' "${PKGS[@]}" | awk 'NF && !seen[$0]++')

todo=()
for p in "${PKGS[@]}"; do
  is_protected "$p" && die "Refusing: $p is protected (see PROTECT in profiles/). Take it out of the batch."
  if ! installed "$p"; then echo "  absent    $p"
  elif ! enabled "$p"; then echo "  disabled  $p"
  else echo "  DISABLE   $p"; todo+=("$p")
  fi
done
[[ ${#todo[@]} -gt 0 ]] || { echo "Nothing to do for '$BATCH'."; exit 0; }
require_apply

# ---- gates ----
if [[ "$BATCH" == handover ]]; then
  if [[ "$SDK" -ge 29 ]]; then
    [[ "$(home_role)" == "$PKG" ]] || die "Refusing: our launcher doesn't hold the HOME role. Run APPLY=1 scripts/activate-home.sh first."
  else
    [[ -f "$STATE/home-activated" ]] || die "Refusing: run APPLY=1 scripts/activate-home.sh first."
  fi
else
  [[ "$(home_resolved)" == "$OURS" ]] || die "Refusing: Home is $(home_resolved), not our launcher. Run the handover batch first."
fi
if [[ "$BATCH" == ambient ]]; then
  [[ "$(screensaver)" == "$OUR_DREAM" ]] || die "Refusing: the screensaver isn't our clock yet. Run APPLY=1 scripts/activate-home.sh."
fi
wifi_gate "${todo[@]}"

record_stock
for p in "${todo[@]}"; do
  a shell pm disable-user --user 0 "$p"
  grep -qx "$p" "$LEDGER" 2>/dev/null || echo "$p" >> "$LEDGER"
  sleep "$SLEEP"
done

# Package changes can clear the preferred activity; re-assert it and go Home.
a shell cmd package set-home-activity --user 0 "$OURS" >/dev/null
a shell input keyevent KEYCODE_HOME
sleep 2
echo "Resolved HOME: $(home_resolved)"
echo "Ledger: $LEDGER ($(wc -l < "$LEDGER") packages). Run scripts/health-check.sh next."
