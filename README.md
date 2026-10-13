# Secure Wipe

An Android app that erases everything on the device, but only after you confirm with **your password and your fingerprint**.

> ⚠️ **Warning:** This app performs a real factory reset. All data on the device is deleted and cannot be recovered. Back up anything important and test on an emulator or spare phone before using it on your main device.

## How it works

1. Grant the app device administrator permission (one-time system prompt).
2. Set a wipe password (minimum 6 characters).
3. To erase the device, enter your password and tap **ERASE EVERYTHING**.
4. Confirm with your fingerprint. Only a successful scan triggers the wipe.

## Features

- Two-step confirmation: password, then fingerprint
- Fingerprint-only prompt, with no PIN or pattern fallback
- Password stored only as a salted PBKDF2 hash, never in plain text
- 60-second lockout after 5 wrong passwords
- Screenshots and recent-apps previews blocked
- Wipes internal storage and external storage via Android's `DevicePolicyManager.wipeData()`

## Build

The APK is built automatically with GitHub Actions.

1. Open the **Actions** tab and run **Build APK** (or push a commit).
2. When the run finishes, download **SecureWipe-APK** from the **Artifacts** section.
3. Unzip it and install `app-debug.apk` on your device (allow installs from unknown sources).

You can also open the project in Android Studio and choose **Build → Build APK(s)**.

## Requirements

- Android 8.0 (API 26) or newer
- A fingerprint enrolled on the device
- Device admin permission granted to the app

## Limitations

- Cloud backups and online accounts are not deleted by a local wipe.
- After the reset, Factory Reset Protection may require the device's previous Google account to set it up again.
- Apps that can erase a device are restricted on Google Play, so this app is intended for direct (sideloaded) installation.
- The fingerprint check happens in the app's logic. It is not cryptographically bound to a Keystore key.

## Disclaimer

Provided as is, without warranty. You are responsible for any data loss. Use it only on devices you own.
