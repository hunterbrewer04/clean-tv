#!/usr/bin/env bash
# Read-only. Run this first on any TV. It changes nothing and saves a copy under state/<serial>/.
#   TV=192.168.1.50 scripts/survey.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
set +e
OUT="$STATE/survey-$(date +%Y%m%d-%H%M%S).txt"

sh_ pm list packages -f | sort > "$STATE/packages.txt"
sh_ pm list packages -s | sed 's/^package://' | sort > "$STATE/system.txt"
sh_ pm list packages -3 | sed 's/^package://' | sort > "$STATE/thirdparty.txt"
sh_ pm list packages -d | sed 's/^package://' | sort > "$STATE/disabled.txt"

ALL_BATCHED="$(for b in "${BATCH_ORDER[@]}"; do batch_packages "$b"; done)"
in_batches() { grep -x "$1" <<<"$ALL_BATCHED" >/dev/null; }

{
echo "=== Device"
for p in ro.product.manufacturer ro.product.brand ro.product.model ro.product.device ro.build.version.release ro.build.version.sdk ro.product.cpu.abi; do
  printf '%-28s %s\n' "$p" "$(sh_ getprop "$p")"
done
printf '%-28s %s\n' "RAM" "$(sh_ cat /proc/meminfo | awk '/MemTotal/ {printf "%.1f GB", $2/1048576}')"
echo "Profile: profiles/$PROFILE.sh   (state dir: state/$SERIAL)"

echo; echo "=== HOME candidates (priority  component). Anything above 0 must be disabled for Home to reach us."
home_candidates | sort -rn | sed 's/^/  /'
echo "  HOME role holder: $(home_role)"
echo "  Resolved HOME:    $(home_resolved)"
echo "  Screensaver:      $(screensaver)"

echo; echo "=== TV inputs (empty on streaming sticks/boxes)"
sh_ dumpsys tv_input | sed -n 's/^ *\([^ ]*\): info: TvInputInfo{.*pkg=\([^,]*\),.*/  \1/p' | sort

echo; echo "=== Saved Wi-Fi owners (a disabled owner deletes the networks it saved)"
sh_ dumpsys wifi | grep -o 'cname=[^ ]*' | sort | uniq -c | sed 's/^/  /'

echo; echo "=== Batches in profiles/$PROFILE.sh"
for b in "${BATCH_ORDER[@]}"; do
  echo "  [$b]"
  while read -r p; do
    [[ -n "$p" ]] || continue
    if ! installed "$p"; then st="absent "; elif enabled "$p"; then st="enabled"; else st="disabled"; fi
    echo "    $st  $p"
  done < <(batch_packages "$b")
done

echo; echo "=== Review candidates: system packages not protected and not in any batch"
echo "    Look these up before touching them. Unknown vendor packages are the ones that brick inputs or remotes."
while read -r p; do
  is_protected "$p" && continue; in_batches "$p" && continue
  if grep -x "$p" "$STATE/disabled.txt" >/dev/null; then st=disabled; else st=enabled; fi
  echo "    $st  $p"
done < "$STATE/system.txt"

echo; echo "=== User-installed or updated apps (only touched if a batch names them)"
sed 's/^/    /' "$STATE/thirdparty.txt"
echo; echo "=== Already disabled"
sed 's/^/    /' "$STATE/disabled.txt"
} | tee "$OUT"
echo; echo "Saved: $OUT"
