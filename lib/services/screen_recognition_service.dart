// lib/services/screen_recognition_service.dart (Optimized - Single Best Algorithm)
import 'dart:async';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'dart:io';
import 'package:flutter/rendering.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'recognition_data_service.dart';

class ScreenRecognitionService {
  // ✅ OPTIMIZATION 1: Cache TextRecognizer (reuse instead of recreate)
  static TextRecognizer? _textRecognizer;

  static TextRecognizer get textRecognizer {
    _textRecognizer ??= TextRecognizer(script: TextRecognitionScript.latin);
    return _textRecognizer!;
  }

  /// Fast analysis with optimized fuzzy matching
  static Future<RecognitionResult> analyzeScreenshot(
    Uint8List imageData, {
    required int width,
    required int height,
    required String format,
  }) async {
    try {
      final startTime = DateTime.now();
      print(
        '🔍 Starting optimized recognition (${width}x${height} $format)...',
      );

      // Extract complete text phrases
      final extractedPhrases = await _extractTextPhrases(
        imageData,
        width: width,
        height: height,
        format: format,
      );

      if (extractedPhrases.isEmpty) {
        return RecognitionResult.error('No text found in screenshot');
      }

      print('📝 Found ${extractedPhrases.length} phrases');
      extractedPhrases.forEach((phrase) => print('   📄 "$phrase"'));

      // Find best match using single optimized algorithm
      final bestMatch = await _findBestMatch(extractedPhrases);

      final duration = DateTime.now().difference(startTime).inMilliseconds;
      print('⚡ Total recognition completed in ${duration}ms');

      return bestMatch ?? RecognitionResult.notRecognized(extractedPhrases);
    } catch (e) {
      print('❌ Recognition error: $e');
      return RecognitionResult.error('Recognition failed: $e');
    }
  }

  /// Extract complete text phrases from image
  static Future<List<String>> _extractTextPhrases(
    Uint8List imageData, {
    required int width,
    required int height,
    required String format,
  }) async {
    try {
      final startTime = DateTime.now();

      // ✅ OPTIMIZATION 4: Convert RGBA bytes to PNG in memory (still faster than file I/O)
      // ML Kit works best with PNG format
      final pngBytes = await _rgbaToPng(imageData, width, height);

      // Write to temp file for ML Kit
      final tempDir = Directory.systemTemp;
      final tempFile = File(
        '${tempDir.path}/uma_screenshot_${DateTime.now().millisecondsSinceEpoch}.png',
      );
      await tempFile.writeAsBytes(pngBytes);

      final inputImage = InputImage.fromFilePath(tempFile.path);
      // ✅ Use cached recognizer instead of creating new one
      final recognizedText = await textRecognizer.processImage(inputImage);

      await tempFile.delete();

      // Extract only complete blocks (phrases)
      List<String> phrases = [];
      for (TextBlock block in recognizedText.blocks) {
        final cleanedText = _cleanText(block.text.trim());
        if (cleanedText.isNotEmpty && cleanedText.length >= 3) {
          phrases.add(cleanedText);
        }
      }

      final duration = DateTime.now().difference(startTime).inMilliseconds;
      print('⚡ OCR extraction completed in ${duration}ms');

      return phrases;
    } catch (e) {
      print('❌ Error extracting phrases: $e');
      return [];
    }
  }

