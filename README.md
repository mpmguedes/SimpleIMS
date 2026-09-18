# SimpleIMS

> **This is a fork of [TurboIMS](https://github.com/Turbo1123/TurboIMS) by Turbo1123,
> which itself builds on the [IMS project by vvb2060](https://github.com/vvb2060/Ims).
> All credit for the original mechanism and feature set goes to them.**

<div align="center">
  <h3>Simple IMS configuration for Android 12 (API 31) and newer</h3>

  [![Android](https://img.shields.io/badge/Android-12%2B%20(API%2031)-green.svg)](https://www.android.com/)
  [![Version](https://img.shields.io/badge/Version-3.0-brightgreen.svg)](https://github.com/mpmguedes/simpleims/releases)

  **Repository: <https://github.com/mpmguedes/simpleims>**
</div>

## What this fork changes

| | |
|---|---|
| **Name** | SimpleIMS (was "Turbo IMS") |
| **Minimum Android** | **12 / API 31** (upstream required 14 / API 34) |
| **Target Android** | 36 (unchanged) |
| **Package** | `io.github.turboims.pixel` (unchanged) |
| **Logo** | New adaptive icon — a SIM card with signal bars |
| **Applying config** | Writes from the app process first; only restarts when a change is really needed (see below) |

### Applying: when the app restarts, and when it does not

Upstream applies the carrier-config override from an **instrumentation process**.
`startInstrumentation` on your own package makes the system force-stop the app, which
looks exactly like a crash.

This fork first writes from the **ordinary app process**, using a delegated shell
identity (with a Shizuku-wrapped `carrier_config` binder as a second attempt), and
only falls back to instrumentation when both are refused. What that means in practice:

- **Config already applied** → nothing to write → the app stays open.
  Verified: same PID before and after, zero `Force stopping` lines.
- **Config actually has to change** (first run after install, or after you toggle a
  switch) → on some firmware, including the Huawei DCO-LX9 this was tested on, the
  platform refuses both in-process mechanisms:

  ```
  SecurityException: Neither user 10559 nor current process has
                     android.permission.MODIFY_PHONE_STATE
  SecurityException: overrideConfig cannot be invoked by shell
  ```

  The fork then uses instrumentation, which restarts the app. That restart is
  announced up front ("the app will close and reopen") instead of looking like a
  crash. On firmware that permits shell-originated `overrideConfig`, this case never
  arises.

> This section previously claimed the restart had been eliminated. It was measured on
> a path that skipped the write, so nothing was actually attempted — that claim is
> retracted. See `docs/ANDROID12_AUDIT.md` §9.4.

### Other fixes carried over from the port

- Real subscription ids are resolved instead of assuming `subId == slot + 1`
  (fixes `IllegalArgumentException: Invalid phoneId 2147483647 for subId 1`).
- Toggling a switch and re-applying now actually rewrites the config (upstream only
  compared a version counter).
- Critical preferences use `commit()` so they survive the process being killed.

## Requirements

- **Android 12 (API 31) or newer**
- [Shizuku](https://shizuku.rikka.app/) running **and** authorised for this app
- An active SIM

> **Note on VoNR:** `KEY_VONR_*` carrier-config keys only exist from Android 13
> (API 33). On Android 12 the VoNR row is shown disabled and explained — there is no
> key to write, so this is a platform limit, not a bug.

## Build

```
./gradlew assembleDebug     # or assembleRelease
./gradlew test              # 51 unit tests
./gradlew lintDebug         # 0 errors
```

`minSdk 31` / `targetSdk 36` / `compileSdk 36` are declared in
`settings.gradle.kts` (not in `app/build.gradle.kts`).

## License

This project is a fork of [TurboIMS](https://github.com/Turbo1123/TurboIMS) and is
distributed under the **Apache License, Version 2.0** — see [`LICENSE`](LICENSE).

Attribution is also carried in [`NOTICE`](NOTICE):

```
Copyright 2024 Turbo IMS Contributors
Copyright 2023 vvb2060 (Original IMS Project)
```

The upstream notice is retained verbatim, alongside the copyright for this fork's
own modifications:

```
Copyright 2026 mpmguedes
```

Anyone redistributing this fork must keep both notices intact.

---

<details>
<summary><b>Original Turbo IMS README (upstream) — kept for reference</b></summary>

# Turbo IMS

<div align="center">
  <img src="Turboims.png" width="200" alt="Turbo IMS Logo"/>

  <h3>Enhanced IMS Configuration Tool for Google Pixel Devices</h3>

  [![Android](https://img.shields.io/badge/Android-14%2B-green.svg)](https://www.android.com/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
  [![Version](https://img.shields.io/badge/Version-3.0-brightgreen.svg)](https://github.com/Turbo1123/TurboIMS/releases)

  English | [简体中文](README_CN.md)
</div>

---

## 📱 About

**Turbo IMS** is an enhanced fork of the original [IMS project by vvb2060](https://github.com/vvb2060/Ims), designed to enable VoLTE, VoWiFi, VoNR, and other advanced IMS features on Google Pixel phones through privileged system configuration.

This enhanced version includes a modernized UI, improved user experience, automatic language detection, and additional convenience features while maintaining full compatibility with the original implementation.

## ✨ New Features in Turbo IMS

### 🎨 **Modern UI Redesign**
- Professional logo and branding
- Clean, Material Design-inspired interface
- Enhanced splash screen with version display
- Improved visual feedback and status indicators

### 🌍 **Automatic Language Detection**
- Automatically detects system language on first launch
- Portuguese interface for Portuguese users (pt-PT, pt-BR, etc.)
- Chinese interface for Chinese users (zh-CN, zh-TW, zh-HK, etc.)
- English interface for all other regions
- Manual language switching available: the button always shows, in its own
  language, the language you will switch **to**, and cycles
  English → Português → 中文 → English
- The choice is saved, so it survives a restart of the app

### 📡 **Quick Network Settings Access**
- One-tap jump to network settings after successful configuration
- Convenient dialog prompts after applying configuration
- Streamlined workflow for testing IMS features

### 🎯 **Per-SIM Configuration**
- Select specific SIM card (SIM 1 or SIM 2)
- Apply configuration to individual SIMs or all SIMs at once
- Clear visual feedback for selected SIM

### 🔄 **Improved User Experience**
- Auto-return to app after configuration
- Clear success/failure notifications
- Android version detection with QPR2 Beta 3+ warnings
- Real-time Shizuku status monitoring

## 🎯 Key Features

### IMS Features Configuration
- ✅ **VoLTE** (4G Voice) - High-definition voice calling over 4G LTE
- ✅ **VoWiFi** (WiFi Calling) - Make calls over WiFi networks
- ✅ **VT** (Video Calling) - IMS-based video calls
- ✅ **VoNR** (5G Voice) - High-definition voice over 5G NR
- ✅ **Cross-SIM Calling** - Dual-SIM interconnection
- ✅ **UT Supplementary Services** - Call forwarding, waiting, etc.
- ✅ **5G NR** (NSA/SA) - Enable 5G standalone/non-standalone networks

### System Requirements
- Google Pixel device (tested on Pixel 6+)
- Android 14 or higher
- [Shizuku](https://github.com/RikkaApps/Shizuku) installed and running
- Shizuku permission granted to Turbo IMS

## 🚀 Installation

### Method 1: Download APK (Recommended)
1. Download the latest APK from [Releases](https://github.com/Turbo1123/TurboIMS/releases)
2. Install the APK on your Pixel device
3. Grant necessary permissions

### Method 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/Turbo1123/TurboIMS.git
cd TurboIMS

# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 📖 Usage Guide

### Prerequisites Setup
1. **Install Shizuku**
   - Download from [GitHub](https://github.com/RikkaApps/Shizuku/releases) or Google Play
   - Start Shizuku service (via Wireless debugging or Root)

2. **Grant Permissions**
   - Open Turbo IMS
   - Grant Shizuku permission when prompted

### Configuring IMS Features

1. **Check System Status**
   - Verify Android version is displayed
   - Ensure Shizuku status shows "✅ Ready"

2. **Select SIM Card**
   - Tap "Select SIM Card" button
   - Choose SIM 1, SIM 2, or "Apply to All SIM Cards"

3. **Enable Features**
   - Toggle switches for desired IMS features
   - All features are enabled by default

4. **Apply Configuration**
   - Tap the blue "Apply Configuration" button
   - Wait 3 seconds for configuration to complete
   - App will automatically return to foreground
   - Choose "Go to Network Settings" to verify features

### Important Notes

⚠️ **Android 16 QPR2 Beta 3+ Users**
- Configuration is non-persistent on Android 16 Beta builds
- Settings will reset after reboot
- You must reapply configuration after each restart

✅ **Verifying Configuration**
- Go to Settings → Network & Internet → SIMs
- Check that VoLTE, VoWiFi options are visible
- Make a test call to verify functionality

## 🛠️ Technical Details

### Architecture
- **Privileged Process**: Uses Android Instrumentation to run with system privileges
- **Shizuku Integration**: Leverages Shizuku framework for privilege escalation
- **CarrierConfigManager**: Directly modifies carrier configuration bundles
- **Shell Permission Delegation**: Temporary NETWORK_SETTINGS permission

### Configuration Keys Modified
The app modifies the following carrier configuration keys:
- `KEY_CARRIER_VOLTE_AVAILABLE_BOOL`
- `KEY_CARRIER_VT_AVAILABLE_BOOL`
- `KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL`
- `KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL`
- `KEY_EDITABLE_ENHANCED_4G_LTE_BOOL`
- `KEY_HIDE_ENHANCED_4G_LTE_BOOL`
- And many more... (see source code for complete list)

### Package Details
- **Package Name**: `io.github.turboims.pixel`
- **Min SDK**: Android 14 (API 34)
- **Target SDK**: Android 15 (API 35)
- **Version**: 3.0 (Build 5)

## 🤝 Credits & Acknowledgments

### Original Project
This project is a fork of the excellent [**IMS by vvb2060**](https://github.com/vvb2060/Ims).

Special thanks to:
- **[@vvb2060](https://github.com/vvb2060)** - Original author and creator of the IMS configuration tool
- The original IMS project enabled thousands of Pixel users to access carrier features

### Upstream Project
Please visit the original project: **[https://github.com/vvb2060/Ims](https://github.com/vvb2060/Ims)**

If you find this tool useful, please consider starring both this repository and the original project! ⭐

### Dependencies
- [Shizuku](https://github.com/RikkaApps/Shizuku) by RikkaApps - Privilege escalation framework
- [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) by LSPosed - Accessing hidden Android APIs

## 📄 License

```
Copyright 2024 Turbo IMS Contributors
Copyright 2023 vvb2060 (Original IMS Project)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## ⚠️ Disclaimer

- This tool modifies system-level carrier configurations
- Use at your own risk
- The authors are not responsible for any issues that may arise
- Always verify IMS features work with your carrier before relying on them
- This is an unofficial tool not endorsed by Google or any carrier

## 🐛 Issues & Support

If you encounter any issues or have feature requests:
1. Check [existing issues](https://github.com/Turbo1123/TurboIMS/issues)
2. Create a new issue with detailed information:
   - Device model
   - Android version
   - Shizuku version
   - Steps to reproduce
   - Logcat output (if applicable)

## 🌟 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for:
- Bug fixes
- New features
- UI improvements
- Translations
- Documentation

---

<div align="center">

  **Made with ❤️ for the Pixel Community**

  If this project helped you, consider giving it a ⭐!

</div>

</details>
