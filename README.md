# Teldrive Android

The official Android client and local gateway for [TelDrive](https://github.com/tgdrive/teldrive) – a powerful Telegram-based cloud storage system.

This Android app not only serves as a management dashboard for your TelDrive environment but also embeds the core TelDrive Go server as a native binary (`libteldrive.so`). This allows you to run a full-fledged TelDrive server directly from your Android phone, enabling local network streaming and providing a seamless Stremio/Nuvio Addon out of the box!

## 🚀 Features

- **Embedded Go Server:** Runs the high-performance TelDrive Go backend natively on your Android device.
- **Built-in Stremio Addon:** Automatically hosts a local Stremio/Nuvio Addon API on port `8080`. Connect your TV or PC to your phone's network IP to stream your Telegram media library in real-time.
- **Smart Episode Matching:** The addon uses Cinemeta to dynamically search your TelDrive database for movie titles and complex TV show formats (e.g., `S02E05`, `2x05`).
- **Material 3 Admin Dashboard:** A sleek, Nord-inspired dark slate dashboard to manage the server lifecycle.
- **Network IP Detection:** Instantly displays your local IPv4 address so you know exactly what URL to paste into Stremio or Rclone on other devices.
- **Rclone Token Wizard:** Easily copy your active session's JWT token for Rclone integration.

## 🛠️ Building the App

### Prerequisites
- **Android Studio** (Flamingo or newer)
- **Java Development Kit (JDK) 17** (Required for Gradle 8.x)
- **Go 1.21+** (If you intend to recompile the native backend binary)

### 1. Compile the Backend (Optional)
This repository already contains the pre-compiled `libteldrive.so` binary. However, if you are developing the backend and want to update the binary:
1. Navigate to your main `teldrive` backend repository.
2. Run the included PowerShell or Bash script: `.\build_android.ps1`
3. The script will automatically strip, cross-compile, and drop the new `libteldrive.so` into `teldrive-android/app/src/main/jniLibs/arm64-v8a/`.

### 2. Configure Local Properties (Optional)
To pre-fill your database credentials so you don't have to type them into the app every time you launch it, create a `local.properties` file in the root directory:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk

# Pre-filled Database Credentials (Optional)
TELDRIVE_SUPABASE_DSN=postgresql://postgres...
TELDRIVE_JWT_SECRET=your_jwt_secret
TELDRIVE_PORT=8080
TELDRIVE_TG_APP_ID=your_telegram_app_id
TELDRIVE_TG_APP_HASH=your_telegram_app_hash
TELDRIVE_TG_UPLOADS_ENCRYPTION_KEY=optional_encryption_key
```

> **Note:** Do NOT commit your `local.properties` file to GitHub! The `.gitignore` is already set up to ignore it. If this file is omitted, the app will compile fine, but the setup screen will be blank for manual entry.

### 3. Build the APK
Open the project in Android Studio, click **Sync Project with Gradle Files**, and hit **Run (Play)** to install it on your device, or go to **Build > Build Bundle / APK > Build APK(s)**.

---

## 📺 How to Use the Stremio Addon

1. Install the APK on your Android phone.
2. Open the **Teldrive** app, enter your configuration details, and click **Save Config & Continue**.
3. Toggle the Server to **ON**.
4. Note the **Network** IP displayed on the dashboard (e.g., `http://192.168.1.15:8080`).
5. Ensure your Smart TV, PC, or secondary phone is connected to the exact same Wi-Fi network.
6. Open **Stremio** or **Nuvio** on your other device, go to **Add-ons**, and search for your local manifest URL:
   `http://192.168.1.15:8080/stremio/manifest.json`
7. Click **Install**. 

You can now click on any movie or TV show in the public Stremio catalog, and Teldrive will automatically find and stream your matching files directly from Telegram!
