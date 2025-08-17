import 'dart:typed_data';
import 'native_service.dart';

class ScreenCaptureManager {
  static bool _isInitialized = false;

  // Initialize the service
  static Future<bool> initialize() async {
    try {
      final testResult = await NativeService.test();
      print('Native service test: $testResult');
      _isInitialized = true;
      return true;
    } catch (e) {
      print('Failed to initialize screen capture manager: $e');
      return false;
    }
  }

  // Check if service is ready
  static bool get isInitialized => _isInitialized;

  // Check permissions
  static Future<PermissionStatus> checkPermissions() async {
    final hasScreenCapture = await NativeService.hasScreenCapturePermission();
    final hasOverlay = await NativeService.hasOverlayPermission();

    if (hasScreenCapture && hasOverlay) {
      return PermissionStatus.granted;
    } else if (!hasScreenCapture) {
      return PermissionStatus.screenCaptureNeeded;
    } else {
      return PermissionStatus.overlayNeeded;
    }
  }

  // Request permissions
  static Future<bool> requestScreenCapturePermission() async {
    return await NativeService.requestScreenCapturePermission();
  }

  // Capture screen
  static Future<Uint8List?> captureScreen() async {
    try {
      final permissions = await checkPermissions();
      if (permissions != PermissionStatus.granted) {
        print('Permissions not granted: $permissions');
        return null;
      }

      final List<int>? imageData = await NativeService.captureScreen();
      if (imageData != null) {
        return Uint8List.fromList(imageData);
      }
      return null;
    } catch (e) {
      print('Error in captureScreen: $e');
      return null;
    }
  }

  // Overlay service control
  static Future<bool> startOverlay() async {
    final hasPermission = await NativeService.hasOverlayPermission();
    if (!hasPermission) {
      print('Overlay permission not granted');
      return false;
    }

    return await NativeService.startOverlayService();
  }

  static Future<bool> stopOverlay() async {
    return await NativeService.stopOverlayService();
  }
}

enum PermissionStatus { granted, screenCaptureNeeded, overlayNeeded, denied }
