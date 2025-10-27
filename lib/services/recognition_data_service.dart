// lib/services/recognition_data_service.dart
import 'dart:convert';
import 'package:flutter/services.dart';

class RecognitionDataService {
  static List<dynamic>? _cachedSupportCards;
  static List<dynamic>? _cachedUmaData;
  static List<dynamic>? _cachedCareerData;
  static List<dynamic>? _cachedRacesData;

  /// Selected Uma character for filtering (null = all characters)
  static String? selectedCharacter;

  /// Get list of all Uma character names for selection dropdown
  /// Returns unique character names (removes duplicates)
  static Future<List<String>> getAllCharacterNames() async {
    final characters = await getUmaData();
    Set<String> uniqueNames = {}; // Use Set to automatically remove duplicates

    for (var character in characters) {
      final name = character['UmaName']?.toString();
      if (name != null && name.isNotEmpty) {
        uniqueNames.add(name); // Set ignores duplicates
      }
    }

    List<String> names = uniqueNames.toList();
    names.sort(); // Sort alphabetically
    print(
      '✅ Loaded ${names.length} unique Uma character names (${characters.length} total entries)',
    );
    return names;
  }

  /// Set the selected character filter
  static void setSelectedCharacter(String? characterName) {
    selectedCharacter = characterName;
    if (characterName == null) {
      print('🔄 Character filter cleared (All Characters)');
    } else {
      print('🎯 Character filter set to: $characterName');
    }
  }

  /// Get the currently selected character
  static String? getSelectedCharacter() {
    return selectedCharacter;
  }

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
  /// Returns the FIRST matching event (legacy method)
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