  /// Convert RGBA bytes to PNG format
  static Future<Uint8List> _rgbaToPng(
    Uint8List rgbaBytes,
    int width,
    int height,
  ) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);

    // Decode RGBA bytes to image
    final completer = Completer<ui.Image>();
    ui.decodeImageFromPixels(
      rgbaBytes,
      width,
      height,
      ui.PixelFormat.rgba8888,
      (ui.Image image) {
        completer.complete(image);
      },
    );

    final image = await completer.future;

    // Draw image to canvas
    canvas.drawImage(image, Offset.zero, Paint());

    // Convert to PNG
    final picture = recorder.endRecording();
    final img = await picture.toImage(width, height);
    final byteData = await img.toByteData(format: ui.ImageByteFormat.png);

    image.dispose();
    img.dispose();
    picture.dispose();

    return byteData!.buffer.asUint8List();
  }

  /// Clean and normalize text
  static String _cleanText(String text) {
    return text
        .toLowerCase()
        .replaceAll(RegExp(r'[^\w\s]'), ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  /// Find best match using single optimized algorithm
  static Future<RecognitionResult?> _findBestMatch(
    List<String> extractedPhrases,
  ) async {
    try {
      final startTime = DateTime.now();

      final supportCardEvents =
          await RecognitionDataService.getSupportCardData();
      final umaCharacters = await RecognitionDataService.getUmaData();
      final careerEvents = await RecognitionDataService.getCareerData();
      final races = await RecognitionDataService.getRacesData();

      String? bestEventName;
      Map<String, dynamic>? bestEventData;
      String? bestType;
      String? bestCharacterName;
      String bestMatchedText = '';
      double highestSimilarity = 0.6; // 60% threshold

      // ✅ OPTIMIZATION 2: Early exit on perfect match (saves time)
      bool foundPerfectMatch = false;

      // Check support card events
      for (var event in supportCardEvents) {
        if (foundPerfectMatch) break; // ✅ Stop if perfect match found

        final eventName = event['EventName']?.toString() ?? '';
        if (eventName.isNotEmpty) {
          final match = _getBestSimilarity(extractedPhrases, eventName);
          if (match.similarity > highestSimilarity) {
            highestSimilarity = match.similarity;
            bestEventName = eventName;
            bestEventData = event;
            bestType = 'support_card';
            bestMatchedText = match.matchedText;

            if (match.similarity >= 0.99) {
              // 99%+ is perfect match
              foundPerfectMatch = true;
            }
          }
        }
      }

      // Check career events
      if (!foundPerfectMatch) {
        // ✅ Skip if perfect match found
        for (var event in careerEvents) {
          if (foundPerfectMatch) break;

          final eventName = event['EventName']?.toString() ?? '';
          if (eventName.isNotEmpty) {
            final match = _getBestSimilarity(extractedPhrases, eventName);
            if (match.similarity > highestSimilarity) {
              highestSimilarity = match.similarity;
              bestEventName = eventName;
              bestEventData = event;
              bestType = 'career_event';
              bestMatchedText = match.matchedText;

              if (match.similarity >= 0.99) {
                foundPerfectMatch = true;
              }
            }
          }
        }
      }

      // Check races
      if (!foundPerfectMatch) {
        // ✅ Skip if perfect match found
        for (var race in races) {
          if (foundPerfectMatch) break;

          final raceName = race['RaceName']?.toString() ?? '';
          if (raceName.isNotEmpty) {
            final match = _getBestSimilarity(extractedPhrases, raceName);
            if (match.similarity > highestSimilarity) {
              highestSimilarity = match.similarity;
              bestEventName = raceName;
              bestEventData = race;
              bestType = 'race';
              bestMatchedText = match.matchedText;

              if (match.similarity >= 0.99) {
                foundPerfectMatch = true;
              }
            }
          }
        }
      }

      // Check uma character events
      if (!foundPerfectMatch) {
        // ✅ Skip if perfect match found
        for (var character in umaCharacters) {
          if (foundPerfectMatch) break;

          final characterName = character['UmaName']?.toString() ?? '';
          final events = character['UmaEvents'] as List? ?? [];

          for (var event in events) {
            if (foundPerfectMatch) break;

            final eventName = event['EventName']?.toString() ?? '';
            if (eventName.isNotEmpty) {
              final match = _getBestSimilarity(extractedPhrases, eventName);
              if (match.similarity > highestSimilarity) {
                highestSimilarity = match.similarity;
                bestEventName = eventName;
                bestEventData = event;
                bestType = 'uma_event';
                bestCharacterName = characterName;
                bestMatchedText = match.matchedText;

                if (match.similarity >= 0.99) {
                  foundPerfectMatch = true;
                }
              }
            }
          }
        }
      }

      final duration = DateTime.now().difference(startTime).inMilliseconds;
      print('⚡ Matching completed in ${duration}ms');

      if (bestEventName == null || bestEventData == null || bestType == null) {
        print('❌ No events found with >60% similarity');
        return null;
      }

      print(
        '🎯 Best match: "$bestEventName" (${(highestSimilarity * 100).toInt()}%)',
      );

      // For uma_events and support_cards, get ALL matching events (multiple entries with same name)
      Map<String, String> options;
      if (bestType == 'uma_event') {
        final allMatchingEvents = await RecognitionDataService.findAllUmaEvents(
          bestEventName,
          characterName: bestCharacterName,
        );
        print(
          '📋 Found ${allMatchingEvents.length} uma event entries for "$bestEventName"',
        );
        options = RecognitionDataService.parseEventOptions(allMatchingEvents);
      } else if (bestType == 'support_card') {
        final allMatchingEvents =
            await RecognitionDataService.findAllSupportCardEvents(
              bestEventName,
            );
        print(
          '📋 Found ${allMatchingEvents.length} support card entries for "$bestEventName"',
        );
        options = RecognitionDataService.parseEventOptions(allMatchingEvents);
      } else {
        options = RecognitionDataService.parseEventOptions(bestEventData);
      }

      // Return result based on type
      if (bestType == 'support_card') {
        return RecognitionResult.supportCardEvent(
          eventName: bestEventName,
          options: options,
          matchedText: bestMatchedText,
          confidence: highestSimilarity,
        );
      } else if (bestType == 'career_event') {
        return RecognitionResult.careerEvent(
          eventName: bestEventName,
          options: options,
          matchedText: bestMatchedText,
          confidence: highestSimilarity,
        );
      } else if (bestType == 'race') {
        return RecognitionResult.race(
          raceName: bestEventName,
          raceData: bestEventData,
          matchedText: bestMatchedText,
          confidence: highestSimilarity,
        );
      } else {
        return RecognitionResult.umaEvent(
          eventName: bestEventName,
          characterName: bestCharacterName ?? 'Unknown',
          options: options,
          matchedText: bestMatchedText,
          confidence: highestSimilarity,
        );
      }
    } catch (e) {
      print('❌ Error finding best match: $e');
      return null;
    }
  }

  /// Get best similarity using optimized algorithm
  static MatchResult _getBestSimilarity(
    List<String> extractedPhrases,
    String eventName,
  ) {
    double bestSimilarity = 0.0;
    String bestMatchedText = '';

    final cleanEventName = _cleanText(eventName);

    for (String phrase in extractedPhrases) {
      final similarity = _calculateOptimizedSimilarity(phrase, cleanEventName);
      if (similarity > bestSimilarity) {
        bestSimilarity = similarity;
        bestMatchedText = phrase;
      }
    }

    return MatchResult(
      similarity: bestSimilarity,
      matchedText: bestMatchedText,
    );
  }

  /// Single optimized similarity algorithm (fastest + most accurate)
  static double _calculateOptimizedSimilarity(String text1, String text2) {
    if (text1.isEmpty || text2.isEmpty) return 0.0;
    if (text1 == text2) return 1.0;

    // 1. Exact match
    if (text1 == text2) return 1.0;

    // 2. Contains match (very fast and accurate for event names)
    if (text1.contains(text2) || text2.contains(text1)) {
      final shorter = text1.length < text2.length ? text1 : text2;
      final longer = text1.length < text2.length ? text2 : text1;
      return 0.8 + (shorter.length / longer.length) * 0.2;
    }

    // 3. Word overlap (best for multi-word event names)
    final words1 = text1.split(' ').where((w) => w.length > 2).toSet();
    final words2 = text2.split(' ').where((w) => w.length > 2).toSet();

    if (words1.isNotEmpty && words2.isNotEmpty) {
      final intersection = words1.intersection(words2);
      final union = words1.union(words2);
      final wordSimilarity = intersection.length / union.length;

      if (wordSimilarity > 0.5)
        return wordSimilarity * 0.9; // High confidence for word matches
    }

    // 4. Simple character-based similarity (fallback)
    final shorter = text1.length < text2.length ? text1 : text2;
    final longer = text1.length < text2.length ? text2 : text1;

    int matches = 0;
    for (int i = 0; i < shorter.length; i++) {
      if (i < longer.length && shorter[i] == longer[i]) {
        matches++;
      }
    }

    return matches / longer.length;
  }

  /// Dispose OCR resources
  static Future<void> dispose() async {
    try {
      await _textRecognizer?.close();
      _textRecognizer = null;
      print('✅ OCR resources disposed');
    } catch (e) {
      print('❌ Error disposing OCR resources: $e');
    }
  }
}

/// Simplified helper class
class MatchResult {
  final double similarity;
  final String matchedText;

  MatchResult({required this.similarity, required this.matchedText});
}

/// RecognitionResult class (keep existing implementation)
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

  factory RecognitionResult.careerEvent({
    required String eventName,
    required Map<String, String> options,
    required String matchedText,
    required double confidence,
  }) {
    return RecognitionResult(
      type: 'career_event',
      eventName: eventName,
      options: options,
      matchedText: matchedText,
      confidence: confidence,
      success: true,
    );
  }

  factory RecognitionResult.race({
    required String raceName,
    required Map<String, dynamic> raceData,
    required String matchedText,
    required double confidence,
  }) {
    return RecognitionResult(
      type: 'race',
      eventName: raceName,
      options: {
        'Grade': raceData['Grade']?.toString() ?? '',
        'Terrain': raceData['Terrain']?.toString() ?? '',
        'Distance': raceData['DistanceMeter']?.toString() ?? '',
        'Season': raceData['Season']?.toString() ?? '',
        'Fans Required': raceData['FansRequired']?.toString() ?? '',
        'Fans Gained': raceData['FansGained']?.toString() ?? '',
      },
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

  String getRecommendation() {
    if (!success) {
      if (type == 'not_found') {
        return 'Event not found. Extracted: ${extractedTexts?.join(", ") ?? "None"}';
      }
      return 'Recognition failed: $eventName';
    }

    final confidenceText = confidence != null
        ? ' (${(confidence! * 100).toInt()}% confidence)'
        : '';

    if (type == 'career_event') {
      return 'Career event: $eventName$confidenceText';
    } else if (type == 'race') {
      return 'Race: $eventName$confidenceText';
    } else if (characterName != null) {
      return 'Uma event: $eventName\nCharacter: $characterName$confidenceText';
    } else {
      return 'Support card event: $eventName$confidenceText';
    }
  }
}
