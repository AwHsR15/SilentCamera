# SilentCamera 2.1

[简体中文](README.md) · [日本語](README.ja.md) · **English**

Six independent camera sound modes for Japanese Android devices, capability detection, original-state restoration, and an in-app camera. Android 8.0+, Chinese/Japanese/English, light/dark themes. No network permission or telemetry.

**Stock-camera muting depends on the device, firmware and camera implementation. No universal success claim is made. Japanese OEM hardware remains unverified.** A configuration read-back is separate from real photo/sound confirmation.

## Downloads

- [Signed preview APK](release/SilentCamera-v2.1-preview.apk)
- [Complete bundle: APK, Magisk, PC tools and guides](release/SilentCamera-v2.1-bundle.zip)
- [Magisk module](release/SilentCamera-v2.1-magisk.zip)
- [Source archive](release/SilentCamera-v2.1-source.zip)
- [SHA256SUMS](release/SHA256SUMS) · [Source/artifact manifest](release/manifest.json)

The preview application ID is `com.marvis.silentcamera.preview`; it can coexist with v1. Preview signing cannot update the original app. Production signing uses externally supplied credentials. The [v1 APK](release/SilentCamera-v1.0-debug.apk) is retained.

## Modes

| Mode | Requirements / limits |
|---|---|
| Samsung CSC | Samsung firmware and Shizuku; make sound follow system settings |
| Enforced-audible volume group | Shizuku and supported CLI; discover an isolated group, refuse ambiguous or unrestorable states |
| Camera app audio | Shizuku; experimental PLAY_AUDIO control for the selected camera package, may affect other audio in that app |
| Root properties | Root and resetprop; change only detected known camera properties, experimental |
| Magisk sound overlay | Separate module for existing allowlisted shutter OGG files; reboot required |
| In-app camera | No Shizuku/root; CameraX still capture and a preview-frame save option |

No ring/media/call/alarm volume group is hardcoded or muted. Device-specific LSPosed hooks are covered as external research, not shipped as a universal executable mode.

## Use and restore

1. Install the APK, inspect detected modes. Start and authorize [Shizuku](https://shizuku.rikka.app/guide/setup/) for the first three system modes. Wireless startup usually requires Android 11+; older versions can use USB ADB.
2. Select a mode and camera package if required. Apply saves the original first and verifies the requested state. Reapplying keeps the first original.
3. Take a photo with the stock camera; record quiet/audible separately. An applied setting does not prove acoustic success.
4. Restore before changing approaches, clearing app data or uninstalling. Backups are local and do not participate in Android backup. Firmware or user mismatches reject restoration.

The in-app camera supports front/back, focus, zoom, grid and available flash/torch. Still capture plays no app shutter sound, but OEM HAL behavior can differ. Saving a preview frame avoids a normal still request and has preview-quality limits. Android 8–9 requires storage permission to save.

## PC tools and building

Unpack the bundle; install official [Platform Tools](https://developer.android.com/tools/releases/platform-tools), Python 3.10+, and authorize USB debugging:

```sh
python3 tools/adb_control.py detect
python3 tools/adb_control.py install
# Samsung only; retain the backup JSON
python3 tools/adb_control.py apply-csc --backup phone-original.json
python3 tools/adb_control.py restore-csc --backup phone-original.json
```

Use `tools/adb-control.sh` on Linux/macOS or `tools\adb-control.bat` on Windows. Select a device with `--serial`, or user with `--user`. Repository installs require `--apk release/SilentCamera-v2.1-preview.apk`.

Build with JDK 17, SDK 35 and Python 3.10+:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 -m unittest discover -s tools -p 'test_*.py'
bash tools/build-release.sh
```

Packaging requires a clean committed Git checkout. The source ZIP builds an APK; clone and check out its `SOURCE_COMMIT.txt` to recreate provenance-aware archives. GitHub Actions verifies and uploads CI artifacts; it does not overwrite committed release files.

[Compatibility and validation](docs/COMPATIBILITY.md) · [Magisk](docs/MAGISK.md) · [Release/signing/checksums](docs/RELEASE.md)

MIT License. Built with Shizuku, AndroidX CameraX and Jetpack Compose.
