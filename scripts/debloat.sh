#!/usr/bin/env bash
# Usage: scripts/debloat.sh <batch>            (dry run)
#        APPLY=1 scripts/debloat.sh <batch>    (disable that batch)
# Batches: handover ambient google-extras vidaa preloads. Run one, test the TV, then the next.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/batches.sh"
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PKG=com.hunterbrewer.livingroomlauncher
LEDGER="$ROOT/state/disabled-by-us.txt"
BATCH="${1:-}"
[[ " ${BATCH_ORDER[*]} " == *" $BATCH "* ]] || { echo "Usage: $0 <${BATCH_ORDER[*]// /|}>" >&2; exit 2; }
mapfile -t PKGS < <(batch_packages "$BATCH")
a() { "$ADB" -s "$TV" "$@"; }
home() { a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1 | tr -d '\r'; }

echo "Batch '$BATCH' disables (user 0):"; printf '  %s\n' "${PKGS[@]}"
[[ "${APPLY:-}" == "1" ]] || { echo 'Dry run. Re-run with APPLY=1.'; exit 0; }

"$ADB" connect "$TV" >/dev/null
if [[ "$BATCH" == handover ]]; then
  # launcherx declares HOME priority 2 (setupwraith 1, ours 0), and Android always picks the highest priority,
  # so HOME can't resolve to us until these two are disabled. Require that we hold the HOME role instead.
  a shell dumpsys role | grep -A1 'name=android.app.role.HOME' | grep -q "holders=$PKG" \
    || { echo 'Refusing: our launcher does not hold the HOME role. Run APPLY=1 scripts/activate-home.sh first.' >&2; exit 1; }
else
  [[ "$(home)" == "$PKG/.MainActivity" ]] || { echo "Refusing: our launcher is not the resolved HOME ($(home)). Run the handover batch first." >&2; exit 1; }
fi
if [[ "$BATCH" == handover ]] && a shell dumpsys wifi | grep -q 'cname=com.google.android.tungsten.setupwraith'; then
  echo 'Refusing: a saved Wi-Fi network is owned by setupwraith; disabling it would delete Wi-Fi. Forget + rejoin the network in Settings first.' >&2; exit 1
fi
if [[ "$BATCH" == ambient ]]; then
  [[ "$(a shell settings get secure screensaver_components | tr -d '\r')" == "$PKG/.ClockDreamService" ]] || { echo 'Refusing: screensaver is not pointed at our ClockDreamService yet.' >&2; exit 1; }
fi

mkdir -p "$(dirname "$LEDGER")"
for p in "${PKGS[@]}"; do
  if ! a shell pm path "$p" >/dev/null 2>&1; then echo "skip (not installed) $p"; continue; fi
  a shell pm disable-user --user 0 "$p"
  grep -qx "$p" "$LEDGER" 2>/dev/null || echo "$p" >> "$LEDGER"
  sleep 15   # Hisense firmware can drop a disable that is followed too quickly by a reboot.
done

# Re-assert our HOME (package-state changes can clear the preferred activity) and return to it.
a shell cmd package set-home-activity --user 0 "$PKG/.MainActivity" >/dev/null
a shell input keyevent KEYCODE_HOME
sleep 2
echo "Resolved HOME: $(home)"
echo "Disabled now:"; a shell pm list packages -d | sed 's/^/  /'
