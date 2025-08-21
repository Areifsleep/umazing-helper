// lib/services/screen_recognition_service.dart
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'recognition_data_service.dart';

class ScreenRecognitionService {
  static final TextRecognizer _textRecognizer = TextRecognizer(
    script: TextRecognitionScript.latin, // Important for Uma Musume
  );

  /// Analyze screenshot and find event by name
  static Future<RecognitionResult> analyzeScreenshot(
    Uint8List imageData,
  ) async {
    try {
      print('🔍 Starting screen recognition for English Uma Musume...');

      // Extract English text using Latin script OCR
      final extractedTexts = await _extractTextFromImage(imageData);

      if (extractedTexts.isEmpty) {
        return RecognitionResult.error('No English text found in screenshot');
      }

      print('📝 Found ${extractedTexts.length} English text elements');

      // Search in your English event data
      final supportCardResult = await _findSupportCardByEventName(
        extractedTexts,
      );
      if (supportCardResult != null) {
        return supportCardResult;
      }

      final umaEventResult = await _findUmaEventByEventName(extractedTexts);
      if (umaEventResult != null) {
        return umaEventResult;
      }

      return RecognitionResult.notRecognized(extractedTexts);
    } catch (e) {
      print('❌ Recognition error: $e');
      return RecognitionResult.error('Recognition failed: $e');
    }
  }

  /// Extract English text from image
  static Future<List<String>> _extractTextFromImage(Uint8List imageData) async {
    try {
      print('🔍 Starting English OCR text extraction...');
      print('📊 Image data size: ${imageData.length} bytes');

      // Save PNG to temp file and process
      final downloadsDir = Directory('/storage/emulated/0/Download');
      if (await downloadsDir.exists()) {
        final debugFile = File(
          '${downloadsDir.path}/debug_${DateTime.now().millisecondsSinceEpoch}.png',
        );
        await debugFile.writeAsBytes(imageData);
        print('💾 DEBUG: Check this image: ${debugFile.path}');
      }

      final tempDir = Directory.systemTemp;
      final tempFile = File(
        '${tempDir.path}/uma_screenshot_${DateTime.now().millisecondsSinceEpoch}.png',
      );

      await tempFile.writeAsBytes(imageData);
      print('💾 Saved temp file: ${tempFile.path}');

      // Create InputImage from file
      final inputImage = InputImage.fromFilePath(tempFile.path);

      // Process with ML Kit
      final recognizedText = await _textRecognizer.processImage(inputImage);

      // DEBUG: Print ALL detected text (before filtering)
      print('🔍 === RAW OCR RESULTS ===');
      print('📝 Total blocks found: ${recognizedText.blocks.length}');

      if (recognizedText.blocks.isEmpty) {
        print('❌ No text blocks detected at all!');
      }

      for (int i = 0; i < recognizedText.blocks.length; i++) {
        final block = recognizedText.blocks[i];
        print('📦 Block $i: "${block.text}"');
        print('   📍 Bounding box: ${block.boundingBox}');
        print('   🔤 Confidence: ${block.recognizedLanguages}');

        for (int j = 0; j < block.lines.length; j++) {
          final line = block.lines[j];
          print('   📄 Line $j: "${line.text}"');

          for (int k = 0; k < line.elements.length; k++) {
            final element = line.elements[k];
            print('     🔤 Element $k: "${element.text}"');
          }
        }
      }
      print('🔍 === END RAW OCR RESULTS ===');

      // Clean up temp file
      await tempFile.delete();
      print('🗑️ Temp file deleted');

      // Extract all text (before filtering)
      List<String> allTexts = [];

      for (TextBlock block in recognizedText.blocks) {
        final blockText = block.text.trim();
        if (blockText.isNotEmpty) {
          allTexts.add(blockText);
          print('📝 OCR Block: "$blockText"');
        }

        for (TextLine line in block.lines) {
          final lineText = line.text.trim();
          if (lineText.isNotEmpty && lineText != blockText) {
            allTexts.add(lineText);
            print('📝 OCR Line: "$lineText"');
          }

          for (TextElement element in line.elements) {
            final elementText = element.text.trim();
            if (elementText.isNotEmpty && elementText.length > 1) {
              allTexts.add(elementText);
              print('📝 OCR Element: "$elementText"');
            }
          }
        }
      }

      print('📊 Total texts before filtering: ${allTexts.length}');
      allTexts.forEach((text) => print('   - "$text"'));

      // Apply filtering
      final filteredTexts = _filterAndCleanTexts(allTexts);

      print('📊 Total texts after filtering: ${filteredTexts.length}');
      filteredTexts.forEach((text) => print('   ✅ "$text"'));

      return filteredTexts;
    } catch (e) {
      print('❌ English OCR extraction failed: $e');
      return [];
    }
  }

