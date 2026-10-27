# Changelog

All notable changes to this project. Versions match the launcher's `versionName`; each one is a git tag, and signed APKs are on [Releases](https://github.com/hunterbrewer04/clean-tv/releases).

## 0.5.4 (2026-10-27)

- **Renamed to Clean TV** (it was "Living Room Launcher", after the first TV it ran on). The app now shows up as "Clean TV", release APKs are `clean-tv-vX.Y.Z.apk`, and the repo moved to `hunterbrewer04/clean-tv`; old links redirect. The package ID (`com.hunterbrewer.livingroomlauncher`) is unchanged, so this installs as a normal update and keeps your tile order.
- `build-release.sh` reads signing settings from `SIGNING_PROPERTIES` only (the old `LIVING_ROOM_SIGNING_PROPERTIES` fallback is gone).
- The Android project moved into `launcher/`. The repo root now holds just the app, `scripts/` and `profiles/`. `scripts/build-release.sh` and CI build from the new location; the APK is unchanged.
- Added `CHANGELOG.md`, `SECURITY.md`, Dependabot for GitHub Actions, and README badges.

## 0.5.3 (2026-10-26)

- **Screensaver:** the clock drifts a little every minute so it can't burn into OLED panels. It now redraws once a minute instead of every 10 seconds.
- **Fix:** a keyboard or game controller connecting or disconnecting no longer restarts the launcher. Before, that reset focus and closed an open menu.
- New installs put the most common apps first (Netflix, YouTube, Disney+, Prime Video, ...). Saved orders aren't touched.
- `hisense` profile no longer disables TiviMate (`ar.tvplayer.tv`). It's a user-installed player, not a preload.
- README: real screenshots, release download, TCL listed as tested, signing-key note. Issue templates for bugs and brand profile reports.

## 0.5.2 (2026-10-22)

- **TCL support.** New `tcl` profile with batches tested on a TCL Google TV (Realtek, Android TV 11).
- **Remote app buttons:** an optional accessibility service that makes the remote's Netflix, YouTube, Prime Video, Apple TV, gear and Input keys work after the stock home screen is disabled. Turn it on with `scripts/app-keys.sh`.
- Inputs: Live TV now always points at the panel's real hardware tuner. Some firmware registers preloaded apps as tuners too.

## 0.5.0 (2026-10-22)

- **Works on any brand.** The Inputs menu is built at runtime from the sources the TV reports, with the TV's own source picker as the last entry. Nothing is hardcoded per model.
- **Profile-based scripts.** `scripts/lib.sh` picks the TV by IP, name or sole device, detects the brand, and keeps per-device state. Profiles: `google` (baseline), `hisense`, `tcl`. `PROTECT` / `PROTECT_PREFIX` are enforced on every write.
- `survey.sh` (read-only inventory) and `install.sh`. Every script that changes the TV is a dry run unless `APPLY=1`.
- MIT license, CI (unit tests, lint, release build, shellcheck), public README.

## 0.4.1 (2026-10-21)

- Header greeting: normal letter spacing, slightly larger, small gap above the clock.

## 0.4.0 (2026-10-21)

- **Hold OK on an app** for a menu: Move, plus Uninstall for apps you installed yourself (through the system's own confirm dialog).
- Batched, reversible debloat with a ledger, so restore and reapply undo exactly what was changed. Health and state checks.

## 0.3.0 (2026-10-20)

- Removed the featured "Top Shelf" row. The app grid now sits directly under the header.

## 0.2.0 (2026-10-20)

- App cards use each app's TV banner, or its icon on a color sampled from it.
- Animated focus with lift, shadow and glare.

## 0.1.0 (2026-10-20)

- First version: a native Kotlin TV launcher with a clock header and app grid, plus guarded install/restore scripts.
