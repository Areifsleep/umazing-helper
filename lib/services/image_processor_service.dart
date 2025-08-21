// lib/services/image_processor_service.dart
import 'dart:typed_data';
import 'screen_recognition_service.dart';

class ImageProcessorService {
  static Future<void> processTemporaryImage(Uint8List imageData) async {
    try {
      print('🔍 Processing image for event recognition...');

      // Recognize event by name only
      final result = await ScreenRecognitionService.analyzeScreenshot(
        imageData,
      );

      // Show result to user
      await _showEventResult(result);

      print('✅ Event recognition completed');
    } catch (e) {
      print('❌ Error in event recognition: $e');
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
