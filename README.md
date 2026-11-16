# 誤発信防止（発信確認）

Google Play ストアで無料配信しているアプリのソースコードを公開します。
勉強中のkotlinで実装してみました。Android-Studio標準のAgentによるAI伴奏による実装です。

## 📱 Google Play ストア
https://play.google.com/store/apps/details?id=jp.ruma.freesoft.kotlinlesson.callConfirmation
   
## ✨ 主な機能と技術要素
- 電話発信前に確認画面を表示して誤発信を防ぎます。
- 技術要素
1. kotlinによる Material Design3 の実装
2. ２種類のForegroundService (short service, special use)
3. OverlayWindow, FullScreenIntent
4. Jetpack Preferences DataStore
5. Direct-Boot Mode

## 🛠 動作環境 / 開発環境
- Android SDK: 34 (Android 14)以上
- Android Studio

## 🚀 ビルドについて
1. proguard-roles.pro  
  release ビルドの場合に削除するクラスやメソッドが定義されています。
2. build.gradle.kts  
  release ビルドの場合に checkClassAbsenceAfterR8 タスクが定義されています。AIにコーディングしてもらいました。
  削除すべきクラスやメソッドが残っていればエラーにします。


