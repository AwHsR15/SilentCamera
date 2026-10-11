# Compatibility and validation

The APK supports Android 8.0/API 26 and later, without a Samsung-only installation restriction. The camera feature is optional at installation. Actual CameraX use requires a working camera provider, permission and supported use-case combination. No claim covers every Japanese handset or OEM stock-camera behavior.

| Device family | Potential stock-camera paths | Hardware status |
|---|---|---|
| Samsung / Galaxy Japanese firmware | CSC, AppOps; root/module depending on firmware | Unverified |
| Google Pixel Japanese configuration | Isolated enforced-audible group if CLI/mapping/state all qualify | Unverified |
| Sony Xperia | Capability detection; app/root/module or alternate camera | Unverified |
| Sharp AQUOS | Capability detection; app/root/module or alternate camera | Unverified |
| FCNT arrows | Capability detection; app/root/module or alternate camera | Unverified |
| Kyocera | Capability detection; app/root/module or alternate camera | Unverified |
| Other Android 8.0+ devices | Detected supported modes or in-app camera | Unverified |

CSC is available only on Samsung with an authorized, readable settings backend. An originally absent value is preserved as absence on restoration. Audio-group mode needs a unique platform mapping with only legacy enforced-audible stream 7, supported group CLI, zero minimum, and equal volumes across all saved routes. It dynamically resolves the group ID; it does not assume group 7. Missing, shared, ambiguous or differing-route groups disable modification. A route change also prevents unsafe restoration.

AppOps PLAY_AUDIO is experimental: it may silence other audio in the selected camera, and AOSP forced-camera sounds may ignore the restriction. Root mode requires both root and resetprop; only existing known properties with recognized values are changed. Services may cache properties, and a reboot can reset them. Magisk only affects discovered allowlisted external sound files.

Settings backups are scoped to Android user and firmware identity, kept locally, and excluded from cloud/device-transfer backup. PC backup additionally hashes the selected ADB serial with the firmware identity. Preserve the backup until restoration is verified. Restore before clearing data or uninstalling; firmware changes can invalidate old backups.

## Verified in this workspace

- Original 1.0 APK build completed as a baseline.
- 29 JVM behavior tests: state transactions, failed reads, absent originals, retained backups, rollback, user/firmware mismatch, audio parsing, allowlisted commands, bounded dual-stream process handling, targets, YUV strides and orientation.
- 7 Python behavior tests: CSC backup/restoration failure cases, missing originals, device/user mismatch, module path allowlist, archive exclusions and deterministic ZIP/hash behavior.
- Android unit tests, APK assembly and lint completed; lint has no errors. Capability-gated private audio API and dependency/update recommendations remain warnings.
- Module shell syntax and PC shell syntax pass; ffprobe recognizes the silent asset as mono 44.1kHz Vorbis, 0.200 seconds. Android APK signatures and all distribution checksums are checked before delivery.

Emulator results and exact signing/artifact provenance are recorded in [RELEASE.md](RELEASE.md). Emulator checks cannot establish acoustic success on physical Japanese devices, root/Magisk compatibility, or OEM camera feature parity.

## Device report template

Include manufacturer/model, Android/API, firmware, stock camera version, mode, configuration result, actual photo sound, restoration result, reboot behavior and relevant diagnostic output. The app's diagnostics omit IMEI, SIM identifiers and device serial. Review logs before sharing.
