# KML Logger

[English](#english) | [日本語](#japanese)

---

<a name="english"></a>
## English

A battery-efficient GPS logger app designed for Android. Recorded location data is saved in **KML format**, ready to be imported directly into tools such as Google Earth.

### 🚀 Features

- **Battery-Efficient Design**: Location update interval is set to 1 minute by default to minimize battery consumption.
- **KML Format Output**: Saves tracks as standard KML files for easy route management and visualization.
- **Background Tracking**: Uses an Android Foreground Service to continue recording even when the app is in the background or the screen is off.
- **Instant Event Logging**: Immediately captures and records coordinates upon tapping "Start", "Pause", "Resume", or "Stop".
- **Real-Time Log View**: Displays captured coordinates, timestamps, and status events in real time on screen.
- **Customizable File Names**: Allows custom file naming in addition to default timestamp-based names.

### 📱 How to Use

1. Launch the app and optionally enter a custom file name.
2. Tap **Start** to begin logging.
   - *Note: Location and background permissions will be requested on the first launch.*
3. In the permission dialog/settings, select **"Allow all the time"** for location access (required for screen-off tracking).
4. Tap **Pause** to pause recording, and **Resume** to continue.
5. Tap **Stop** to end the session and finalize the KML file.

#### 📂 File Storage Location
Recorded `.kml` files are stored in the app's external documents directory:
`Android/data/com.kusa.kmllogger/files/Documents/`
*(Accessible via device file manager apps)*

### 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts (Material 3)
- **Location API**: Google Play Services (Fused Location Provider)
- **Service**: Foreground Service (Location type)
- **Data Format**: KML (XML based)
- **Target SDK**: 37 (Android 15)

### ⚠️ Notes & Tips

- Indoor usage or weak GPS reception may cause delays in location updates.
- Android battery optimization settings may terminate background services. For reliable tracking, set the app battery usage setting to **"Unrestricted"**.

### 📄 License

[MIT License](LICENSE)

---

<a name="japanese"></a>
## 日本語

Android端末で動作する、省電力設計のGPSロガーアプリです。取得した位置情報は、Google Earthなどで直接読み込める **KML形式** で保存されます。

### 🚀 特徴

- **省電力設計**: バッテリー消費を抑えるため、位置情報の取得間隔をデフォルト1分に設定しています。
- **KML形式出力**: 標準的なKMLファイルとして保存され、移動ルートの管理が容易です。
- **バックグラウンド記録**: フォアグラウンドサービスを実装しており、アプリがバックグラウンドにある時や画面がオフの時でも記録を継続します。
- **イベント即時記録**: 「開始」「一時停止」「再開」「停止」のボタン操作を行った瞬間の座標を即座に記録します。
- **リアルタイム・ログ表示**: 画面上で取得した座標やステータスをリアルタイムに確認できます。
- **ファイル名カスタマイズ**: 開始日時のデフォルト名のほか、ユーザーが任意のファイル名を設定可能です。

### 📱 使い方

1. アプリを起動し、必要に応じてファイル名を入力します。
2. 「Start（開始）」ボタンを押すと記録を開始します。
   - ※ 初回起動時に位置情報（およびバックグラウンド実行）の許可が求められます。
3. 画面の指示に従い、位置情報の設定で **「常に許可」** を選択してください（スリープ中の記録に必要です）。
4. 「Pause（一時停止）」で一時停止、「Resume（再開）」で記録を再開します。
5. 「Stop（停止）」で記録を終了し、ファイルを保存します。

#### 📂 保存先
記録された `.kml` ファイルは、端末の以下のパスに保存されます：
`Android/data/com.kusa.kmllogger/files/Documents/`
（ファイルマネージャーアプリなどでアクセス可能です）

### 🛠 技術スタック

- **Language**: Kotlin
- **UI**: XML Layouts (Material 3)
- **Location API**: Google Play Services (Fused Location Provider)
- **Service**: Foreground Service (Location type)
- **Data Format**: KML (XML based)
- **Target SDK**: 37 (Android 15)

### ⚠️ 注意事項

- 高精度な位置情報を取得するため、GPSが届きにくい屋内などでは座標の更新が遅れる場合があります。
- Androidの「バッテリーの最適化」設定によりサービスが停止される場合があります。安定した記録のために、アプリの設定から「制限なし」にすることをお勧めします。

### 📄 ライセンス

[MIT License](LICENSE)