  /// Filter and clean extracted texts
  // Make filtering less aggressive for debugging
  static List<String> _filterAndCleanTexts(List<String> texts) {
    Set<String> uniqueTexts = {};

    for (String text in texts) {
      // DEBUG: Lower the minimum length requirement
      if (text.length < 2) {
        // Changed from 3 to 2
        print('🚫 Skipped too short: "$text"');
        continue;
      }

      if (_isCommonUIText(text)) {
        print('🚫 Skipped common UI: "$text"');
        continue;
      }

      // Clean and normalize text
      String cleaned = _cleanText(text);
      if (cleaned.length >= 2) {
        // Changed from 3 to 2
        uniqueTexts.add(cleaned);
        print('✅ Added: "$cleaned" (from "$text")');
      } else {
        print('🚫 Skipped after cleaning: "$text" → "$cleaned"');
      }
    }

    print('📊 Final unique texts: ${uniqueTexts.length}');
    return uniqueTexts.toList();
  }

  /// Check if text is common UI element (not event name)
  static bool _isCommonUIText(String text) {
    final commonTexts = [
      'ok', 'cancel', 'close', 'back', 'next', 'yes', 'no',
      'menu', 'settings', 'home', 'start', 'end', 'pause',
      'play', 'stop', 'continue', 'retry', 'quit', 'exit',
      // Remove single characters from common list for debugging
      // '×', '✓', '→', '←', '↑', '↓', '+', '-', '!', '?',
    ];

    String lowercaseText = text.toLowerCase();
    bool isCommon = commonTexts.contains(lowercaseText);

    if (isCommon) {
      print('🚫 Identified as common UI text: "$text"');
    }

    return isCommon;
  }

  /// Find support card event by matching event names
  static Future<RecognitionResult?> _findSupportCardByEventName(
    List<String> extractedTexts,
  ) async {
    try {
      print('🔍 Searching in support card events...');

      // Get all support card events
      final supportCardEvents =
          await RecognitionDataService.getSupportCardData();

      // Try to match each extracted text with event names
      for (String extractedText in extractedTexts) {
        for (var event in supportCardEvents) {
          final eventName = event['EventName']?.toString() ?? '';

          if (_isEventNameMatch(extractedText, eventName)) {
            print('✅ Found support card event: "$eventName"');
            print('   Matched with extracted text: "$extractedText"');

            // Parse event options
            final options = RecognitionDataService.parseEventOptions(event);

            return RecognitionResult.supportCardEvent(
              eventName: eventName,
              options: options,
              matchedText: extractedText,
              confidence: _calculateMatchConfidence(extractedText, eventName),
            );
          }
        }
      }

      print('❌ No support card events matched');
      return null;
    } catch (e) {
      print('❌ Error searching support card events: $e');
      return null;
    }
  }

  /// Find uma character event by matching event names
  static Future<RecognitionResult?> _findUmaEventByEventName(
    List<String> extractedTexts,
  ) async {
    try {
      print('🔍 Searching in uma character events...');

      // Get all uma characters and their events
      final umaCharacters = await RecognitionDataService.getUmaData();

      // Try to match each extracted text with event names
      for (String extractedText in extractedTexts) {
        for (var character in umaCharacters) {
          final characterName = character['UmaName']?.toString() ?? '';
          final events = character['UmaEvents'] as List? ?? [];

          for (var event in events) {
            final eventName = event['EventName']?.toString() ?? '';

            if (_isEventNameMatch(extractedText, eventName)) {
              print('✅ Found uma event: "$eventName"');
              print('   Character: "$characterName"');
              print('   Matched with extracted text: "$extractedText"');

              // Parse event options
              final options = RecognitionDataService.parseEventOptions(event);

              return RecognitionResult.umaEvent(
                eventName: eventName,
                characterName: characterName,
                options: options,
                matchedText: extractedText,
                confidence: _calculateMatchConfidence(extractedText, eventName),
              );
            }
          }
        }
      }

      print('❌ No uma events matched');
      return null;
    } catch (e) {
      print('❌ Error searching uma events: $e');
      return null;
    }
  }

