# Living Room Launcher

A private, lightweight Android TV launcher built specifically for Hunter's Hisense `songshan` TV (Android TV 11, 32-bit MediaTek, about 1.8 GB RAM). It replaces the ad-heavy home screen without replacing app data, Google Play, Chromecast, AirPlay, TV inputs, or Home Assistant's remote service.

## Current install candidate: v0.1.0

- Native Kotlin + one custom Android `View`; no Compose, analytics, trackers, or third-party runtime libraries.
- Dynamic greeting, clock, date, and Apple TV-inspired Top Shelf layout.
- Five-column app grid populated from installed Leanback apps.
- Focus lift, D-pad navigation, and hold-OK app rearranging with persistent order.
- Header buttons for Inputs, Wi-Fi, and Settings.
- Exact input routes for HDMI 1-4, composite AV, and the antenna tuner on this TV.
- Play Store appears as **Get Apps** and newly installed apps appear automatically.
- Jellyfin is first and gets a dedicated shelf. v0.1 deliberately does not extract credentials or viewing history from Jellyfin's private app data; selecting Jellyfin opens the existing signed-in app.
- Included clock DreamService, used before Google DreamX is disabled.
- Reversible, guarded operations scripts. Installation is separate from Home activation and debloating.

## Safety model

1. Build and test the APK off-device.
2. Install it **alongside** the stock launcher and launch it explicitly.
3. Test apps, Play Store, inputs, D-pad, casting, AirPlay, and HA remote.
4. Only then run Home activation.
5. Debloat in a later, explicitly approved phase. Scripts use `pm disable-user`; they do not clear app data.

Do not use factory reset. If this Hisense ever opens Android Recovery, choose **Try again**.

## Build on brewserver

The source of truth is this repo (cloned on micro-server at `~/Code/tv-debloat`). brewserver holds the Android SDK and a working copy at `~/Code/living-room-launcher`:

```bash
git clone git@github.com:hunterbrewer04/living-room-launcher.git ~/Code/living-room-launcher
cd ~/Code/living-room-launcher
scripts/build-release.sh
```

The signing keystore and properties live outside the repository. `*.jks`, `*.keystore`, `signing.properties`, `local.properties`, and APKs are ignored.

## Install without replacing Home

Run from the micro-server, while someone is at the TV:

```bash
cd ~/Code/tv-debloat
scripts/install-test.sh
```

## Test checklist before Home activation

- [ ] D-pad reaches every app and header button
- [ ] OK opens Jellyfin, Netflix, Disney+, Prime Video, Tubi, and Fubo
- [ ] Existing apps remain logged in
- [ ] Get Apps opens Google Play and can install an app
- [ ] A newly installed app appears without rebooting
- [ ] Hold OK, move an app, save, and verify order persists
- [ ] Wi-Fi and Settings open their native TV screens
- [ ] HDMI 1-4, AV, and Live TV routes open without crashing
- [ ] Chromecast still receives a cast
- [ ] AirPlay still receives a stream
- [ ] Home Assistant remote still controls the TV
- [ ] Home button can return safely to the stock launcher during alongside testing

After all checks pass:

```bash
APPLY=1 scripts/activate-home.sh
```

That command changes Home and the clock screensaver only. It does not disable stock packages.

## Recovery

```bash
scripts/restore.sh
```

The manual post-update repair is:

```bash
scripts/reapply.sh
```

## Known-safe boundary

Keep all MediaTek TV services, Chromecast, AirPlay/HomeKit, Android TV Remote, Bluetooth/autopair, TV inputs/tuner, Play Services, Play Store, WebView, keyboard, Dolby/audio, Hisense SmartSense, customer launcher, remote key services, and OTA components until separately reviewed.
