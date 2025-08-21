// lib/services/recognition_data_service.dart
import 'dart:convert';
import 'package:flutter/services.dart';

class RecognitionDataService {
  static List<dynamic>? _cachedSupportCards;
  static List<dynamic>? _cachedUmaData;

  /// Load support card recognition data
  static Future<List<dynamic>> getSupportCardData() async {
    if (_cachedSupportCards == null) {
      final jsonString = await rootBundle.loadString(
        'assets/data/support_card.json',
      );
      _cachedSupportCards = json.decode(jsonString) as List;
      print(
        '✅ Support card events loaded: ${_cachedSupportCards!.length} events',
      );
    }
    return _cachedSupportCards!;
  }

  /// Load uma recognition data
  static Future<List<dynamic>> getUmaData() async {
    if (_cachedUmaData == null) {
      final jsonString = await rootBundle.loadString(
        'assets/data/uma_data.json',
      );
      _cachedUmaData = json.decode(jsonString) as List;
      print('✅ Uma characters loaded: ${_cachedUmaData!.length} characters');
    }
    return _cachedUmaData!;
  }

  /// Search for support card event by event name
  static Future<Map<String, dynamic>?> findSupportCardEvent(
    String eventName,
  ) async {
    final events = await getSupportCardData();

    try {
      return events.firstWhere(
        (event) =>
            event['EventName']?.toString().toLowerCase().contains(
              eventName.toLowerCase(),
            ) ??
            false,
      );
    } catch (e) {
      return null;
    }
  }

  /// Search for uma character by name
  static Future<Map<String, dynamic>?> findUmaCharacter(
    String characterName,
  ) async {
    final characters = await getUmaData();

    try {
      return characters.firstWhere(
        (character) =>
            character['UmaName']?.toString().toLowerCase().contains(
              characterName.toLowerCase(),
            ) ??
            false,
      );
    } catch (e) {
      return null;
    }
  }

  /// Search for uma character event by event name
  static Future<Map<String, dynamic>?> findUmaEvent(
    String eventName, {
    String? characterName,
  }) async {
    final characters = await getUmaData();

    for (var character in characters) {
      // If character name specified, only search in that character
      if (characterName != null &&
          !character['UmaName'].toString().toLowerCase().contains(
            characterName.toLowerCase(),
          )) {
        continue;
      }

      final events = character['UmaEvents'] as List? ?? [];

      try {
        final event = events.firstWhere(
          (event) =>
              event['EventName']?.toString().toLowerCase().contains(
                eventName.toLowerCase(),
              ) ??
              false,
        );

        // Add character context to the event
        return {
          ...event,
          'CharacterName': character['UmaName'],
          'CharacterObjectives': character['UmaObjectives'],
        };
      } catch (e) {
        continue;
      }
    }

    return null;
  }

  /// Get all event names from support cards (for recognition matching)
  static Future<List<String>> getAllSupportCardEventNames() async {
    final events = await getSupportCardData();
    return events
        .map((event) => event['EventName']?.toString() ?? '')
        .where((name) => name.isNotEmpty)
        .toSet() // Remove duplicates
        .toList();
  }

  /// Get all uma character names (for recognition matching)
  static Future<List<String>> getAllUmaCharacterNames() async {
    final characters = await getUmaData();
    return characters
        .map((character) => character['UmaName']?.toString() ?? '')
        .where((name) => name.isNotEmpty)
        .toList();
  }

  /// Get all uma event names (for recognition matching)
  static Future<List<String>> getAllUmaEventNames() async {
    final characters = await getUmaData();
    List<String> allEventNames = [];

    for (var character in characters) {
      final events = character['UmaEvents'] as List? ?? [];
      for (var event in events) {
        final eventName = event['EventName']?.toString();
        if (eventName != null && eventName.isNotEmpty) {
          allEventNames.add(eventName);
        }
      }
    }

    return allEventNames.toSet().toList(); // Remove duplicates
  }

  /// Get character objectives for training guidance
  static Future<List<Map<String, dynamic>>> getCharacterObjectives(
    String characterName,
  ) async {
    final character = await findUmaCharacter(characterName);
    if (character != null) {
      return List<Map<String, dynamic>>.from(character['UmaObjectives'] ?? []);
    }
    return [];
  }

  /// Parse event options to get recommendations
  static Map<String, String> parseEventOptions(Map<String, dynamic> eventData) {
    final eventOptions =
        eventData['EventOptions'] as Map<String, dynamic>? ?? {};
    Map<String, String> parsedOptions = {};

    eventOptions.forEach((key, value) {
      parsedOptions[key] = value.toString();
    });

    return parsedOptions;
  }

  /// Preload all data (call this on app startup)
  static Future<void> preloadAllData() async {
    try {
      await Future.wait([getSupportCardData(), getUmaData()]);

      final supportEvents = await getAllSupportCardEventNames();
      final umaCharacters = await getAllUmaCharacterNames();
      final umaEvents = await getAllUmaEventNames();

      print('✅ All recognition data preloaded successfully');
      print('   - Support card events: ${supportEvents.length}');
      print('   - Uma characters: ${umaCharacters.length}');
      print('   - Uma events: ${umaEvents.length}');
    } catch (e) {
      print('❌ Failed to preload data: $e');
      rethrow;
    }
  }
}