  /// Search for support card events by event name
  /// Returns ALL matching events (since one EventName can have multiple option entries)
  static Future<List<Map<String, dynamic>>> findAllSupportCardEvents(
    String eventName,
  ) async {
    final events = await getSupportCardData();
    List<Map<String, dynamic>> matchingEvents = [];

    for (var event in events) {
      if (event['EventName']?.toString().toLowerCase().contains(
            eventName.toLowerCase(),
          ) ??
          false) {
        matchingEvents.add(event);
      }
    }

    print(
      '📋 Found ${matchingEvents.length} support card entries for "$eventName"',
    );
    return matchingEvents;
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
  /// Returns ALL matching events (since one EventName can have multiple options)
  static Future<List<Map<String, dynamic>>> findAllUmaEvents(
    String eventName, {
    String? characterName,
  }) async {
    final characters = await getUmaData();
    List<Map<String, dynamic>> matchingEvents = [];

    for (var character in characters) {
      // If character name specified, only search in that character
      if (characterName != null &&
          !character['UmaName'].toString().toLowerCase().contains(
            characterName.toLowerCase(),
          )) {
        continue;
      }

      final events = character['UmaEvents'] as List? ?? [];

      for (var event in events) {
        if (event['EventName']?.toString().toLowerCase().contains(
              eventName.toLowerCase(),
            ) ??
            false) {
          // Add character context to the event
          matchingEvents.add({
            ...event,
            'CharacterName': character['UmaName'],
            'CharacterObjectives': character['UmaObjectives'],
          });
        }
      }

      // If we found events for this character, don't search other characters
      if (matchingEvents.isNotEmpty) {
        break;
      }
    }

    return matchingEvents;
  }

  /// Legacy method for backward compatibility - returns first match
  static Future<Map<String, dynamic>?> findUmaEvent(
    String eventName, {
    String? characterName,
  }) async {
    final events = await findAllUmaEvents(
      eventName,
      characterName: characterName,
    );
    return events.isNotEmpty ? events.first : null;
  }

  static Future<List<dynamic>> getCareerData() async {
    if (_cachedCareerData == null) {
      final jsonString = await rootBundle.loadString('assets/data/career.json');
      _cachedCareerData = json.decode(jsonString) as List;
      print('✅ Career events loaded: ${_cachedCareerData!.length} events');
    }
    return _cachedCareerData!;
  }

  /// Load races data
  static Future<List<dynamic>> getRacesData() async {
    if (_cachedRacesData == null) {
      final jsonString = await rootBundle.loadString('assets/data/races.json');
      _cachedRacesData = json.decode(jsonString) as List;
      print('✅ Races loaded: ${_cachedRacesData!.length} races');
    }
    return _cachedRacesData!;
  }

  /// Search for career event by name
  static Future<Map<String, dynamic>?> findCareerEvent(String eventName) async {
    final events = await getCareerData();

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

  /// Search for race by name
  static Future<Map<String, dynamic>?> findRace(String raceName) async {
    final races = await getRacesData();

    try {
      return races.firstWhere(
        (race) =>
            race['RaceName']?.toString().toLowerCase().contains(
              raceName.toLowerCase(),
            ) ??
            false,
      );
    } catch (e) {
      return null;
    }
  }

  /// Get all career event names (for recognition matching)
  static Future<List<String>> getAllCareerEventNames() async {
    final events = await getCareerData();
    return events
        .map((event) => event['EventName']?.toString() ?? '')
        .where((name) => name.isNotEmpty)
        .toSet() // Remove duplicates
        .toList();
  }

  /// Get all race names (for recognition matching)
  static Future<List<String>> getAllRaceNames() async {
    final races = await getRacesData();
    return races
        .map((race) => race['RaceName']?.toString() ?? '')
        .where((name) => name.isNotEmpty)
        .toSet() // Remove duplicates
        .toList();
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

  /// Parse event options from Uma events, support cards, career events, or races
  /// Handles multiple entries with the same EventName (Uma events format)
  /// Options are returned in order: Top Option, Bottom Option, Option 1-5, Choice 1-10
  static Map<String, String> parseEventOptions(
    dynamic eventData, {
    bool isMultipleEntries = false,
  }) {
    Map<String, String> options = {};

    // If eventData is a List (multiple entries with same EventName)
    if (eventData is List) {
      print('📋 Parsing ${eventData.length} event entries');

      // Track which option types we've seen to maintain order
      Map<String, String> topOptions = {};
      Map<String, String> bottomOptions = {};
      Map<String, String> numberedOptions = {};
      Map<String, String> choiceOptions = {};

      for (var entry in eventData) {
        if (entry is Map<String, dynamic>) {
          final eventOptions = entry['EventOptions'];

          if (eventOptions is Map<String, dynamic>) {
            eventOptions.forEach((key, value) {
              // Clean up the effect text
              final cleanValue = value
                  .toString()
                  .replaceAll('\\r\\n', '\n')
                  .replaceAll('\\n', '\n')
                  .replaceAll('\r\n', '\n')
                  .replaceAll('\n', ', ')
                  .replaceAll(RegExp(r'\s+'), ' ')
                  .trim();

              // Categorize options by type for proper ordering
              if (key.toLowerCase().contains('top option')) {
                topOptions[key] = cleanValue;
              } else if (key.toLowerCase().contains('bottom option')) {
                bottomOptions[key] = cleanValue;
              } else if (RegExp(
                r'option\s*\d+',
                caseSensitive: false,
              ).hasMatch(key)) {
                numberedOptions[key] = cleanValue;
              } else if (RegExp(
                r'choice\s*\d+',
                caseSensitive: false,
              ).hasMatch(key)) {
                choiceOptions[key] = cleanValue;
              } else {
                // Other options go in numbered section
                numberedOptions[key] = cleanValue;
              }

              print(
                '   ✅ Found $key: ${cleanValue.length > 50 ? cleanValue.substring(0, 50) + "..." : cleanValue}',
              );
            });
          }
        }
      }

      // Add options in proper order: Top → Bottom → Options → Choices
      options.addAll(topOptions);
      options.addAll(bottomOptions);
      options.addAll(numberedOptions);
      options.addAll(choiceOptions);

      print(
        '📋 Total parsed ${options.length} options from ${eventData.length} entries (ordered: Top→Bottom→Options)',
      );
      return options;
    }

    // Single event entry (Map)
    if (eventData is! Map<String, dynamic>) {
      print('❌ Invalid event data type: ${eventData.runtimeType}');
      return options;
    }

    final Map<String, dynamic> eventMap = eventData;
    print('🔍 DEBUG: Event data keys: ${eventMap.keys.toList()}');

    // Try EventOptions format (Uma events)
    if (eventMap['EventOptions'] != null) {
      print('   ✅ Found EventOptions field');
      final eventOptions = eventMap['EventOptions'];

      if (eventOptions is Map<String, dynamic>) {
        eventOptions.forEach((key, value) {
          final cleanValue = value
              .toString()
              .replaceAll('\\r\\n', '\n')
              .replaceAll('\\n', '\n')
              .replaceAll('\r\n', '\n')
              .replaceAll('\n', ', ')
              .replaceAll(RegExp(r'\s+'), ' ')
              .trim();

          options[key] = cleanValue;
          print(
            '   ✅ Found $key: ${cleanValue.length > 50 ? cleanValue.substring(0, 50) + "..." : cleanValue}',
          );
        });
      }
    }

    // Try Choice1/Choice1Effect format (support cards)
    if (options.isEmpty) {
      print('   ⚠️ No EventOptions found, trying Choice1/Choice2 format...');

      for (int i = 1; i <= 10; i++) {
        final choiceKey = 'Choice$i';
        final effectKey = 'Choice${i}Effect';

        final choice = eventMap[choiceKey]?.toString();
        final effect = eventMap[effectKey]?.toString();

        if (choice != null && choice.isNotEmpty) {
          if (effect != null && effect.isNotEmpty) {
            final cleanEffect = effect
                .replaceAll('\\n', ', ')
                .replaceAll('\n', ', ')
                .replaceAll(RegExp(r'\s+'), ' ')
                .trim();
            options['Choice $i'] = '$choice: $cleanEffect';
          } else {
            options['Choice $i'] = choice;
          }
          print('   ✅ Found $choiceKey: $choice');
        }
      }
    }

    // Try Option1 format (fallback)
    if (options.isEmpty) {
      print('   ⚠️ No Choice1/Choice2 format found, trying Option1/Option2...');

      for (int i = 1; i <= 10; i++) {
        final optionKey = 'Option$i';
        final option = eventMap[optionKey]?.toString();
        if (option != null && option.isNotEmpty) {
          options['Option $i'] = option;
          print('   ✅ Found $optionKey: $option');
        }
      }
    }

    // Try generic "Options" array
    if (options.isEmpty && eventMap['Options'] != null) {
      print('   ⚠️ Trying Options array...');
      final optionsList = eventMap['Options'] as List?;
      if (optionsList != null) {
        for (int i = 0; i < optionsList.length; i++) {
          options['Choice ${i + 1}'] = optionsList[i].toString();
          print('   ✅ Found Option ${i + 1}: ${optionsList[i]}');
        }
      }
    }

    // Debug: Show potential option fields if still empty
    if (options.isEmpty) {
      print('   ⚠️ Still no options found, showing all fields...');
      eventMap.forEach((key, value) {
        print(
          '   🔍 Field: $key = ${value.toString().length > 100 ? value.toString().substring(0, 100) + "..." : value}',
        );
      });
    }

    print('📋 Parsed ${options.length} options from event');
    return options;
  }

  /// Preload all data (call this on app startup)
  static Future<void> preloadAllData() async {
    try {
      await Future.wait([
        getSupportCardData(),
        getUmaData(),
        getCareerData(),
        getRacesData(),
      ]);

      final supportEvents = await getAllSupportCardEventNames();
      final umaCharacters = await getAllUmaCharacterNames();
      final umaEvents = await getAllUmaEventNames();
      final careerEvents = await getAllCareerEventNames();
      final races = await getAllRaceNames();

      print('✅ All recognition data preloaded successfully');
      print('   - Support card events: ${supportEvents.length}');
      print('   - Uma characters: ${umaCharacters.length}');
      print('   - Uma events: ${umaEvents.length}');
      print('   - Career events: ${careerEvents.length}');
      print('   - Races: ${races.length}');
    } catch (e) {
      print('❌ Failed to preload data: $e');
      rethrow;
    }
  }
}
