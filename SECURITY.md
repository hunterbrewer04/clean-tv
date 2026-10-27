# Security

## Reporting a vulnerability

Please don't open a public issue. Use **[Report a vulnerability](https://github.com/hunterbrewer04/clean-tv/security/advisories/new)** (GitHub private reporting) instead. It goes only to the maintainer, and stays private until a fix is out.

Useful to include: the launcher version, the TV model and Android version, and the steps to reproduce.

## Scope

- **The launcher app.** It has no network access, and its only permission is `REQUEST_DELETE_PACKAGES`, which opens the system's own uninstall confirmation. The optional "Remote app buttons" accessibility service only reads key events. Anything that lets another app or a remote input abuse these is in scope.
- **The scripts.** They run `adb` commands against a TV you choose. Anything that could make them change a device or package you didn't ask for is in scope, for example getting around `PROTECT` lists or the `APPLY=1` gate.

Only the latest release gets fixes.

## Verifying a download

Each release lists the APK's SHA-256 next to it (`.sha256` file), and the signing certificate's SHA-256 in the release notes:

```
49d92732245eee5d407e04e0446d66139370b827a3ab2ed7407b5665ff5b7c93
```

Check the certificate with `apksigner verify --print-certs clean-tv-vX.Y.Z.apk`.
