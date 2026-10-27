# Clean TV setup guide

This is the full walkthrough: turn on ADB, survey the TV, install the launcher, hand Home over, and clear out bloat in reversible batches. The [README](../README.md) is the short version.

You can also have an AI assistant walk you through it. See [AI-PROMPT.md](AI-PROMPT.md).

> [!WARNING]
> Disabling system packages on firmware you don't know can break things. Everything here uses `pm disable-user`, which is reversible: nothing is uninstalled and `restore.sh` undoes it. Still, go one batch at a time, and never factory-reset to "fix" something (you'd lose every login). If a TV boots into Android Recovery, choose **Try again**.

**Contents:** [What you need](#what-you-need) · [1. Turn on ADB](#1-turn-on-adb) · [2. Survey](#2-survey-read-only) · [3. Install](#3-install-the-launcher) · [4. Test side by side](#4-test-it-side-by-side) · [5. Hand Home over](#5-hand-home-over) · [6. Debloat](#6-debloat-in-batches) · [Remote app buttons](#remote-app-buttons-tcl-and-similar) · [Undo and updates](#undo-and-after-updates) · [Troubleshooting](#troubleshooting) · [Adding a brand](#adding-a-brand) · [How it works](#how-it-works)

## What you need

- A computer on the same network as the TV.
- `adb` (Android platform-tools). On macOS: `brew install --cask android-platform-tools`. On Debian/Ubuntu: `sudo apt install adb`. Or get it from [developer.android.com/tools/releases/platform-tools](https://developer.android.com/tools/releases/platform-tools).
- **bash 4 or newer.** Linux works as is. On Windows, use WSL. macOS still ships bash 3.2, which is too old: run `brew install bash`, open a new terminal, and check that `bash --version` says 5.x. The scripts start with `#!/usr/bin/env bash`, so they pick up the Homebrew bash from your PATH.
- A copy of this repo:

```bash
git clone https://github.com/hunterbrewer04/clean-tv.git
cd clean-tv
```

All commands below are run from the repo root.

## 1. Turn on ADB

On the TV: **Settings → System → About**, then click **Android TV OS build** seven times. Then **Settings → System → Developer options → USB debugging** (some sets call it Network/ADB debugging). Find the TV's IP under **Settings → Network**.

```bash
adb connect 192.168.1.50
```

Accept the "Allow debugging?" prompt on the TV. On Android 11+ sets that only offer **Wireless debugging**, pair first with `adb pair IP:PORT`, then pass that `IP:PORT` as `TV`.

Every script takes the target as `TV=`, which can be an IP, `IP:port`, or a name from `devices.conf`. With one device connected you can leave it out.

```bash
# devices.conf (gitignored)
living=192.168.1.50:5555
bedroom=192.168.1.51:5555
```

## 2. Survey (read-only)

```bash
TV=bedroom scripts/survey.sh
```

This prints the model, the HOME candidates and their priority, the TV inputs, who owns the saved Wi-Fi, what each profile batch would touch, and the **review candidates**: system packages that are neither protected nor in a batch. It saves a copy under `state/<serial>/` (gitignored) so you have a baseline.

## 3. Install the launcher

Download the signed APK and its `.sha256` from [Releases](https://github.com/hunterbrewer04/clean-tv/releases/latest), then install it:

```bash
TV=bedroom APPLY=1 APK=~/Downloads/clean-tv-v0.5.4.apk scripts/install.sh
```

If the `.sha256` file sits next to the APK, `install.sh` checks it first. You can also build your own: you need JDK 17 plus an Android SDK with platform 34, and the signing setup is described at the top of `scripts/build-release.sh`. Without an `APK=`, `install.sh` installs the newest APK in `dist/`:

```bash
scripts/build-release.sh
TV=bedroom APPLY=1 scripts/install.sh
```

> [!NOTE]
> Android only accepts updates signed with the same key. If you switch between the release APK and your own build, uninstall first. That resets your saved tile order, but nothing else.

This installs **alongside** the stock home screen. Nothing is disabled and Home isn't changed yet.

## 4. Test it side by side

Open it from the stock home screen's app row and check:

- [ ] D-pad reaches every app and header button
- [ ] Apps open and are still signed in
- [ ] Get Apps opens Google Play, and a newly installed app shows up without a reboot
- [ ] Hold OK → Move, rearrange, and the order survives leaving and coming back
- [ ] Wi-Fi and Settings open
- [ ] Every entry in Inputs switches to the right source, Live TV opens the tuner, and "All sources" opens the TV's picker
- [ ] Chromecast, AirPlay, and your remote still work
- [ ] The Home button still gets you back to the stock home screen

Don't go on until all of these pass.

## 5. Hand Home over

```bash
TV=bedroom APPLY=1 scripts/activate-home.sh     # preferred Home + clock screensaver; disables nothing
TV=bedroom scripts/debloat.sh handover          # dry run: shows what it will disable
TV=bedroom APPLY=1 scripts/debloat.sh handover
TV=bedroom scripts/health-check.sh
```

`handover` is its own step because Android resolves Home by **intent-filter priority before** it checks the default you picked. Google's stock home (priority 2) and setup wizard (priority 1) both out-rank any third-party launcher (0), so the Home button keeps going to stock until they're disabled. The script finds everything that out-ranks the launcher at runtime and checks a few things before it disables anything:

- The launcher has to hold the HOME role before anything gets disabled.
- **Wi-Fi ownership is checked.** Disabling the app that saved a network deletes that network. The setup wizard often owns the network you joined during setup, and if it does, the script refuses. Forget and re-join that network in Settings, then run it again.
- Out-ranking vendor packages stop the script so a human can look at them first.

## 6. Debloat in batches

```bash
TV=bedroom scripts/debloat.sh ambient           # dry run first, always
TV=bedroom APPLY=1 scripts/debloat.sh ambient
TV=bedroom scripts/health-check.sh
```

Each profile lists its batches, and `survey.sh` shows them. The baseline order is `handover`, `ambient`, `google-extras`, `preloads`; brand profiles add their own, such as `vidaa` on Hisense or `tcl-telemetry` and `tcl-content` on TCL. After each batch, use the TV for a bit before moving on. To disable a single package you've researched from the review list:

```bash
TV=bedroom APPLY=1 scripts/debloat.sh extra com.vendor.something
```

Everything disabled goes into `state/<serial>/disabled-by-us.txt`. The scripts refuse to touch anything matched by `PROTECT` or `PROTECT_PREFIX` in the profile: Play services, casting, the remote service, WebView, keyboards, TV settings/providers, chipset namespaces (`com.mediatek.`, `com.mstar.`, `com.realtek.`, `com.amlogic.`, ...), and each brand's own input/OTA/system stack.

## Remote app buttons (TCL and similar)

On some firmware (TCL Google TV, at least), the remote's Netflix, YouTube, Prime Video, Apple TV, gear and Input keys send vendor keycodes that only Google's stock home screen answers, so they go dead after the handover. The launcher ships an optional accessibility service, **Remote app buttons**, that catches just those keys and opens the matching app. It reads nothing on screen, and any key it doesn't know passes through untouched.

```bash
TV=bedroom APPLY=1 scripts/app-keys.sh     # same as Settings > Accessibility > Remote app buttons > On
```

> [!NOTE]
> TCL's auto-start blocker (`APP_AUTO_START` app-op) resets to "ignore" on every boot, which stops Android from binding the service. Until that's solved, re-run `scripts/app-keys.sh` after a reboot if the app buttons stop working.

## Undo, and after updates

```bash
TV=bedroom APPLY=1 scripts/restore.sh     # re-enable exactly what we disabled, give Home + screensaver back to stock
TV=bedroom APPLY=1 scripts/reapply.sh     # after a firmware update brings the stock home back
```

Every script that changes the TV prints its plan and stops unless `APPLY=1` is set. `restore.sh` keeps any package it couldn't re-enable in the ledger and exits non-zero, so you can run it again. `reapply.sh` re-checks the HOME role and Wi-Fi ownership before it disables anything.

## Troubleshooting

| Symptom | What to do |
|---|---|
| `Can't reach ... over adb` | Debugging got switched off, or the TV changed IP. Check **Settings → Network**, run `adb connect` again, and accept the prompt on the TV. |
| `adb devices` shows `unauthorized` | The "Allow debugging?" prompt is waiting on the TV. If it never appears, run `adb kill-server` and connect again. |
| `Refusing: <pkg> owns N saved Wi-Fi network(s)` | Disabling that package would delete those networks. If it owns the one you're on, forget it and re-join it in **Settings → Network**, then re-run. If it only owns old networks, decide whether you can lose them. |
| Home button still opens the stock home | `handover` hasn't run or didn't finish. `scripts/health-check.sh` shows which app Home resolves to, and `scripts/survey.sh` lists every HOME candidate with its priority. Anything above 0 still out-ranks the launcher. |
| `mapfile: command not found` or `bad substitution` | Your bash is older than 4 (stock macOS). See [What you need](#what-you-need). |
| Something broke after a batch | `TV=... APPLY=1 scripts/restore.sh` puts everything back. Then open an issue with the model, the Android version and the batch name. |
| TV boots into Android Recovery | Choose **Try again**. Never factory reset. |

## Adding a brand

Copy `profiles/tcl.sh` to `profiles/<manufacturer>.sh`. The filename is `ro.product.manufacturer` lowercased; check it with `adb shell getprop ro.product.manufacturer`. Add the brand's system namespaces to `PROTECT_PREFIX`, then add batches only for packages you've disabled on a real set and lived with. PRs with tested profiles are welcome. Please note the model, the Android version, and what you checked afterward.

Machine-specific tweaks, like an extra batch or a different `DISABLE_SLEEP`, go in `profiles/local.sh`. That file is gitignored and sourced last.

## How it works

- **Target and profile.** `scripts/lib.sh` is sourced by every script. It resolves `TV=`, reads `ro.product.manufacturer`, and loads `profiles/<brand>.sh`. If no profile matches, it falls back to `profiles/google.sh`, which only lists Google packages that are the same on every brand. `PROFILE=` overrides the choice.
- **Per-TV state.** `state/<serial>/` holds the ledger (`disabled-by-us.txt`), the stock Home and screensaver recorded before the first change (`stock.env`), and survey output. Some TVs report a placeholder serial; then the scripts build an ID from brand, model and address so two TVs never share a ledger.
- **Reversible by design.** Packages are disabled with `pm disable-user --user 0`, never uninstalled. After each disable the script waits `DISABLE_SLEEP` seconds (default 15), because some firmware drops a disable that's followed too quickly by a reboot. `restore.sh` re-enables only what's in the ledger, so anything you disabled yourself stays disabled.
- **Inputs.** The launcher builds its Inputs menu at runtime from `TvInputManager`. It drops hidden inputs, HDMI-CEC child devices, AirPlay and app-registered tuners, keeps one Live TV entry, and ends with the TV's own source picker. Nothing is hardcoded per model.

## Layout

```
launcher/   the Android app (Gradle project): Kotlin, one Activity, one Canvas View, a DreamService
scripts/    lib.sh (shared) + survey, install, activate-home, debloat, health-check, restore, reapply, app-keys, smoke-check, build-release
profiles/   google.sh (baseline), hisense.sh, tcl.sh
state/      per-TV ledgers and surveys (gitignored)
docs/       this guide, the AI prompt, screenshots
```
