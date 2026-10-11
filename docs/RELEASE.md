# SilentCamera 2.0 preview delivery

This delivery adds six independent approaches, conservative capability detection, original-state recovery, Chinese/Japanese/English themes and UI, and an in-app CameraX camera with preview-frame saving. Research and the approved design are included in `docs/`.

## Files

| File in release/ | Contents |
|---|---|
| SilentCamera-v2.0-preview.apk | Signed installable preview, application ID com.marvis.silentcamera.preview, API 26+ |
| SilentCamera-v2.0-magisk.zip | Optional systemless shutter OGG module |
| SilentCamera-v2.0-bundle.zip | APK, module, Python/Windows/Unix ADB tools, three-language guides and checksums |
| SilentCamera-v2.0-source.zip | Tracked source and build tools; excludes release files, caches and private keys |
| manifest.json | Exact source commit, APK/module/source byte sizes and SHA-256 hashes |
| SHA256SUMS | APK, module, bundle, source and manifest integrity |

The source commit is embedded in APK BuildConfig and the source ZIP's `SOURCE_COMMIT.txt`. Packaging rejects an APK without the expected commit. The artifact-only commit follows the source commit and is intentionally separate; use the manifest source commit for reproduction. Archives have sorted paths and fixed timestamps; Android APKs are not claimed to be bit-for-bit reproducible across signing keys or toolchains.

## Signing and rebuilding

The v1 signing private key is absent, so the new preview uses a separate package ID and a local Android debug certificate. Keep v1 installed until you have verified the new app. Android enforces signature equality for updates, including future preview updates. A newly generated debug key on another machine cannot update this exact preview package without uninstalling it; restore system changes and export photos before uninstalling. Keys are never committed.

For an externally signed production build, set `SILENTCAMERA_KEYSTORE`, `SILENTCAMERA_STORE_PASSWORD`, `SILENTCAMERA_KEY_ALIAS` and `SILENTCAMERA_KEY_PASSWORD`, then run `./gradlew assembleRelease`. The production ID remains `com.marvis.silentcamera`. Only the original v1 signing key can produce an APK that updates v1. Production signing is separate from the preview packaging script.

From a clean committed checkout with JDK 17, SDK 35 and Python 3.10+, run `bash tools/build-release.sh`. CI uses the same test/lint/build/package pipeline and uploads artifacts, with no signing secrets required. CI debug certificates may differ from the committed preview's certificate.

Verify downloaded files in the `release/` directory with `sha256sum -c SHA256SUMS` (Linux) or `shasum -a 256 -c SHA256SUMS` (macOS). On Windows, use `Get-FileHash -Algorithm SHA256 <file>` and compare with SHA256SUMS. Hashes prove file integrity against this manifest, not Japanese-device compatibility.

## Runtime verification

Runtime checks are being recorded before the final artifact commit. Japanese OEM phones, root/resetprop modification and Magisk installation remain unverified in this environment. See [COMPATIBILITY.md](COMPATIBILITY.md) for the capability rules and test coverage.
