import 'package:flutter/services.dart';

class NativeService {
  static const MethodChannel _channel = MethodChannel('uma_screen_capture');

  // Test connection
  static Future<String> test() async {
    try {
      final String result = await _channel.invokeMethod('test');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to test connection: ${e.message}');
    }
  }

  // Permission checks
  static Future<bool> hasScreenCapturePermission() async {
    try {
      final bool result = await _channel.invokeMethod(
        'hasScreenCapturePermission',
      );
      return result;
    } on PlatformException catch (e) {
      print('Error checking screen capture permission: ${e.message}');
      return false;
    }
  }

  static Future<bool> hasOverlayPermission() async {
    try {
      final bool result = await _channel.invokeMethod('hasOverlayPermission');
      return result;
    } on PlatformException catch (e) {
      print('Error checking overlay permission: ${e.message}');
      return false;
    }
  }

  // Permission requests
  // In your Flutter screen capture manager
  static Future<bool> requestScreenCapturePermission() async {
    try {
      print('Requesting screen capture permission...');
      // This now starts service first, then requests permission
      final bool result = await _channel.invokeMethod(
        'requestScreenCapturePermission',
      );

      if (result) {
        print('✅ Screen capture permission granted and service started');
      } else {
        print('❌ Screen capture permission denied');
      }

      return result;
    } on PlatformException catch (e) {
      print('Error requesting screen capture permission: ${e.message}');
      return false;
    }
  }

  // Screen capture
  static Future<List<int>?> captureScreen() async {
    try {
      final List<int>? result = await _channel.invokeMethod('captureScreen');
      return result;
    } on PlatformException catch (e) {
      print('Error capturing screen: ${e.message}');
      return null;
    }
  }

  // Overlay service
  static Future<bool> startOverlayService() async {
    try {
      await _channel.invokeMethod('startOverlayService');
      return true;
    } on PlatformException catch (e) {
      print('Error starting overlay service: ${e.message}');
      return false;
    }
  }

  static Future<bool> stopOverlayService() async {
    try {
      await _channel.invokeMethod('stopOverlayService');
      return true;
    } on PlatformException catch (e) {
      print('Error stopping overlay service: ${e.message}');
      return false;
    }
  }
}
