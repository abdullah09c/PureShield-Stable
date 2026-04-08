<div align="center">
  <h1>🛡️ PureShield</h1>
  <p><b>Guard Your Mind. Purify Your Screen.</b></p>
  <p><i>بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</i></p>

  <p>
    <a href="https://github.com/abdullah09c/PureShield-Stable/releases"><img src="https://img.shields.io/github/v/release/abdullah09c/PureShield-Stable?style=flat-square&color=00B67A" alt="Latest Release" /></a>
    <a href="https://android-arsenal.com/api?level=23"><img src="https://img.shields.io/badge/API-23%2B-blue.svg?style=flat-square" alt="API" /></a>
    <a href="https://github.com/abdullah09c/PureShield-Stable/blob/main/LICENSE"><img src="https://img.shields.io/github/license/abdullah09c/PureShield-Stable?style=flat-square" alt="License" /></a>
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin" />
  </p>

  <p>A lightweight, privacy-respecting Android Application focused on digital wellbeing: blocking short-form content (Reels/Shorts) and filtering harmful web content, all without collecting any data</p>
</div>

---

## 📌 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Initial Setup](#-initial-setup)
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
- **Uninstall Protection**: PIN-protected Device Admin prevents impulsive removal of the app when you are most vulnerable to relapse.
- **Start on Boot**: Automatically and silently initiates protection when your phone turns on.
- **Customizable Reminder Messages**: Personalize the block screen with custom text, Ayahs, or reminders to stay grounded.
- **Persistent Notification**: Low priority persistent notification to keep the accessibility service alive and reliable.
- **No Ads**: Fully ad-free experience with no banner ads, popups, or ad tracking.
- **100% Private & Open Source**: Zero trackers, absolutely no internet permission requested. Your data never leaves your device.

## 📸 Screenshots

| Home | Blocking Setup | Active Overlay |
| :---: | :---: | :---: |
| <img src="docs/home.jpg" width="220" alt="Home Screen"/> | <img src="docs/setup.jpg" width="220" alt="Setup Screen"/> | <img src="docs/overlay.jpg" width="220" alt="Block Overlay"/> |


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

## 🔒 Privacy Policy

PureShield is built on the philosophy of absolute privacy.
- The app explicitly **does not request the `INTERNET` permission** in its manifest.
- The Accessibility Service runs completely locally and never logs your screen contents.
- No analytics, telemetry, or crash reporting SDKs are bundled.

## 🤝 Contributing

Contributions are highly welcome! Whether it's finding bugs, adding support for new social media apps, improving the UI, or translating the application, your help is appreciated.

## 📄 License

This project is licensed under <a href="https://www.gnu.org/licenses/gpl-3.0.en.html">GPL-3.0</a>.

Anyone may use, share, and modify this code under the terms of the GNU GPL v3.
