# 日版 Android 快门声音方案调研

调研日期：2026-10-11。对象：AwHsR15/SilentCamera，基线提交 `3d095b0`。

以下区分“第三方项目报告可用”“Android 源码中的机制”和“本项目实机验证”。本次没有连接日版手机，因此不把上游作者报告当作本项目的实机测试结果。

## 覆盖范围

不存在经公开证据确认、无需高级权限、能够关闭所有日本 Android 机型原厂相机声音的统一开关。品牌、地区固件、相机版本、音频策略和系统更新都会改变结果。让应用能安装、识别设备并提供适合的方案，可以作为泛用性目标；保证所有原厂相机都静音不能作为发布承诺。

Android 官方的 [`CameraInfo.canDisableShutterSound`](https://developer.android.com/reference/android/hardware/Camera.CameraInfo) 明确表示部分设备不能通过 Camera API 关闭快门声。[AOSP Camera.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/hardware/Camera.java) 在检测到强制相机声音时还会跳过 AppOps 导致的快门关闭操作。

## 可区分的方案类别

| 类别 | 机制和权限 | 已有依据 | 适用性与限制 | 集成方式 |
|---|---|---|---|---|
| 厂商设置 / Samsung CSC | Shizuku 或电脑 ADB 修改 `csc_pref_camera_forced_shuttersound_key` | [camsung](https://github.com/ericswpark/camsung)、[android-disable-shutter-sound](https://github.com/cloudtion/android-disable-shutter-sound) | Samsung One UI 专用；设置回读为 0 不等于实际无声；缺失值不表示已静音；OTA 可能重置 | 稳定模式，品牌限制、备份、精确回读与恢复 |
| 音频策略音量组 | Shizuku / ADB 通过 `cmd audio` 操作 enforced audible 音量组 | [SilentPixel](https://github.com/Morendais/SilentPixel)；上游报告测试了 Pixel 10 Pro XL | Android 版本和 OEM 是否提供命令、音量组映射都不同；上游示例同时修改组 7、2，不能照搬其“无副作用”描述 | 有条件模式；从当前设备诊断输出识别目标；不能可靠解析则禁用；备份 mute 与各输出设备音量 |
| 相机应用音频 / AppOps | Shizuku / ADB 对指定相机包修改 `PLAY_AUDIO` | [ShizuTools MixedAudioActivity](https://github.com/legendsayantan/ShizuTools/blob/master/app/src/main/java/com/legendsayantan/adbtools/MixedAudioActivity.kt)、[AOSP Camera.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/hardware/Camera.java) | 可能影响该应用的其他播放；AOSP 的强制快门逻辑可以忽略此限制，不能视为通用强制静音 | 实验模式；只选已发现的相机应用、保存原 AppOps、回读、恢复；禁用全应用批量处理 |
| 系统属性 / root | 获得 root 后用 resetprop 改相机声音相关属性 | [AndroPlus camerasound](https://github.com/AndroPlus-org/magisk-module-camerasound)、[Magisk 内部说明](https://topjohnwu.github.io/Magisk/details.html) | 不同固件读取的属性不同；系统服务可能缓存；只读 `ro.*` 不能通过普通 ADB setprop 改写；部分场景需重启 | 高级模式；明确要求 root + resetprop；备份已有属性、回读；不宣称免 root 可改只读属性 |
| 系统音效替换 / Magisk | 通过模块覆盖已存在的快门音文件或资源，不直接删除系统文件 | [AndroPlus camerasound](https://github.com/AndroPlus-org/magisk-module-camerasound)、[magisk-custom-sounds](https://github.com/laddge/magisk-custom-sounds)、[Magisk 模块文档](https://topjohnwu.github.io/Magisk/guides.html) | 对相机 APK 内嵌声音、HAL 播放无保证；分区位置及文件名各异；安装需 Magisk 和重启 | 独立模块模式与 ZIP；检测实际路径，仅覆盖匹配的快门文件；使用有效静音音频；卸载模块恢复 |
| 应用内替代拍照 | 使用 CameraX / Camera2，不播放应用快门音；必要时保存预览帧 | [CameraX 拍照](https://developer.android.com/media/camera/camerax/take-photo)、[CameraX 图像分析](https://developer.android.com/media/camera/camerax/analyze) | 不会修改其他相机的声音；高画质 still capture 仍可能受厂商控制；预览帧质量低于原厂拍照，不保证 HDR、多摄和夜景 | 通用候选模式；正常预览、明确拍摄操作、相册保存；区分高画质拍照与预览帧保存，拒绝保证所有 HAL 无声 |
| APK Hook / Xposed / LSPosed | 在指定相机版本中拦截声音调用或替换资源 | [Firefds Kit S](https://github.com/Xposed-Modules-Repo/sb.firefds.s.firefdskit)、[GCam Custom Shutter](https://github.com/vitalto/gcam-shutter-sound) | 强绑定品牌、相机版本、方法名与框架；声音可能由 HAL / 系统播放；不能给所有相机共用一个 hook | 在“其他方案”展示来源、适用版本和安装要求；本版本不把仅有教程的内容伪装成可执行通用模式 |

## 不适合作为通用开关的思路

- 普通静音、耳机、切换 SIM / 地区与飞行模式：效果依赖设备和当前固件，不能当作固定能力。
- `CameraDevice.setCameraAudioRestriction`：官方用途是拍摄期间限制铃声、闹钟、通知等干扰，不是原厂快门静音 API；不能据此宣称关闭其他相机的声音。[官方 API](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#setCameraAudioRestriction(int))
- 删除 `/system` 的声音文件：已有 Magisk 覆盖方式可以恢复，直接改系统分区不作为应用操作。
- 把所有音量调为 0：会影响媒体、通话、闹钟等，且强制音流可能拒绝修改。

## 当前项目实际问题

1. `readCscValue()` 把命令失败转换成 `null`，无法区分缺失与错误。
2. `executeSilence()` 在回读为 `null` 时仍可能报告成功。
3. 对非 Samsung 机型缺少能力判断。
4. 没有原值备份或恢复操作。
5. IO 协程内直接修改 Compose 状态列表；后续应在主线程统一更新状态。
6. shell 先完整读取 stdout 再读取 stderr，没有超时，存在阻塞风险。
7. 用 Shizuku 隐藏 newProcess 反射作为唯一执行接口；应采用官方支持的 UserService 承载受限命令执行。
8. Android 8.0+ 的最低版本合理，但界面只有中文、固定深色，README 的三语不等于应用三语。

## 证据等级

各模式至少记录命令是否执行、状态是否回读、是否由用户实拍确认。仅当用户实拍确认后显示“此设备上已确认无声”，否则显示“配置已应用，请实拍确认”。任何异常、缺失值、解析失败和权限不足，都不显示静音成功。

发布兼容表应按制造商、机型、Android 版本、固件、相机包与版本、模式和实拍结果记录。Samsung、Pixel、Sony Xperia、Sharp AQUOS、Fujitsu / arrows、Kyocera 等只是待验证设备类别，不等于已通过测试。
