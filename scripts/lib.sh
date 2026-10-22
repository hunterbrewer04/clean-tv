# shellcheck shell=bash
# Shared helpers for every script. Sourced, never run directly.
#
# Target selection (first match wins):
#   TV=192.168.1.50            IP, port 5555 assumed
#   TV=192.168.1.50:37123      IP:port (e.g. Android 11+ wireless debugging)
#   TV=bedroom                 a name from devices.conf (gitignored), lines like: bedroom=192.168.1.50:5555
#   (unset)                    the only device `adb devices` lists
#
# The brand profile is picked from ro.product.manufacturer (profiles/<brand>.sh, else profiles/google.sh).
# Override with PROFILE=<name>. Optional personal tweaks go in profiles/local.sh (gitignored).

# shellcheck disable=SC2034  # OURS, SDK, LEDGER, ... are used by the scripts that source this
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKG=com.hunterbrewer.livingroomlauncher
OURS="$PKG/.MainActivity"
OUR_DREAM="$PKG/.ClockDreamService"

if [[ -z "${ADB:-}" ]]; then
  ADB="$(command -v adb || true)"
  [[ -n "$ADB" ]] || ADB="$HOME/.local/bin/adb"
fi
[[ -x "$ADB" ]] || { echo "adb not found. Install Android platform-tools or set ADB=/path/to/adb." >&2; exit 1; }

die() { echo "$*" >&2; exit 1; }

