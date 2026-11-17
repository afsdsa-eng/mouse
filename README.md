# マウスボタン変更 (Android)

Kotlin製のAndroidアプリです。GitHub ActionsでデバッグAPKを自動ビルドします。

## 現在の実装
- 左・右・中央・サイドボタンの割り当て設定画面
- 設定をSharedPreferencesに保存
- 日本語UI

## 重要な制限
この初期版は**割り当て設定を保存するところまで**です。root化していない通常のAndroidでは、一般アプリが他のアプリに届くマウスクリックをシステム全体で横取り・置換する公開APIはありません。そのため、このAPKはシステム全体のクリック動作を変更しません。設定画面にも制限を明記しています。

## APKの取得
1. GitHubの **Actions** タブを開く
2. **Build Android APK** を選択
3. 最新の成功した実行を開く
4. **Artifacts** の `mouse-debug-apk` をダウンロード
5. ZIPを展開して `app-debug.apk` をAndroidにインストール

## ローカルビルド
Android Studioでこのリポジトリを開き、Gradle同期後に `app` の debug APK をビルドしてください。
