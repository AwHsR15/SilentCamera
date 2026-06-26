# SilentCamera · Galaxy 日本版 シャッター音オフツール

[简体中文](README.md) · **日本語** · [English](README.en.md)

> Shizuku を使って Samsung 日本／韓国版端末の「強制カメラシャッター音」をオフにする小さなツール。Root 不要、ワンタップ。

Samsung は日本版・韓国版の端末でカメラのシャッター音を**強制オン**にしており、カメラアプリ内では消せません。
本ツールは [Shizuku](https://shizuku.rikka.app/) で ADB レベルの権限を取得し、公開された CSC システム設定
`csc_pref_camera_forced_shuttersound_key = 0` を書き込むことで、シャッター音を再びシステム音量に追従させます（=消音可能に）。

<p align="center">
  <em>Jetpack Compose · Material 3 · Shizuku API 13 · Kotlin</em>
</p>

---

## ✨ 機能

- **ワンタップ消音** —— `settings put system csc_pref_camera_forced_shuttersound_key 0` を自動実行
- **3 つのステータスカード** —— Shizuku サービス / 権限 / 現在のシャッター状態を一目で確認
- **書き込み前後の検証** —— 変更直後に読み戻して実際に反映されたか確認
- **音声ファイル検出** —— `/system/media/audio/ui/` 内のシャッター音を検出（読み取りのみ、削除・変更なし）
- **色分けログ** —— ステップ／成功／警告／エラーを色分け表示、自動スクロール・コピー・クリア対応
- **Root 不要** —— Shizuku のみ（ワイヤレスデバッグまたは USB で起動）

## 📦 すぐにインストール

[`release/SilentCamera-v1.0-debug.apk`](release/SilentCamera-v1.0-debug.apk) をダウンロードして端末に入れるだけ。

## 🚀 使い方

```
Shizuku 起動 ──▶ アプリを開く ──▶「権限を要求」して許可 ──▶「ワンタップ消音」──▶ 完了
```

1. [Shizuku](https://shizuku.rikka.app/) をインストールして起動（開発者向けオプションでワイヤレス／USB デバッグを有効化）
2. SilentCamera を開く
3. 「**権限を要求**」→ Shizuku のダイアログで許可
4. 「**ワンタップ消音**」をタップ。ログに `[✓] 快门强制音已关闭！` が出れば成功

## 🔧 自分でビルド

```bash
# Android SDK（platform-35, build-tools 35）と JDK 17+ が必要
./gradlew assembleDebug
# 出力: app/build/outputs/apk/debug/app-debug.apk
```

## 🧠 技術メモ

| 項目 | 内容 |
|---|---|
| 設定キー | `csc_pref_camera_forced_shuttersound_key`（CSC = 地域別カスタマイズ層のシステム設定） |
| 権限 | Shizuku の ADB シェル。`Shizuku.newProcess()` をリフレクションで呼び出し（13.x API では private） |
| シェル | `&&` やワイルドカードが必要な処理は `sh -c` 経由。引数がリテラル扱いになるのを防ぐ |
| 永続性 | 値は `SettingsProvider` に保存され通常の再起動では消えない。**大型 OTA 後はリセットされる場合あり**、再実行が必要 |
| 構成 | `MainActivity`（Compose UI）/ `MainViewModel`（状態・ロジック）/ `ShizukuShell`（コマンド）/ `Theme`（配色） |
| 範囲 | この公開システム設定 1 項目のみを変更。**Root なし・システムパーティション改変なし・セキュリティ回避なし** |

## ⚠️ 免責事項

本ツールは**自身が所有する端末**で、個人的な利用体験の改善のみを目的としています。日本・韓国などではシャッター音に関する
社会的規範や法規制があります。お住まいの地域の法律を理解・遵守し、他者のプライバシーを尊重し、**盗撮など違法な用途には絶対に使用しないでください**。
作者は一切の濫用について責任を負いません。

## 📄 ライセンス

[MIT](LICENSE) © 2026 AwHsR15
