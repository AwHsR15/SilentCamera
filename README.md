# SilentCamera - 三星日版快门静音工具

基于 Shizuku API 的三星日版手机相机快门音强制关闭工具。

## 功能
- 一键执行 `settings put system csc_pref_camera_forced_shuttersound_key 0`
- 实时显示 Shizuku 服务 / 授权 / 快门状态三类卡片
- 底层系统音频文件检测（单条 `sh -c` 探测，准确可靠）
- 写入前后对比验证
- 彩色分级运行日志，支持自动滚动、一键复制、清空
- 无需 ROOT，仅需 Shizuku

## 前置条件
1. 手机上安装 [Shizuku](https://shizuku.rikka.app/) 并启动（无线调试或 USB 均可）
2. 开启开发者选项 → USB 调试 / 无线调试

## 使用步骤
1. 用 Android Studio 打开本项目
2. 编译安装到手机
3. 打开 App → 点击「请求权限」→ 允许
4. 点击「一键静音」→ 等待完成

## 项目结构
```
SilentCamera/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/
        │   ├── themes.xml
        │   └── strings.xml
        └── java/com/marvis/silentcamera/
            ├── MainActivity.kt     # UI（Compose）
            ├── MainViewModel.kt    # 状态与业务逻辑
            ├── ShizukuShell.kt     # Shizuku shell 封装
            └── Theme.kt            # 配色与主题
```

## 自行编译
```bash
# 需要 Android SDK（platform-35、build-tools 35）与 JDK 17+
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 技术说明
- 使用 Shizuku API 13.x 获取 ADB 级 shell 权限
- `csc_pref_camera_forced_shuttersound_key` 是 CSC（消费者软件定制）层级的系统参数
- 该值写入 SettingsProvider，普通重启不丢失
- 系统 OTA 大版本更新后可能需要重新执行
- 仅修改这一项公开系统设置，不涉及 root、不改动系统分区、不绕过任何安全机制

## 免责声明
本工具仅供在**你本人拥有的设备**上、用于改善个人使用体验。日／韩等地区对相机快门声音有相关社会规范甚至法规，请在使用前了解并遵守你所在地区的法律法规，尊重他人隐私，切勿用于偷拍等非法用途。作者不对任何滥用行为负责。

## 许可证
[MIT](LICENSE)
