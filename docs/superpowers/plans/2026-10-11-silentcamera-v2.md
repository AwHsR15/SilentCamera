# SilentCamera 2.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking. The user selected direct implementation, verification and submission; no additional design or publishing approval is required.

**Goal:** Deliver a multilingual Android shutter-control app with six distinct modes, conservative compatibility checks, reversible changes, an alternate camera, and verified distributable artifacts.

**Architecture:** Keep Compose and introduce a pure Kotlin policy/transaction layer, capability-aware mode controllers, a bounded Shizuku UserService executor and root executor, and persistent original-state backups. Keep camera capture independent from privileged operations. Publish only artifacts built from the final reviewed source.

**Tech Stack:** Kotlin 2.0, Android Gradle Plugin 8.5, JDK 17, Android SDK 35, Compose Material 3, Shizuku API 13, CameraX, JUnit 4, Python packaging utilities.

**Spec:** [Approved design](../specs/2026-10-11-silentcamera-v2-design.md). Approved by the user on 2026-10-11 with direct execution and submission authorized.

## Global Constraints

- Android 8.0+ (minSdk 26), package namespace `com.marvis.silentcamera`; signed preview application ID `com.marvis.silentcamera.preview` can coexist with v1.
- Six independent modes: Samsung CSC, enforced audio group, camera-package AppOps (experimental), root properties, Magisk sound module, in-app camera.
- Never claim acoustic success from a settings write; `null` or failed reads are never success.
- Detect actual capabilities, never hardcode an audio group or mute ring/media/call/alarm groups.
- Backup first, do not overwrite originals on reapply, verify apply and restore, retain backup on failure.
- Chinese/Japanese/English, system or selectable light/dark theme, no network permission or telemetry.
- No private signing keys committed; real-device results stay unverified until tested.

## Review Focus

- A failed read looks like an absent setting: transaction tests must reject failure before any write.
- Current-user changes and reinstall/device identity mismatches: backup tests reject cross-user/device restoration.
- Audio group has multiple output routes with different volumes: parser/controller tests refuse modification without exact restorable state.
- Cancellation, long stderr and dead Shizuku binder: process tests verify bounded dual-stream reading and cleanup; runtime errors become explicit results.
- Image-analysis buffers and activity rotation: frame tests verify stride-aware conversion and orientation; camera lifecycle handles disposal and failed saves.

## Task 1: Build baseline and pure policy/transaction core

**Files:** `app/build.gradle.kts`, `app/src/main/java/com/marvis/silentcamera/core/Models.kt`, `Policy.kt`, `Transactions.kt`; tests under `app/src/test/java/com/marvis/silentcamera/core/`.

**Interfaces:** `CommandResult(exitCode, stdout, stderr)`, `CommandExecutor.execute(args: List<String>): CommandResult`, `DeviceIdentity(fingerprint, userId)`, `OriginalState`, `BackupStore`, `SettingTransaction.apply()/restore(): OperationResult`, `AudioGroupParser.parse(dump): AudioGroup?`.

- [x] Prepare JDK/SDK and run original `assembleDebug`; record pre-existing failures.
- [x] Write regression tests for missing/failed reads, absent original restoration, reapply preservation, failed verification, unsafe package names, ambiguous/differing-route audio groups; run and observe failure.
- [x] Implement models, capability rules, conservative parser and transactional backup/restore.
- [x] Run all JVM tests and compile; commit the working core.

## Task 2: Privileged executors and six mode controllers

**Files:** `app/src/main/aidl/com/marvis/silentcamera/IShellService.aidl`, `shell/ShellService.kt`, `shell/Executors.kt`, `modes/ModeRepository.kt`, `modes/AndroidBackupStore.kt`, manifest and ViewModel.

**Interfaces:** executor from Task 1; `ModeRepository.detect(): DeviceReport`, `apply(mode, cameraPackage): OperationResult`, `restore(mode, cameraPackage): OperationResult`; repository owns identity-scoped backups.

- [x] Test command bounds/timeout behavior and transaction rollback before implementing those changes.
- [x] Replace hidden newProcess reflection with official Shizuku UserService, root backend and allowlisted commands; detect current user and actual audio CLI support.
- [x] Implement CSC, audio group, AppOps and root apply/restore; Magisk and camera remain separate actions with clear applicability and limitations.
- [x] Make ViewModel operations mutually exclusive, state changes on main thread, logs bounded, recover on binder loss; run tests/compile and commit.

## Task 3: Multilingual adaptive interface and camera

**Files:** `MainActivity.kt`, `MainViewModel.kt`, `ui/ModeScreen.kt`, `ui/DiagnosticsScreen.kt`, `ui/CameraScreen.kt`, `ui/Preferences.kt`, `Theme.kt`, `camera/FrameConverter.kt`, locale resources and camera/storage manifest declarations.

**Interfaces:** ViewModel exposes device report, selected mode/package, busy state, operation result, logs and verification; camera owns its Preview/ImageCapture/ImageAnalysis use cases.

- [x] Test frame conversion for padded strides, invalid input and rotation; observe failing tests.
- [x] Implement mode cards, detected requirements, apply/restore, manual acoustic confirmation, accessible navigation, settings, diagnostic copy/share and system camera launch.
- [x] Implement CameraX still and preview-frame save, front/back camera, supported flash/torch, focus/zoom/grid, orientation and MediaStore/legacy storage; dispose buffers/use cases safely.
- [x] Validate available runtime conditions and document remaining software-emulator limitations in RELEASE.md; verify locale parity and permission/lifecycle handling, run tests/lint/build and commit.

## Task 4: Magisk and portable delivery tools

**Files:** `packaging/magisk/`, `tools/adb_control.py`, `tools/adb-control.sh`, `tools/adb-control.bat`, `tools/package_release.py`, `tools/build-release.sh`, `.github/workflows/android.yml`, release docs, READMEs.

**Interfaces:** ZIP modules use valid silent OGG and discovered allowlisted paths; packaging script consumes the final APK and tracked source, emits deterministic source and delivery archives plus SHA256SUMS/manifest.

- [x] Write behavior tests for module path filtering and archive content/checksum/no-key rules; run failing tests.
- [x] Implement reversible module installation, PC tools with detection, user selection and backup/restore, external formal signing configuration, CI and reproducible packaging.
- [x] Build signed preview APK; check APK certificate, ZIP structure, shell/Python validation and artifact size; document signing and genuine compatibility limitations.
- [x] Commit verified code and review the full branch; fix material findings and re-run relevant checks.

## Task 5: Final verification and repository delivery

- [ ] Run `testDebugUnitTest lintDebug assembleDebug`, Python tool tests, shell syntax, APK signature verification and checksum verification. Use emulator if supported and report otherwise.
- [ ] Package APK, Magisk ZIP, delivery ZIP and source ZIP with exact source commit metadata; commit artifacts.
- [ ] Fast-forward main after confirming no concurrent upstream changes, or preserve changes with a normal merge; push without force.
- [ ] Read back remote commit and exact artifact paths; provide direct GitHub links, checks performed and real-device limits.
