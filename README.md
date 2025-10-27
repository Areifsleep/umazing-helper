# Umazing Helper

Uma Musume Pretty Derby event recognition assistant app that helps you identify and filter character-specific events using screen capture technology.

## ⚠️ Alpha Release - Read Before Using

**Current Status**: This is an **alpha release** with limited device testing.

**Known Limitations**:

- 🔴 **Screen capture coordinates are hardcoded** - May not work on different screen resolutions
- 🔴 **Tested on single device only** - Compatibility unknown
- 🔴 **No region calibration feature** - Cannot customize capture areas

**Tested Configuration**:

- Device: [Add your device model]
- Android: [Add your Android version]
- Resolution: [Add your screen resolution]

**Use at your own risk.** This release is for early feedback and testing.

See [CHANGELOG.md](CHANGELOG.md) for detailed limitations.

---

## 📱 Features

- **Screen Capture Recognition**: Capture and analyze game screens to identify events
- **Character Filter**: Optional character-specific event filtering
  - Search through 86+ Uma Musume characters
  - Filter events for specific characters
  - Persistent selection across app sessions
- **Floating Overlay**: Convenient scan button overlay
  - Draggable scan button for easy access
  - Long-press (500ms) to show character selection
  - Transparent background with green "U" icon
- **Dual Interface**:
  - Flutter UI for in-app character selection
  - Native Android overlay for quick access during gameplay

## 🚀 Getting Started

### Prerequisites

- Flutter SDK (latest stable version)
- Android SDK (API 21 or higher)
- Android device or emulator

### Installation

1. Clone the repository:

```bash
git clone https://github.com/Areifsleep/umazing-helper.git
cd umazing-helper
```

2. Install dependencies:

```bash
flutter pub get
```

3. Run the app:

```bash
flutter run
```

### Required Permissions

The app requires the following permissions:

1. **Screen Capture Permission**: To capture game screens for event recognition
2. **Overlay Permission**: To display the floating scan button

On first launch, the app will guide you through granting these permissions.

## 📖 User Guide

### Using the Scan Button

#### Method 1: In-App Character Selection

1. Open the app and navigate to "Screen Capture" screen
2. Use the search bar to find and select your Uma character
3. Grant required permissions when prompted
4. Tap "Start Overlay" to show the floating scan button

#### Method 2: Overlay Character Selection

1. Start the overlay service
2. **Long-press (hold for 0.5 seconds)** the green "U" scan button
3. A character selection overlay will appear
4. Select your desired character from the list
5. The selection will sync with the main app

### Character Filter

- **All Characters (No Filter)**: Shows events for all characters
- **Specific Character**: Only shows events for the selected character
- **Clear Selection**: Tap the X button to reset to "All Characters"
- **Search**: Type to quickly find characters by name

### Scan Button Features

- **Drag**: Touch and drag to reposition the button
- **Long-press**: Hold for 500ms to open character selection overlay
- **Icon**: Green "U" (for Uma) in a white scanning frame

## 🏗️ Project Structure

```
lib/
├── main.dart                           # App entry point
├── models/                             # Data models
├── screens/                            # UI screens
│   ├── home_screen.dart
│   ├── screen_capture_screen.dart      # Main screen with character filter
│   ├── event_list_screen.dart
│   └── event_detail_screen.dart
├── services/                           # Business logic
│   ├── recognition_data_service.dart   # Character & event data management
│   └── screen_capture_manager.dart     # Android native communication
└── widgets/                            # Reusable components

android/
└── app/src/main/kotlin/com/example/umazing_helper/
    ├── MainActivity.kt
    ├── OverlayService.kt               # Overlay service
    ├── OverlayManager.kt               # Drag & long-press handling
    └── CharacterSelectionOverlay.kt    # Native character picker

assets/
└── data/
    ├── uma_data.json                   # Character & event data
    ├── support_card.json
    ├── career.json
    └── races.json
```

## 📊 Data Sources

This project uses game data from multiple sources:

### Uma Musume Character & Event Data

- **Source**: [GameTora Uma Musume Database](https://gametora.com/umamusume)
- **Usage**: Character names, event information, and game reference data
- **Acknowledgement**: We gratefully acknowledge GameTora for providing comprehensive Uma Musume data

### JSON Data Structure (MIT Licensed)

- **Original Project**: MIT Licensed data from Akari's project
- **License**: MIT License (See below)
- **Files**: `uma_data.json`, `support_card.json`, `career.json`, `races.json`

## 📄 License

### Application License

MIT License

Copyright (c) 2025 Areifsleep

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### Data License

The JSON data files (`uma_data.json`, `support_card.json`, `career.json`, `races.json`) are derived from:

**Original MIT License**

Copyright (c) 2025 Akari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 🙏 Acknowledgements

### Data & Resources

- **[GameTora](https://gametora.com/umamusume)** - For providing comprehensive Uma Musume character and event database
- **Akari** - For the MIT-licensed JSON data structure and game data compilation
- **Cygames** - Uma Musume Pretty Derby is a trademark of Cygames, Inc.

### Technologies

- **Flutter** - Cross-platform UI framework
- **Kotlin** - Android native implementation
- **Material Design** - UI/UX design system

### Community

- Flutter community for excellent documentation and packages
- Android developers for overlay and screen capture examples
- Uma Musume community for game knowledge and testing

## ⚠️ Disclaimer

This is a fan-made helper application and is not affiliated with, endorsed by, or connected to Cygames, Inc. or the official Uma Musume Pretty Derby game.

- This app does NOT modify game files or game data
- This app does NOT provide any unfair advantages
- This app is for informational and convenience purposes only
- Uma Musume Pretty Derby and all related trademarks are property of Cygames, Inc.

Use this application at your own discretion and in accordance with the game's Terms of Service.

## 🐛 Known Issues

- First long-press may show "Failed to load characters" if data hasn't loaded yet (fixed in latest version)
- Overlay requires manual permission grant on Android 6.0+
- Screen capture requires MediaProjection API (Android 5.0+)

## 🔄 Updates & Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and updates.

## 📞 Support

For issues, questions, or contributions:

- Open an issue on GitHub
- Check existing documentation in the `/docs` folder
- Review closed issues for similar problems

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📚 Additional Documentation

- [Character Filter Feature](CHARACTER_FILTER_FEATURE.md) - Detailed character filter implementation
- [Overlay System](OVERLAY_FIXES.md) - Native Android overlay documentation
- [Scan Icon Design](SCAN_ICON_UPDATE.md) - Icon design specifications
- [UI Improvements](UI_IMPROVEMENTS.md) - UI/UX enhancement history

---

Made with ❤️ for the Uma Musume community
