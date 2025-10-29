import 'package:shared_preferences/shared_preferences.dart';

/// Service to manage scan button preferences (size and transparency)
class ScanButtonPreferences {
  static const String _keyScanButtonSize = 'scan_button_size';
  static const String _keyScanButtonOpacity = 'scan_button_opacity';

  // Default values
  static const double defaultSize = 60.0;
  static const double defaultOpacity = 0.9;

  /// Get scan button size (40-100)
  static Future<double> getScanButtonSize() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getDouble(_keyScanButtonSize) ?? defaultSize;
  }

  /// Set scan button size (40-100)
  static Future<void> setScanButtonSize(double size) async {
    final prefs = await SharedPreferences.getInstance();
    final clampedSize = size.clamp(40.0, 100.0);

    // Save as double (Flutter format)
    await prefs.setDouble(_keyScanButtonSize, clampedSize);

    // Also save as string for Android to read easily
    await prefs.setString(
      'flutter.$_keyScanButtonSize',
      clampedSize.toString(),
    );

    print('💾 Scan button size saved: $clampedSize (both formats)');
  }

  /// Get scan button opacity (0.1-1.0)
  static Future<double> getScanButtonOpacity() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getDouble(_keyScanButtonOpacity) ?? defaultOpacity;
  }

  /// Set scan button opacity (0.1-1.0)
  static Future<void> setScanButtonOpacity(double opacity) async {
    final prefs = await SharedPreferences.getInstance();
    final clampedOpacity = opacity.clamp(0.1, 1.0);

    // Save as double (Flutter format)
    await prefs.setDouble(_keyScanButtonOpacity, clampedOpacity);

    // Also save as string for Android to read easily
    await prefs.setString(
      'flutter.$_keyScanButtonOpacity',
      clampedOpacity.toString(),
    );

    print('💾 Scan button opacity saved: $clampedOpacity (both formats)');
  }

  /// Get all preferences
  static Future<Map<String, double>> getAllPreferences() async {
    return {
      'size': await getScanButtonSize(),
      'opacity': await getScanButtonOpacity(),
    };
  }

  /// Reset to defaults
  static Future<void> resetToDefaults() async {
    await setScanButtonSize(defaultSize);
    await setScanButtonOpacity(defaultOpacity);
    print('🔄 Scan button preferences reset to defaults');
  }
}
