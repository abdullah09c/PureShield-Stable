<div align="center">
  <h1>🛡️ PureShield</h1>
  <p><b>Guard Your Mind. Purify Your Screen.</b></p>
  <p><i>بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</i></p>

  <p>
    <a href="https://github.com/abdullah09c/PureShield/releases"><img src="https://img.shields.io/github/v/release/abdullah09c/PureShield?style=flat-square&color=00B67A" alt="Latest Release" /></a>
    <a href="https://android-arsenal.com/api?level=21"><img src="https://img.shields.io/badge/API-21%2B-blue.svg?style=flat-square" alt="API" /></a>
    <a href="https://github.com/abdullah09c/PureShield/blob/main/LICENSE"><img src="https://img.shields.io/github/license/abdullah09c/PureShield?style=flat-square" alt="License" /></a>
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin" />
  </p>

  <p>A minimal, lightweight, and privacy-respecting Android application designed to block addictive short-form content (Reels & Shorts) and filter adult content using Private DNS — built with an Islamic theme.</p>
</div>

---

## 📌 Table of Contents

- [Features](#-features)
- [How It Works](#-how-it-works)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Initial Setup](#-initial-setup)
- [Building from Source](#-building-from-source)
- [Tech Stack](#-tech-stack)
- [Privacy Policy](#-privacy-policy)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

- **Reels & Shorts Blocker**: Accurately detects and blocks addictive short-form video UI without breaking the core functionality of the apps. Supported platforms:
  - YouTube Shorts
  - Facebook Reels & FB Lite Reels
  - Instagram
  - TikTok
- **Safe Internet (Private DNS)**: Easily apply community-trusted DNS filters to block adult material network-wide. Defaults include:
  - CleanBrowsing (Family/Adult filters)
  - Cloudflare Family
  - Google SafeDNS
- **Uninstall Protection**: PIN-protected Device Admin prevents impulsive removal of the app when you are most vulnerable to relapse.
- **Start on Boot**: Automatically and silently initiates protection when your phone turns on.
- **Customizable Reminder Messages**: Personalize the block screen with custom text, Ayahs, or reminders to stay grounded.
- **Persistent Notification**: Low priority persistent notification to keep the accessibility service alive and reliable.
- **100% Private & Open Source**: Zero trackers, absolutely no internet permission requested. Your data never leaves your device.

## 🛠️ How It Works

PureShield utilizes Android's **Accessibility Service** to read the active window content and view hierarchies. By doing so, it can precisely pinpoint when you navigate to addictive screens (like the "Reels" tab on Instagram or "Shorts" on YouTube) and automatically draw an overlay over it, showing your predefined reminder and redirecting you out of the loop.

## 📸 Screenshots

| Home | Blocking Setup | Active Overlay |
| :---: | :---: | :---: |
| <img src="docs/home.jpg" width="220" alt="Home Screen"/> | <img src="docs/setup.jpg" width="220" alt="Setup Screen"/> | <img src="docs/overlay.jpg" width="220" alt="Block Overlay"/> |

*(Note: Don't forget to create a `docs` folder and add your `home.jpg`, `setup.jpg`, and `overlay.jpg` screenshots, or update the paths above!)*

## 📥 Installation

You can download the latest compiled APK from the [Releases section](https://github.com/abdullah09c/PureShield/releases).

1. Download the `PureShield-release.apk` file.
2. Open the file and allow "Install from Unknown Sources" if prompted.
3. Once installed, follow the setup guide below.

## 🚀 Initial Setup

Since PureShield acts as a system-level overlay and accessibility service, it requires a few permissions to function reliably.

1. Open **PureShield**.
2. **Battery Optimization**: Tap the prompt to disable battery optimization. This stops Android from killing the blocking service in the background.
3. **Notifications (Android 13+)**: Allow notification permission to keep the service running smoothly.
4. **Accessibility Service**: Tap the **Reels Blocker toggle**, which redirects you to Accessibility Settings.
   - Look for **PureShield** under "Downloaded Apps" or "Installed Services".
   - Turn it **ON**.
   - ⚠️ **Android 13+ Users**: If it says "Restricted Setting", go back to your phone's Settings > Apps > PureShield. Tap the three dots (⋮) in the top right corner and tap **Allow Restricted Settings**. Then try enabling the Accessibility Service again.
5. Setup your **PIN Code** and enable **Uninstall Protection** (Device Admin) via settings to solidify the barrier.

## 💻 Building from Source

This project is built purely with AndroidX and has no complex third-party dependencies, keeping the build size exceptionally small (~3 MB) and compile times very fast.

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah09c/PureShield.git
   ```
2. Open the cloned directory in **Android Studio**.
3. Let Gradle sync project files.
4. Hit **Run** (`Shift + F10`) to compile and deploy to your connected Android device or emulator.

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI Toolkit**: XML Layouts, Material Components
- **Min SDK**: API 21 (Android 5.0) — *Supports 99%+ of active devices*
- **Target SDK**: API 35 (Android 15)
- **Architecture**: Minimalist clean architecture, Native SharedPreferences for persistent configuration.

## 🔒 Privacy Policy

PureShield is built on the philosophy of absolute privacy.
- The app explicitly **does not request the `INTERNET` permission** in its manifest.
- The Accessibility Service runs completely locally and never logs your screen contents.
- No analytics, telemetry, or crash reporting SDKs are bundled.

## 🤝 Contributing

Contributions are highly welcome! Whether it's finding bugs, adding support for new social media apps, improving the UI, or translating the application, your help is appreciated.

## 👨‍💻 Developer

**Abdullah Al Fuwad**  
*Noakhali Science and Technology University (NSTU)*  
GitHub: [@abdullah09c](https://github.com/abdullah09c)

## 📄 License

This project is licensed under the [GPL-3.0 License](LICENSE) - see the LICENSE file for details. Being Free and Open Source Software (FOSS) ensures the community can vet the code and contribute to its mission.
