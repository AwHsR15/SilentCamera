# SilentCamera · 三星日版相机快门静音工具

**简体中文** · [日本語](README.ja.md) · [English](README.en.md)

> 一个用 Shizuku 关闭三星日／韩版手机「强制相机快门音」的小工具。无需 Root，一键搞定。

三星在日版、韩版机型上把相机快门声写成了**强制开启**且 App 内无法关闭。本工具通过
[Shizuku](https://shizuku.rikka.app/) 拿到 ADB 级权限，写入一个公开的 CSC 系统设置项
`csc_pref_camera_forced_shuttersound_key = 0`，让快门声重新跟随系统音量（即可静音）。

<p align="center">
  <em>Jetpack Compose · Material 3 · Shizuku API 13 · Kotlin</em>
</p>

---

## ✨ 功能

- **一键静音** —— 自动执行 `settings put system csc_pref_camera_forced_shuttersound_key 0`
- **三状态实时卡片** —— Shizuku 服务 / 授权状态 / 当前快门状态一目了然
- **写入前后校验** —— 修改后立即回读，确认是否真正生效
- **底层音频文件探测** —— 检测 `/system/media/audio/ui/` 下的快门音频（只读，不删改）
- **分级彩色日志** —— 步骤 / 成功 / 警告 / 错误分色显示，支持自动滚动、一键复制、清空
- **无需 Root** —— 仅依赖 Shizuku（无线调试或 USB 均可激活）

## 📦 直接安装

下载 [`release/SilentCamera-v1.0-debug.apk`](release/SilentCamera-v1.0-debug.apk) 传到手机安装即可。

## 🚀 使用流程

```
启动 Shizuku ──▶ 打开 App ──▶ 点「请求权限」并允许 ──▶ 点「一键静音」──▶ 完成
```

1. 手机安装并启动 [Shizuku](https://shizuku.rikka.app/)（开发者选项里开启无线调试 / USB 调试）
2. 打开 SilentCamera
3. 点「**请求权限**」→ 在 Shizuku 弹窗中允许
4. 点「**一键静音**」，等待日志显示 `[✓] 快门强制音已关闭！`

## 🔧 自行编译

```bash
# 需要 Android SDK（platform-35、build-tools 35）与 JDK 17+
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 🧠 技术细节

| 主题 | 说明 |
|---|---|
| 关键设置项 | `csc_pref_camera_forced_shuttersound_key`，CSC（消费者软件定制）层级的系统参数 |
| 权限来源 | Shizuku 提供的 ADB 级 shell，通过反射调用 `Shizuku.newProcess()`（该方法在 13.x API 中为 private） |
| Shell 语义 | 需要 `&&`、通配符时统一走 `sh -c`，避免参数被当成字面量 |
| 持久性 | 值写入 `SettingsProvider`，普通重启不丢；系统**大版本 OTA 后可能被重置**，需重新执行 |
| 架构 | `MainActivity`（Compose UI）/ `MainViewModel`（状态与逻辑）/ `ShizukuShell`（命令封装）/ `Theme`（配色） |
| 边界 | 只修改这一项公开系统设置，**不涉及 Root、不改系统分区、不绕过任何安全机制** |

```
SilentCamera/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── res/values/            # 主题、颜色、字符串
│   └── java/com/marvis/silentcamera/
│       ├── MainActivity.kt    # UI（Jetpack Compose）
│       ├── MainViewModel.kt   # 状态与业务逻辑
│       ├── ShizukuShell.kt    # Shizuku shell 封装
│       └── Theme.kt           # 配色与主题
└── release/                   # 编译好的 APK
```

## ⚠️ 免责声明

本工具仅供在**你本人拥有的设备**上、用于改善个人使用体验。日／韩等地区对相机快门声音有相关社会规范甚至法规，
请在使用前了解并遵守你所在地区的法律法规，尊重他人隐私，**切勿用于偷拍等任何非法用途**。作者不对任何滥用行为负责。

## 📄 许可证

[MIT](LICENSE) © 2026 AwHsR15