resolve_tv() {
  local t="${TV:-}"
  if [[ -z "$t" ]]; then
    mapfile -t devs < <("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
    [[ ${#devs[@]} -eq 1 ]] || die "Set TV=<ip[:port]|name>. Connected devices: ${devs[*]:-none}"
    t="${devs[0]}"
  elif [[ -f "$ROOT/devices.conf" ]]; then
    # Literal key match (no regex), and refuse ambiguous names: picking the wrong TV is a safety failure.
    local hits
    hits="$(awk -F= -v key="$t" '$1 == key { sub(/^[^=]*=/, ""); print }' "$ROOT/devices.conf")"
    if [[ -n "$hits" ]]; then
      [[ "$(wc -l <<<"$hits")" -eq 1 ]] || die "devices.conf has more than one entry named '$t'."
      t="$hits"
    fi
  fi
  [[ -n "$t" ]] || die "Empty TV target."
  [[ "$t" == *:* || "$t" != *.*.*.* ]] || t="$t:5555"
  TV="$t"
}
resolve_tv

# </dev/null: adb shell otherwise eats stdin, which silently ends any `while read` loop after one iteration.
a() { "$ADB" -s "$TV" "$@" </dev/null; }
sh_() { a shell "$@" | tr -d '\r'; }

connect() {
  [[ "$TV" == *:* ]] && "$ADB" connect "$TV" >/dev/null 2>&1 || true
  a get-state >/dev/null 2>&1 || die "Can't reach $TV over adb. Is USB/network debugging on, and did you accept the prompt on the TV?"
}
connect

SDK="$(sh_ getprop ro.build.version.sdk)"
BRAND="$(sh_ getprop ro.product.manufacturer | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9-')"
MODEL="$(sh_ getprop ro.product.model)"
# State is keyed per TV. Many sets report a blank or placeholder serial, so fall back to the adb target
# (and include the model) rather than letting two TVs share one ledger.
SERIAL="$(sh_ getprop ro.serialno | tr -cd 'A-Za-z0-9._-')"
case "${SERIAL,,}" in
  ""|unknown|0|0123456789abcdef|serial|android|null) SERIAL="$(tr -cd 'a-z0-9-' <<<"${BRAND}-${MODEL,,}")-${TV//[^A-Za-z0-9]/_}" ;;
esac
STATE="$ROOT/state/$SERIAL"           # per-TV, gitignored: ledger, stock settings, surveys
LEDGER="$STATE/disabled-by-us.txt"
STOCK_ENV="$STATE/stock.env"
mkdir -p "$STATE"
# Bind the state dir to this device; refuse if it was created for a different model.
if [[ -f "$STATE/device.txt" ]]; then
  grep -qx "model=$MODEL" "$STATE/device.txt" \
    || die "state/$SERIAL belongs to a different device ($(grep '^model=' "$STATE/device.txt")). Not using its ledger."
else
  printf 'brand=%s\nmodel=%s\ntarget=%s\n' "$BRAND" "$MODEL" "$TV" > "$STATE/device.txt"
fi

# ---- profile ----
PROFILE="${PROFILE:-$BRAND}"
[[ -f "$ROOT/profiles/$PROFILE.sh" ]] || PROFILE=google
# shellcheck source=/dev/null
source "$ROOT/profiles/$PROFILE.sh"
# shellcheck source=/dev/null
[[ -f "$ROOT/profiles/local.sh" ]] && source "$ROOT/profiles/local.sh"

batch_packages() { local v="BATCH_${1//-/_}[@]"; printf '%s\n' "${!v}"; }

is_protected() {
  local p="$1" x
  for x in "${PROTECT[@]}"; do [[ "$p" == "$x" ]] && return 0; done
  for x in "${ALLOW[@]}"; do [[ "$p" == "$x" ]] && return 1; done
  for x in "${PROTECT_PREFIX[@]}"; do [[ "$p" == "$x"* ]] && return 0; done
  return 1
}

installed() { a shell pm path "$1" >/dev/null 2>&1; }
# No `grep -q` on adb pipelines: with pipefail, grep quitting early can SIGPIPE the writer and flip the result.
enabled() { sh_ pm list packages -e "$1" | grep -x "package:$1" >/dev/null; }

# ---- HOME ----
home_resolved() { sh_ cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1; }
home_role() { sh_ dumpsys role | grep -A1 'name=android.app.role.HOME' | sed -n 's/.*holders=//p' | head -n 1; }

# "<priority> <component>" for every enabled HOME activity.
home_candidates() {
  sh_ pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
    | awk '/priority=/ { sub(/.*priority=/, ""); split($0, f, " "); pr = f[1]; next }
           /\// { gsub(/^ +/, ""); print pr, $0 }'
}

# Packages whose HOME priority beats ours (0). Android picks the highest priority before it looks at the HOME
# role or the preferred activity, so every one of these has to be disabled before Home resolves to us.
home_blockers() { home_candidates | awk -v me="$PKG" '$1 > 0 { split($2, c, "/"); if (c[1] != me) print c[1] }' | sort -u; }

screensaver() { sh_ settings get secure screensaver_components; }

# Abort unless APPLY=1. Every script that changes the TV calls this after printing its plan.
require_apply() { [[ "${APPLY:-}" == "1" ]] || { echo "Dry run ($PROFILE profile, $TV). Re-run with APPLY=1."; exit 0; }; }

# Refuse if any package in "$@" owns a saved Wi-Fi network: disabling it deletes those networks
# (and drops adb if one of them is the network in use).
wifi_gate() {
  local wifi p n
  wifi="$(sh_ dumpsys wifi | grep -o 'cname=[^ ]*' || true)"
  for p in "$@"; do
    n="$(awk -v p="$p" '{ c = substr($0, 7); sub(/:.*/, "", c) } c == p { n++ } END { print n + 0 }' <<<"$wifi")"
    [[ "$n" == 0 ]] || die "Refusing: $p owns $n saved Wi-Fi network(s); disabling it deletes them.
Forget and re-join those networks in TV Settings first (list them: adb shell cmd wifi list-networks)."
  done
}

# Save the stock Home and screensaver once, before we change them, so restore.sh can put them back.
record_stock() {
  [[ -f "$STOCK_ENV" ]] && return 0
  local home dream
  # What Home actually is right now wins; candidate ranking is only a fallback.
  home="$(home_resolved)"
  if [[ "$home" != */* || "$home" == "$OURS" ]]; then
    home="$(home_candidates | awk -v me="$PKG" '$1 >= 0 { split($2, c, "/"); if (c[1] != me) print $1, $2 }' | sort -rn | awk 'NR==1 {print $2}')"
  fi
  dream="$(screensaver)"; [[ "$dream" == "$OUR_DREAM" ]] && dream=""
  printf 'STOCK_HOME=%q\nSTOCK_DREAM=%q\n' "$home" "${dream%%,*}" > "$STOCK_ENV"
  sh_ pm list packages -d | sed 's/^package://' | sort > "$STATE/disabled.before.txt"
}
