# Clean TV

[![CI](https://github.com/hunterbrewer04/clean-tv/actions/workflows/ci.yml/badge.svg)](https://github.com/hunterbrewer04/clean-tv/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/hunterbrewer04/clean-tv)](https://github.com/hunterbrewer04/clean-tv/releases/latest)
[![Android TV 6.0+](https://img.shields.io/badge/Android%20TV-6.0%2B-3DDC84?logo=android&logoColor=white)](#compatibility)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A small, ad-free home screen for Google TV and Android TV, plus reversible ADB scripts to switch the stock home screen off and clear out preinstalled bloat. It works on any brand.

![Clean TV home screen on a TCL Google TV](docs/screenshots/home.png)

| Hold OK on an app | Inputs menu | Clock screensaver |
|---|---|---|
| ![App menu with Move and Uninstall](docs/screenshots/app-menu.png) | ![Inputs menu listing HDMI 1-3, AV, Live TV and All sources](docs/screenshots/inputs.png) | ![Clock screensaver](docs/screenshots/screensaver.png) |

<sub>Real screenshots from a TCL Google TV, taken over ADB. The screensaver shot is re-centered; on the TV it drifts slowly to protect the panel.</sub>

**[Download the latest APK](https://github.com/hunterbrewer04/clean-tv/releases/latest)** · **[Setup guide](docs/GUIDE.md)** · **[Set it up with an AI assistant](docs/AI-PROMPT.md)** · [What's new](CHANGELOG.md)

## Features

- **No ads, no tracking, no network.** The APK is about 115 KB, pure Kotlin, with no third-party runtime libraries. Its only permission opens the system "Uninstall?" dialog.
- **Clock, greeting and date header** over a five-column grid of your installed TV apps, with banner art when an app has it.
- **Hold OK on an app** to move it, or to uninstall it if you installed it yourself.
- **Inputs menu** built from whatever HDMI/AV/tuner sources the TV reports, with nothing hardcoded per model.
- **Wi-Fi and Settings shortcuts**, and Play Store as **Get Apps**. New installs show up right away.
- **Clock screensaver** that drifts a little every minute so it can't burn into OLED panels.
- About 55–75 MB of RAM in use, vs about 150 MB for the stock Google TV home on the same set.

It never touches app data, so Netflix, Disney+ and everything else stay signed in. Chromecast, AirPlay, Bluetooth remotes, the Google TV / Home Assistant remote, TV inputs and the Play Store keep working, and the scripts refuse to disable the packages behind them.

## How it works

1. **Survey** the TV with a read-only script that saves a baseline.
2. **Install** the launcher next to the stock home screen and test it.
3. **Hand Home over** to Clean TV, then **disable bloat** one batch at a time.
4. **Undo** whenever you want with `restore.sh`. Nothing is uninstalled.

```bash
adb connect 192.168.1.50
TV=192.168.1.50 scripts/survey.sh                                     # read-only
TV=192.168.1.50 APPLY=1 APK=clean-tv-v0.5.4.apk scripts/install.sh   # alongside stock, changes nothing else
```

Every script that changes the TV is a dry run until you add `APPLY=1`. The **[setup guide](docs/GUIDE.md)** has the full walkthrough: enabling ADB, the side-by-side checklist, the Home handover, batches, TCL remote buttons, undo, troubleshooting, and adding a brand.

> [!WARNING]
> Disabling system packages on firmware you don't know can break things. Everything here is reversible, but go one batch at a time, and never factory-reset to "fix" something (you'd lose every login). If a TV boots into Android Recovery, choose **Try again**.

**Prefer to have AI walk you through it?** [AI-PROMPT.md](docs/AI-PROMPT.md) has a copy-paste prompt for ChatGPT, Claude, Gemini or a coding agent. It points the assistant at the guide and keeps it on the safe path.

## Compatibility

| | |
|---|---|
| OS | Android TV / Google TV, Android 6.0+ (API 23). Home handover tested on Android 11 |
| Tested | Hisense Google TV (Android TV 11, MediaTek), TCL Google TV (Android TV 11, Realtek) |
| Profiles | `google` (any brand, the baseline), `hisense`, `tcl` |
| Streaming sticks | Launcher works. The Inputs menu just has fewer entries (or none) |
| Your computer | `adb` and bash 4+: Linux, macOS (with Homebrew bash), or Windows via WSL |

The launcher is brand-neutral. Brands differ in **which packages are safe to disable**, and that lives in `profiles/<brand>.sh`. Any other brand gets the `google` baseline, which only touches Google packages that are the same on every TV. Tested a new brand? See [Adding a brand](docs/GUIDE.md#adding-a-brand).

## Limits

- No cross-app "Continue Watching" row. Reading other apps' watch-next data needs a privileged permission that only the stock launcher gets.
- Screenshots often come back blank on TVs (secure video layers), so `survey.sh` reports text only.

## License

MIT, see [LICENSE](LICENSE). The bundled Outfit font is under the SIL Open Font License ([LICENSES/Outfit-OFL.txt](LICENSES/Outfit-OFL.txt)).
