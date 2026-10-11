# Magisk shutter overlay / 快门音模块 / シャッター音モジュール

## Installation / 安装 / インストール

Install `SilentCamera-v2.1-magisk.zip` from Magisk 20.4+ → Modules → Install from storage on a booted rooted device, then reboot. Test a real photo. This module is optional; the APK and in-app camera work without it.

在已启动且获得 Root 的设备上，Magisk 20.4+ → 模块 → 从本地安装，选择 ZIP，重启后实拍验证。模块是可选方案，APK 和应用内拍照无需它。

起動済み Root 端末で Magisk 20.4以降 → モジュール → ストレージからインストール。ZIP を選び、再起動して実写確認します。APK とアプリ内カメラにこのモジュールは不要です。

## Behavior and recovery / 原理与恢复 / 動作と復元

Only existing allowlisted `.ogg` shutter files in system/vendor/product/system_ext audio UI directories are overlaid. The module contains a valid 0.2-second silent Vorbis file. It does not delete system files, change SELinux, replace camera APKs, mute whole directories, or run a boot-time property daemon. No matching files causes installation to abort.

仅对 system/vendor/product/system_ext 中现有的白名单快门 OGG 做 systemless 覆盖。音频是有效的 0.2 秒静音 Vorbis；不删除系统文件，不修改 SELinux、不替换相机 APK。没有匹配文件就中止安装。厂商 APK 内嵌音效、HAL 声音或未列入白名单的资源可能不受影响。

既存の許可リスト内のシャッター OGG のみ systemless で置き換えます。元のシステムファイルは削除しません。対象がない場合はインストールを中止します。APK 内蔵・HAL・リスト外の音には効かない可能性があります。

**Restore:** disable or remove `SilentCamera Shutter Audio` in Magisk, then reboot. Original partition contents remain intact. If a phone cannot boot normally, use that Magisk version's documented safe-mode/module-removal procedure; do not delete random system files.

**恢复：** 在 Magisk 禁用或移除 `SilentCamera Shutter Audio`，再重启；原分区文件保持完整。如无法正常启动，使用对应 Magisk 版本的官方安全模式／模块移除流程。

**復元：** Magisk でモジュールを無効化または削除し、再起動します。正常起動できない場合は使用中の Magisk の公式セーフモード／削除手順を参照してください。

This module is structurally and script-tested, but has not been installed on Japanese OEM hardware. Other sound modules can override the same paths; disable conflicts before testing. It will not make shutter behavior universally controllable.

Sources: [Magisk guides](https://topjohnwu.github.io/Magisk/guides.html), [Magisk module installer](https://github.com/topjohnwu/Magisk/blob/master/scripts/module_installer.sh). The shipped bootstrap and path policy are original project code under MIT; the installed Magisk runtime is not bundled.
