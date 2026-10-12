# Motorola razr 50 Ultra / Android 16 investigation

Date: 2026-10-12. Target: motorola razr 50 ultra, Android 16 / API 36, Android user 0, stock camera package `com.motorola.camera5`, no root. This is a user device report plus source research; the phone is not attached to the workspace.

## What the supplied logs establish

- Shizuku is running and authorized.
- AppOps reads succeeded, `PLAY_AUDIO` changed to `ignore`, and a restoration changed it to `default`.
- The user reports that stock-camera photographs still make sound. Therefore, configuration write success is not acoustic success on this device. Repeating the same AppOps change is not a new solution.
- `cmd audio help` returned exit 0 with empty output repeatedly. The current app therefore did not detect the required `adj-group-volume` CLI. This does not establish that every audio Binder API is absent or that the stream/group mapping is isolated.
- Samsung CSC is not a Motorola setting. Root properties and the Magisk overlay require permissions absent on this phone.
- The specific firmware/channel, camera version and current SIM country were not supplied. Do not generalize the report to every Motorola or every razr 50 Ultra firmware.

## Relevant primary sources and candidate paths

### Motorola's regional shutter setting

[Motorola's official camera help](https://help.motorola.com/hc/apps/camera/c90h/en-us/CGT1809261726.html) says that the shutter-sound toggle is unavailable in restricted regions. A [firsthand razr 50 Ultra review](https://kazudot.hatenablog.com/entry/2025/05/06/214236) reports shutter and screenshot sounds when using a Japanese SIM. This supports investigating the regional policy, but neither source provides a verified Android 16 bypass for this device.

### Foreign SIM/eSIM: Motorola-specific, conditional

[A firsthand edge 40 experiment](https://note.com/yuurin1567/n/ne837d8d7b9fb) reports that recognizing an overseas eSIM/SIM exposed the original camera's sound switch while retaining the Japanese SIM. Its later notes report that certain SoftBank carrier variants, including edge 50s Pro, do not support this path. Restarting with the overseas eSIM disabled could re-enable the restriction; Android 14 also changed screenshot behavior.

The confirmed test device was edge 40, not this razr / Android 16 combination. Do not present the method as guaranteed, install/delete SIM profiles automatically, recommend a paid plan before determining whether this firmware is eligible, or treat changing the UI language as changing the SIM identity. If the current SIM is already non-Japanese, adding another overseas profile is not an established solution.

### Temporary device-wide mute: separate from shutter-only control

The developer's [current Mute Camera trial listing](https://play.google.com/store/apps/details?id=jp.snowlife01.android.trialplusmutecamera&hl=en), updated 2026-10-07 when researched, advertises a rebuilt muting method, Android 8–17 support and a 48-hour full-feature trial. It explicitly says some devices cannot be muted and warns that music, notifications and alarms are muted too. Turn muting off before uninstalling. These are developer claims, not a confirmed razr 50 Ultra / Android 16 test. A positive test would establish a useful feasibility result, not reveal the closed-source implementation or prove that it uses any particular Binder API.

A distinct implementation hypothesis is Shizuku direct audio Binder access instead of requiring an OEM `cmd audio` front end. Android 16's [AudioService](https://android.googlesource.com/platform/frameworks/base/+/android16-release/services/core/java/com/android/server/audio/AudioService.java) exposes `isMasterMute()` and a permission-enforced `setMasterMute(...)`; [Shell's manifest](https://android.googlesource.com/platform/frameworks/base/+/android16-release/packages/Shell/AndroidManifest.xml) declares `MODIFY_AUDIO_ROUTING`. This makes a shell-authorized probe plausible in AOSP. Motorola may change permissions or behavior. A working CLI is not required by the Binder API, but this is not proof of acoustic effectiveness on the user's phone.

Any retained implementation must be an explicitly named temporary device-wide mute mode, with original-state backup, successful read-back, automatic expiry/restoration, immediate restore, and failure handling. It must not alter stream-volume indices or claim to preserve audible alarms/ringtones while the master mute is active. That is a different side-effect boundary from the previously approved shutter-only modes.

### Root/framework interception

[DisableCameraSound](https://github.com/auag0/DisableCameraSound) intercepts `AudioService.readCameraSoundForced()` and returns false. Its author confirms only AOSP testing and requires a libxposed 101 environment. It is not a verified Motorola-specific module and is not an available route for the user's current unrooted setup.

## Unsupported claims excluded

- No verified primary-source Motorola reader was found for the commonly repeated `settings put system camera_sound 0` advice. Writing and reading an arbitrary Settings key does not prove that stock Camera5 uses it.
- AppOps can be bypassed in Android's forced-camera-sound handling; the [Android 16 Camera source](https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/android/hardware/Camera.java) is a relevant example. It does not prove which exact branch Motorola Camera5 uses.
- No claim of a universal no-root, stock-camera, shutter-only fix is supported by this investigation.

## Next evidence needed

Determine the sales/firmware channel and current SIM country. Then choose a conditional regional-switch test or temporary-mute feasibility test. Before adding a direct Binder audio mode, collect read-only audio policy/forced-state and API capability diagnostics; empty CLI help alone is insufficient. Confirm every sound claim with an actual stock-camera photograph and verify restoration.