  /// Check if extracted text matches event name
  static bool _isEventNameMatch(String extractedText, String eventName) {
    if (extractedText.isEmpty || eventName.isEmpty) return false;

    // Clean both texts for comparison
    final cleanExtracted = _cleanText(extractedText);
    final cleanEventName = _cleanText(eventName);

    // Skip very short texts
    if (cleanExtracted.length < 3 || cleanEventName.length < 3) return false;

    // 1. Exact match (highest priority)
    if (cleanExtracted == cleanEventName) {
      print('   ✅ Exact match: "$cleanExtracted" == "$cleanEventName"');
      return true;
    }

    // 2. One contains the other (high priority)
    if (cleanEventName.contains(cleanExtracted) && cleanExtracted.length >= 5) {
      print(
        '   ✅ Contains match: "$cleanEventName" contains "$cleanExtracted"',
      );
      return true;
    }

    if (cleanExtracted.contains(cleanEventName) && cleanEventName.length >= 5) {
      print(
        '   ✅ Contains match: "$cleanExtracted" contains "$cleanEventName"',
      );
      return true;
    }

    // 3. High similarity match (medium priority)
    final similarity = _calculateSimilarity(cleanExtracted, cleanEventName);
    if (similarity >= 0.85) {
      print(
        '   ✅ Similarity match: ${(similarity * 100).toInt()}% - "$cleanExtracted" ~ "$cleanEventName"',
      );
      return true;
    }

    // 4. Partial word match (low priority)
    if (_hasSignificantWordOverlap(cleanExtracted, cleanEventName)) {
      print('   ✅ Word overlap match: "$cleanExtracted" <-> "$cleanEventName"');
      return true;
    }

    return false;
  }

  /// Clean text for comparison
  static String _cleanText(String text) {
    return text
        .toLowerCase()
        .replaceAll(
          RegExp(r'[^\w\s\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]'),
          ' ',
        ) // Keep Japanese characters
        .replaceAll(RegExp(r'\s+'), ' ') // Normalize spaces
        .trim();
  }

  /// Calculate text similarity using Levenshtein distance
  static double _calculateSimilarity(String s1, String s2) {
    if (s1.isEmpty || s2.isEmpty) return 0.0;
    if (s1 == s2) return 1.0;

    final longer = s1.length > s2.length ? s1 : s2;
    final shorter = s1.length > s2.length ? s2 : s1;

    if (longer.isEmpty) return 1.0;

    final editDistance = _levenshteinDistance(longer, shorter);
    return (longer.length - editDistance) / longer.length;
  }

  /// Calculate Levenshtein distance between two strings
  static int _levenshteinDistance(String s1, String s2) {
    final matrix = List.generate(
      s1.length + 1,
      (i) => List.generate(s2.length + 1, (j) => 0),
    );

    for (int i = 0; i <= s1.length; i++) matrix[i][0] = i;
    for (int j = 0; j <= s2.length; j++) matrix[0][j] = j;

    for (int i = 1; i <= s1.length; i++) {
      for (int j = 1; j <= s2.length; j++) {
        final cost = s1[i - 1] == s2[j - 1] ? 0 : 1;
        matrix[i][j] = [
          matrix[i - 1][j] + 1, // deletion
          matrix[i][j - 1] + 1, // insertion
          matrix[i - 1][j - 1] + cost, // substitution
        ].reduce((a, b) => a < b ? a : b);
      }
    }

    return matrix[s1.length][s2.length];
  }

  /// Check for significant word overlap between texts
  static bool _hasSignificantWordOverlap(String text1, String text2) {
    final words1 = text1.split(' ').where((w) => w.length >= 2).toSet();
    final words2 = text2.split(' ').where((w) => w.length >= 2).toSet();

    if (words1.isEmpty || words2.isEmpty) return false;

    final intersection = words1.intersection(words2);
    final union = words1.union(words2);

    // Require at least 50% word overlap
    return (intersection.length / union.length) >= 0.5;
  }

