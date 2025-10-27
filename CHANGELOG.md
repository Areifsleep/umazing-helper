# Changelog

All notable changes to the Umazing Helper project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Complete user documentation in README.md
- License information for both application and data
- Acknowledgements for GameTora and data contributors
- Comprehensive user guide with screenshots and examples

## [0.1.0-alpha] - 2025-10-27

⚠️ **ALPHA RELEASE - Limited Device Support**

This is an early alpha release with limited testing. See [Known Limitations](#known-limitations) below.

### Added

- Character filter feature with searchable dropdown
- Native Android character selection overlay
- Bidirectional synchronization between Flutter UI and Kotlin overlay
- Long-press (500ms) gesture to show character selection
- Scan button icon with green "U" letter
- Transparent button background
- Read-only text field when character is selected
- Autocomplete dialog with outside-click-to-close
- Draggable overlay button
- Permission handling (screen capture + overlay)

### Data

- Integrated Uma Musume character data (86 characters)
- Event data from MIT-licensed JSON files
- Support card, career, and race data

### Known Limitations

⚠️ **Critical Limitations**:

- **Screen capture coordinates are hardcoded** - May not work on devices with different screen resolutions
- **Tested on single device only** - [Add your device model and resolution here]
- **No region calibration** - Users cannot customize capture regions
- **No multi-device testing** - Compatibility with other devices unknown

⚠️ **Use at your own risk**. This alpha is released for:

- Community feedback and testing
- Early adopters willing to help debug
- Developers who can modify code for their device

### Tested On

- Device: [Your Device Model]
- Android Version: [Your Android Version]
- Screen Resolution: [Your Screen Resolution]
- Game Version: Uma Musume Pretty Derby [Version]

---

## Version Naming

- **Major version** (X.0.0): Breaking changes or major feature additions
- **Minor version** (0.X.0): New features, improvements, backward compatible
- **Patch version** (0.0.X): Bug fixes and minor improvements

## Categories

- **Added**: New features
- **Changed**: Changes to existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Security improvements
- **Technical**: Internal/architectural changes
- **Data**: Data updates or changes
