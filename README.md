# SilentCamera 2.0

**简体中文** · [日本語](README.ja.md) · [English](README.en.md)

面向日版 Android 的相机声音工具：六个独立模式、设备能力检测、原状态恢复，以及无需 Shizuku 的应用内拍照。支持 Android 8.0+，中／日／英界面和浅／深色主题。应用不请求联网权限，不含遥测。

**原厂相机能否静音取决于机型、系统和相机实现，无法保证所有日本安卓机都成功。** 请先检测能力，再选择方案；配置回读成功后仍需实拍确认。本版本尚未完成日本品牌实体手机测试。

## 下载

- [新版安装包：SilentCamera-v2.0-preview.apk](release/SilentCamera-v2.0-preview.apk)
- [完整封装：APK、Magisk、电脑工具、说明](release/SilentCamera-v2.0-bundle.zip)
- [独立 Magisk 模块](release/SilentCamera-v2.0-magisk.zip)
- [源码封装](release/SilentCamera-v2.0-source.zip)
- [SHA-256 校验](release/SHA256SUMS) · [产物与源提交清单](release/manifest.json)

预览包已签名，包名为 `com.marvis.silentcamera.preview`，可与旧版并行安装。它使用预览签名，不能直接覆盖旧版；正式签名需从外部提供私钥。保留 [1.0 安装包](release/SilentCamera-v1.0-debug.apk) 供回退。

## 六个模式

| 模式 | 条件与用途 |
|---|---|
| Samsung CSC | 三星固件 + Shizuku；关闭强制设置，让相机声音跟随系统设置 |
| 强制提示音音量组 | Shizuku + 支持音量组 CLI；动态识别独立 enforced-audible 组，无法精确恢复时禁用 |
| 相机应用音频控制 | Shizuku；仅针对所选相机包的 PLAY_AUDIO，实验功能，可能影响该相机其他音频 |
| Root 声音属性 | Root + resetprop；只修改检测到的已知相机属性，实验功能 |
| Magisk 音效替换 | 独立模块；仅覆盖存在的白名单快门 OGG，安装／禁用后需重启 |
| 应用内拍照 | 无需 Shizuku／Root；CameraX 拍照，另有保存预览帧的替代方式 |

没有硬编码并静音铃声、媒体、通话或闹钟组。LSPosed 等机型相关 hook 的调研与外部链接见 [方案调研](docs/research/shutter-muting-approaches.md)，未伪装为通用执行模式。

## 使用

1. 安装 APK，进入「模式」查看设备和适用方案。三星优先 CSC，其他设备按检测结果选择，也可直接进入「拍照」。
2. 前三个系统设置模式需要安装并启动 [Shizuku](https://shizuku.rikka.app/guide/setup/)，然后在应用中授权。Android 11+ 通常支持无线调试；Android 8–10 可以从电脑通过 USB 启动。
3. 选择模式，必要时选择相机应用，再点「应用」。应用会先保存原状态，写入后回读。重复应用不会覆盖首次备份。
4. 启动原厂相机实拍，再记录「无声」或「仍有声音」。「配置已应用」不代表已确认声音消失。
5. 结束测试或换方案前点「恢复原状态」。**卸载或清除应用数据之前先恢复**，否则本地恢复备份会丢失。固件／Android 用户变化时拒绝使用不匹配的备份。

应用内相机提供前后摄、对焦、缩放、网格、设备支持的闪光／补光和相册保存。高画质拍照不主动播放应用快门声；厂商 HAL 仍可能发声。预览帧保存不调用普通拍照请求，质量和分辨率受预览限制。Android 8–9 保存需要额外存储权限。

## 电脑工具与构建

解压完整封装，安装官方 [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools)，安装 Python 3.10+，开启 USB 调试并授权电脑：

```sh
python3 tools/adb_control.py detect
python3 tools/adb_control.py install
# 仅 Samsung；原状态保存到本地 JSON，不要丢失
python3 tools/adb_control.py apply-csc --backup phone-original.json
python3 tools/adb_control.py restore-csc --backup phone-original.json
```

Windows 使用 `tools\adb-control.bat`，Linux／macOS 使用 `tools/adb-control.sh`，参数相同。多设备用 `--serial`；默认操作前台 Android 用户，可用 `--user` 指定用户。在源码仓库安装时加 `--apk release/SilentCamera-v2.0-preview.apk`。

需要 JDK 17、Android SDK 35 和 Python 3.10+。完整封装仅能从已提交的 Git 源码构建：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 -m unittest discover -s tools -p 'test_*.py'
bash tools/build-release.sh
```

源码 ZIP 可直接编译 APK；如需重建带 Git 提交溯源的封装，请克隆仓库并检出 `SOURCE_COMMIT.txt` 标明的提交。GitHub Actions 自动验证并保存构建产物，不自动覆盖仓库内已交付文件。

[兼容性与验证](docs/COMPATIBILITY.md) · [Magisk 安装／恢复](docs/MAGISK.md) · [发布、签名与校验](docs/RELEASE.md)

MIT License。感谢 Shizuku、AndroidX CameraX 和 Jetpack Compose 项目。