  /// Calculate match confidence score
  static double _calculateMatchConfidence(
    String extractedText,
    String eventName,
  ) {
    final similarity = _calculateSimilarity(
      _cleanText(extractedText),
      _cleanText(eventName),
    );

    // Boost confidence for exact matches
    if (_cleanText(extractedText) == _cleanText(eventName)) {
      return 1.0;
    }

    // Boost confidence for contains matches
    if (_cleanText(eventName).contains(_cleanText(extractedText)) ||
        _cleanText(extractedText).contains(_cleanText(eventName))) {
      return 0.95;
    }

    return similarity;
  }

  /// Dispose OCR resources
  static Future<void> dispose() async {
    try {
      await _textRecognizer.close();
      print('✅ OCR resources disposed');
    } catch (e) {
      print('❌ Error disposing OCR resources: $e');
    }
  }
}

/// Result class for event recognition
class RecognitionResult {
  final String type;
  final String eventName;
  final String? characterName;
  final Map<String, String> options;
  final String? matchedText;
  final double? confidence;
  final List<String>? extractedTexts;
  final bool success;

  RecognitionResult({
    required this.type,
    required this.eventName,
    this.characterName,
    required this.options,
    this.matchedText,
    this.confidence,
    this.extractedTexts,
    required this.success,
  });

  factory RecognitionResult.supportCardEvent({
    required String eventName,
    required Map<String, String> options,
    required String matchedText,
    required double confidence,
  }) {
    return RecognitionResult(
      type: 'support_card',
      eventName: eventName,
      options: options,
      matchedText: matchedText,
      confidence: confidence,
      success: true,
    );
  }

  factory RecognitionResult.umaEvent({
    required String eventName,
    required String characterName,
    required Map<String, String> options,
    required String matchedText,
    required double confidence,
  }) {
    return RecognitionResult(
      type: 'uma_event',
      eventName: eventName,
      characterName: characterName,
      options: options,
      matchedText: matchedText,
      confidence: confidence,
      success: true,
    );
  }

  factory RecognitionResult.notRecognized(List<String> extractedTexts) {
    return RecognitionResult(
      type: 'not_found',
      eventName: 'Event not recognized',
      options: {},
      extractedTexts: extractedTexts,
      success: false,
    );
  }

  factory RecognitionResult.error(String message) {
    return RecognitionResult(
      type: 'error',
      eventName: 'Recognition Error',
      options: {'error': message},
      success: false,
    );
  }

  /// Get formatted recommendation text
  String getRecommendation() {
    if (!success) {
      if (type == 'not_found') {
        return 'Event not found in database. Extracted text:\n${extractedTexts?.join(", ") ?? "None"}';
      }
      return 'Recognition failed: $eventName';
    }

    final confidenceText = confidence != null
        ? ' (${(confidence! * 100).toInt()}% confidence)'
        : '';

    if (characterName != null) {
      return 'Found uma event: $eventName\nCharacter: $characterName$confidenceText';
    } else {
      return 'Found support card event: $eventName$confidenceText';
    }
  }

  /// Get formatted options text
  String getFormattedOptions() {
    if (options.isEmpty) return 'No options available';

    StringBuffer buffer = StringBuffer();
    options.forEach((key, value) {
      buffer.writeln('$key:');
      buffer.writeln(value.replaceAll(r'\r\n', '\n'));
      buffer.writeln();
    });

    return buffer.toString().trim();
  }

  /// Get best recommended option (simple logic)
  String? getBestOption() {
    if (options.isEmpty) return null;

    // Simple heuristic: prefer "Top Option" if available
    if (options.containsKey('Top Option')) {
      return 'Top Option';
    }

    // Otherwise return first option
    return options.keys.first;
  }

  String getExtractedTextsDebug() {
    if (extractedTexts == null || extractedTexts!.isEmpty) {
      return 'No text extracted';
    }
    return 'Extracted texts:\n${extractedTexts!.map((text) => '- "$text"').join('\n')}';
  }
}
