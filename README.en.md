# SilentCamera · Shutter-Sound Silencer for Japanese Samsung Phones

[简体中文](README.md) · [日本語](README.ja.md) · **English**

> A tiny tool that uses Shizuku to turn off the *forced* camera shutter sound on Japanese/Korean Samsung phones. No root, one tap.

On their Japanese and Korean models, Samsung **forces** the camera shutter sound on and offers no in-app way to disable it.
This tool uses [Shizuku](https://shizuku.rikka.app/) to gain ADB-level access and writes the public CSC system setting
`csc_pref_camera_forced_shuttersound_key = 0`, so the shutter sound follows the system volume again (i.e. it can be silenced).

<p align="center">
  <em>Jetpack Compose · Material 3 · Shizuku API 13 · Kotlin</em>
</p>

---

## ✨ Features

- **One-tap silence** — runs `settings put system csc_pref_camera_forced_shuttersound_key 0` for you
- **Three live status cards** — Shizuku service / permission / current shutter state at a glance
- **Before/after verification** — reads the value back immediately to confirm it actually took effect
- **Audio-file probe** — detects shutter audio under `/system/media/audio/ui/` (read-only, never deletes)
- **Color-coded log** — step / success / warning / error colors, with auto-scroll, copy and clear
- **No root** — Shizuku only (activate via wireless debugging or USB)

## 📦 Install directly

Download [`release/SilentCamera-v1.0-debug.apk`](release/SilentCamera-v1.0-debug.apk) and install it on your phone.

## 🚀 How to use

```
Start Shizuku ──▶ Open the app ──▶ "Request permission" & allow ──▶ "One-tap silence" ──▶ Done
```

1. Install and start [Shizuku](https://shizuku.rikka.app/) (enable wireless / USB debugging in Developer options)
2. Open SilentCamera
3. Tap **Request permission** → allow it in the Shizuku dialog
4. Tap **One-tap silence**; the log should show `[✓] 快门强制音已关闭！` (shutter sound disabled)

## 🔧 Build it yourself

```bash
# Requires Android SDK (platform-35, build-tools 35) and JDK 17+
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## 🧠 Technical notes

| Topic | Detail |
|---|---|
| Setting key | `csc_pref_camera_forced_shuttersound_key`, a CSC (region-customization) system setting |
| Permission | ADB-level shell from Shizuku; `Shizuku.newProcess()` is invoked via reflection (it's private in the 13.x API) |
| Shell semantics | Anything needing `&&` or globs goes through `sh -c`, so args aren't treated literally |
| Persistence | Stored in `SettingsProvider`, survives normal reboots; **a major OTA may reset it**, just re-run |
| Architecture | `MainActivity` (Compose UI) / `MainViewModel` (state & logic) / `ShizukuShell` (commands) / `Theme` (colors) |
| Scope | Changes only this one public system setting — **no root, no system-partition edits, no security bypass** |

## ⚠️ Disclaimer

This tool is intended solely for improving your personal experience on **a device you own**. Some regions (e.g. Japan, Korea)
have social norms or laws regarding camera shutter sounds. Understand and comply with the laws in your area, respect others'
privacy, and **never use it for covert photography or any unlawful purpose**. The author is not responsible for any misuse.

## 📄 License

[MIT](LICENSE) © 2026 AwHsR15
