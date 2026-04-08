# Privacy Policy for PureShield

Last updated: 2026-04-09

PureShield is designed to run locally on your Android device to help you block distracting short-form content and provide start-on-boot behavior and Private DNS shortcuts.

## 1. Data Collection and Sharing

PureShield does not collect, store, transmit, or sell personal data.

- No account creation
- No analytics SDK
- No advertising SDK
- No crash reporting SDK
- No telemetry SDK
- No cloud sync or server-side user database

PureShield stores app configuration only on your device (for example, blocker settings and related preferences needed for app functionality).

## 2. Network Access

PureShield does not request the Android INTERNET permission.

This means PureShield cannot send your app activity, Accessibility content, or personal data to external servers.

Private DNS shortcuts, when used, open Android system settings so you can change DNS directly on the device. PureShield does not proxy, inspect, or route your internet traffic.

## 3. Accessibility Service Usage

PureShield uses Android Accessibility Service only for user-requested functionality:

- Detecting when supported short-form feeds/screens are opened
- Triggering local block actions based on your selected settings

Accessibility-related processing is performed locally on-device and is not shared externally.

## 4. Permissions and Features

PureShield may request the following permissions or system features depending on what you enable:

- RECEIVE_BOOT_COMPLETED: Optional start-on-boot behavior
- POST_NOTIFICATIONS: Persistent status notification
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: Optional battery-optimization exemption to keep blocking reliable
- Accessibility Service (BIND_ACCESSIBILITY_SERVICE): Core blocking feature

PureShield does not use Device Admin or request BIND_DEVICE_ADMIN in the current version.

Private DNS shortcuts open Android system settings so you can change DNS directly on the device. PureShield does not proxy, inspect, or route your internet traffic.

## 5. Local Storage and Backup

- PureShield settings are stored locally on your device.
- The app is configured with backup disabled in app manifest settings (`allowBackup=false`) to reduce unintended data export.

## 6. Children

PureShield is a self-control and digital wellbeing utility. It is not designed to collect personal information from children.

## 7. Changes to This Policy

This Privacy Policy may be updated from time to time. Material changes will be reflected by updating the date at the top of this document.

## 8. Contact

Developer: Abdullah Al Fuwad

GitHub: https://github.com/abdullah09c/PureShield-Stable
