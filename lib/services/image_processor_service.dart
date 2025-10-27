// lib/services/image_processor_service.dart
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'screen_recognition_service.dart';

class ImageProcessorService {
  static const platform = MethodChannel('uma_screen_capture');

  static Future<void> processTemporaryImage(
    Uint8List imageData, {
    required int width,
    required int height,
    required String format,
  }) async {
    try {
      print(
        '🔍 Processing raw $format image (${width}x${height}) for event recognition...',
      );

      // Recognize event by name only (ML Kit will handle raw RGBA bytes)
      final result = await ScreenRecognitionService.analyzeScreenshot(
        imageData,
        width: width,
        height: height,
        format: format,
      );

      // Show result to user in console
      await _showEventResult(result);

      // Show result overlay on top of other apps (Android native overlay)
      await _showResultOverlay(result);

      print('✅ Event recognition completed');
    } catch (e) {
      print('❌ Error in event recognition: $e');
    }
  }

  static Future<void> _showResultOverlay(RecognitionResult result) async {
    try {
      // Always show overlay - Android will decide whether to show success or error based on confidence
      await platform.invokeMethod('showEventResult', {
        'eventName': result.eventName,
        'characterName': result.characterName,
        'eventType': result.type,
        'confidence': result.confidence ?? 0.0,
        'options': result.options,
      });

      if (result.success && (result.confidence ?? 0.0) >= 0.94) {
        print(
          '📱 Success overlay shown (confidence: ${((result.confidence ?? 0) * 100).toInt()}%)',
        );
      } else {
        print(
          '📱 Error overlay shown (confidence: ${((result.confidence ?? 0) * 100).toInt()}%)',
        );
      }
    } catch (e) {
      print('❌ Error showing overlay dialog: $e');
    }
  }

  static Future<void> _showEventResult(RecognitionResult result) async {
    if (result.success) {
      print('🎯 Event Recognized!');
      print('   Type: ${result.type}');
      print('   Event: ${result.eventName}');

      if (result.characterName != null) {
        print('   Character: ${result.characterName}');
      }

      print('   Options:');
      result.options.forEach((key, value) {
        print('     $key: ${value.replaceAll(r'\r\n', ' | ')}');
      });

      print('   Recommendation: ${result.getRecommendation()}');
    } else {
      print('❌ Event Recognition Failed');
      print('   Reason: ${result.eventName}');
      if (result.extractedTexts != null) {
        print('   Extracted text: ${result.extractedTexts}');
      }
    }
  }
}
