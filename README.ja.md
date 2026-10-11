# SilentCamera 2.0

[简体中文](README.md) · **日本語** · [English](README.en.md)

日本向け Android のカメラ音ツール。6つの独立モード、端末能力の検出、元の状態への復元、アプリ内カメラを搭載。Android 8.0以降、中国語／日本語／英語とライト／ダークテーマに対応。ネットワーク権限・テレメトリーはありません。

**純正カメラの消音可否は機種、ファームウェア、カメラの実装に依存します。全機種での成功は保証できません。日本向け実機では未検証です。** 設定の読み戻しと実際の撮影音の確認は別です。

## ダウンロード

- [署名済みプレビュー APK](release/SilentCamera-v2.0-preview.apk)
- [全部入り ZIP：APK、Magisk、PC ツール、説明](release/SilentCamera-v2.0-bundle.zip)
- [Magisk モジュール](release/SilentCamera-v2.0-magisk.zip)
- [ソース ZIP](release/SilentCamera-v2.0-source.zip)
- [SHA256SUMS](release/SHA256SUMS) · [ソース／成果物の記録](release/manifest.json)

プレビュー版のパッケージ名は `com.marvis.silentcamera.preview` です。旧版と共存できます。署名が異なるため旧版へ上書きできません。正式署名の秘密鍵は外部から設定します。[v1 APK](release/SilentCamera-v1.0-debug.apk) も残しています。

## 6つのモード

| モード | 条件・用途 |
|---|---|
| Samsung CSC | Samsung ファームウェア + Shizuku。音をシステム設定に従わせます |
| 強制通知音グループ | Shizuku + 対応 CLI。独立したグループを動的に特定し、正確に復元できない場合は無効化 |
| カメラアプリの音声制御 | Shizuku。選択したカメラの PLAY_AUDIO を制御する実験機能。他の音声にも影響する可能性あり |
| Root の音声プロパティ | Root + resetprop。検出した既知のカメラプロパティのみ変更する実験機能 |
| Magisk 音声置換 | 既存の許可リスト内のシャッター OGG を上書きする独立モジュール。再起動が必要 |
| アプリ内カメラ | Shizuku／Root 不要。CameraX 撮影とプレビューフレーム保存 |

着信・メディア・通話・アラームのグループを固定番号で消音しません。機種依存の LSPosed hook は外部調査資料として紹介し、汎用実行モードにはしていません。

## 使用と復元

1. APK をインストールし、検出結果を確認します。最初の3つのシステム設定モードには [Shizuku](https://shizuku.rikka.app/guide/setup/) の起動と許可が必要です。通常、無線起動は Android 11以降、旧版は USB ADB を使えます。
2. モードと必要なカメラアプリを選び、適用します。変更前に元の状態を保存し、変更後に読み戻します。再適用で最初のバックアップを上書きしません。
3. 純正カメラで実写し、無音／音ありを記録します。「設定を適用済み」は消音成功を意味しません。
4. 別の方法へ切り替える前、データ消去・アンインストール前に復元してください。バックアップは端末内に保存され、Android の自動バックアップには含まれません。ファームウェア・ユーザーが異なる場合は復元を拒否します。

アプリ内カメラは前後切替、フォーカス、ズーム、グリッド、対応端末のフラッシュ／ライトに対応します。通常撮影でアプリはシャッター音を再生しませんが、HAL が鳴る場合があります。フレーム保存は通常の静止画要求を使わず、画質はプレビューに制限されます。Android 8–9 の保存にはストレージ権限が必要です。

## PC ツールとビルド

ZIP を展開し、公式 [Platform Tools](https://developer.android.com/tools/releases/platform-tools) と Python 3.10以降をインストールして USB デバッグを許可してください。

```sh
python3 tools/adb_control.py detect
python3 tools/adb_control.py install
# Samsung のみ。バックアップ JSON を保管してください
python3 tools/adb_control.py apply-csc --backup phone-original.json
python3 tools/adb_control.py restore-csc --backup phone-original.json
```

Linux／macOS は `tools/adb-control.sh`、Windows は `tools\adb-control.bat`。複数端末は `--serial`、ユーザー指定は `--user` を使います。リポジトリ内からのインストールは `--apk release/SilentCamera-v2.0-preview.apk` を追加します。

JDK 17、SDK 35、Python 3.10以降でビルドします。

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 -m unittest discover -s tools -p 'test_*.py'
bash tools/build-release.sh
```

封装にはコミット済みでクリーンな Git 作業ツリーが必要です。ソース ZIP から APK をビルドできます。コミット情報付き ZIP の再作成にはリポジトリをクローンし、`SOURCE_COMMIT.txt` のコミットをチェックアウトしてください。GitHub Actions は検証と成果物保存を行い、リポジトリ内の配布物を自動上書きしません。

[互換性・検証](docs/COMPATIBILITY.md) · [Magisk](docs/MAGISK.md) · [配布・署名・ハッシュ](docs/RELEASE.md)

MIT License。Shizuku、AndroidX CameraX、Jetpack Compose を使用。
