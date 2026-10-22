#!/usr/bin/env bash
# Undo everything on this TV: re-enable exactly what our scripts disabled, then give Home and the screensaver
# back to the stock apps. The launcher stays installed (uninstall it from Settings > Apps if you want).
#   TV=... scripts/restore.sh            (dry run)
#   TV=... APPLY=1 scripts/restore.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
STOCK_HOME=""; STOCK_DREAM=""
# shellcheck source=/dev/null
[[ -f "$STOCK_ENV" ]] && source "$STOCK_ENV"

pkgs=()
if [[ -f "$LEDGER" ]]; then mapfile -t pkgs < <(awk 'NF' "$LEDGER"); else echo "No ledger at $LEDGER; nothing to re-enable."; fi
for p in "${pkgs[@]}"; do echo "  ENABLE    $p"; done
echo "Home -> ${STOCK_HOME:-(best-ranked stock Home, decided after re-enabling)}"
echo "Screensaver -> ${STOCK_DREAM:-(firmware default)}"
require_apply

failed=()
for p in "${pkgs[@]}"; do
  installed "$p" || { echo "  gone      $p (uninstalled by an update?)"; continue; }
  a shell pm enable --user 0 "$p" >/dev/null 2>&1 || true
  if enabled "$p"; then echo "  enabled   $p"; else echo "  FAILED    $p"; failed+=("$p"); fi
done

# The recorded Home has to exist again; otherwise take the best-ranked HOME that isn't ours.
if [[ -z "$STOCK_HOME" ]] || ! home_candidates | awk '{print $2}' | grep -x "$STOCK_HOME" >/dev/null; then
  STOCK_HOME="$(home_candidates | awk -v me="$PKG" '$1 >= 0 { split($2, c, "/"); if (c[1] != me) print $1, $2 }' | sort -rn | awk 'NR==1 {print $2}')"
fi
if [[ -n "$STOCK_DREAM" && "$STOCK_DREAM" != null ]]; then
  a shell settings put secure screensaver_components "$STOCK_DREAM"
  a shell settings put secure screensaver_default_component "$STOCK_DREAM"
else
  a shell settings delete secure screensaver_components >/dev/null       # falls back to the firmware default
  a shell settings delete secure screensaver_default_component >/dev/null
fi
[[ -n "$STOCK_HOME" ]] && a shell cmd package set-home-activity --user 0 "$STOCK_HOME"
sleep 2
a shell input keyevent KEYCODE_HOME
echo "Resolved HOME: $(home_resolved)"

if [[ ${#failed[@]} -gt 0 ]]; then
  # Keep only what's still disabled so the next run retries exactly those.
  printf '%s\n' "${failed[@]}" > "$LEDGER"
  die "Could not re-enable ${#failed[@]} package(s); they're still in $LEDGER. Re-run restore.sh."
fi
[[ -f "$LEDGER" ]] && mv "$LEDGER" "$LEDGER.restored-$(date +%Y%m%d-%H%M%S)"
rm -f "$STATE/home-activated"
