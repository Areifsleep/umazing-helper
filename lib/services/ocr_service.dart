// lib/services/ocr_service.dart
import 'dart:typed_data';
import 'dart:ui';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';

class OCRService {
  static final _textRecognizer = TextRecognizer(
    script: TextRecognitionScript.japanese, // Important for Uma Musume
  );

  static Future<List<String>> extractTextFromImage(Uint8List imageData) async {
    try {
      // Convert bytes to InputImage
      final inputImage = InputImage.fromBytes(
        bytes: imageData,
        metadata: InputImageMetadata(
          size: Size(1080, 2304), // Your screen size
          rotation: InputImageRotation.rotation0deg,
          format: InputImageFormat.nv21,
          bytesPerRow: 1080 * 4, // Adjust based on your format
        ),
      );

      // Perform OCR
      final RecognizedText recognizedText = await _textRecognizer.processImage(
        inputImage,
      );

      // Extract all text lines
      List<String> extractedTexts = [];

      for (TextBlock block in recognizedText.blocks) {
        for (TextLine line in block.lines) {
          final text = line.text.trim();
          if (text.isNotEmpty) {
            extractedTexts.add(text);
            print('📝 OCR found: "$text"');
          }
        }
      }

      return extractedTexts;
    } catch (e) {
      print('❌ OCR Error: $e');
      return [];
    }
  }

  static Future<void> dispose() async {
    await _textRecognizer.close();
  }
}
