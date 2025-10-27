import 'package:flutter/material.dart';
import '../services/screen_capture_manager.dart';
import '../services/recognition_data_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ScreenCaptureScreen extends StatefulWidget {
  @override
  _ScreenCaptureScreenState createState() => _ScreenCaptureScreenState();

  /// Static method to show character selection modal from anywhere
  static Future<void> showCharacterSelectionModalGlobal(
    BuildContext context,
  ) async {
    try {
      final names = await RecognitionDataService.getAllCharacterNames();
      final currentSelection = RecognitionDataService.getSelectedCharacter();

      if (!context.mounted) return;

      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (BuildContext modalContext) {
          String? selectedInModal = currentSelection;

          return StatefulBuilder(
            builder: (BuildContext context, StateSetter setModalState) {
              return DraggableScrollableSheet(
                initialChildSize: 0.7,
                minChildSize: 0.5,
                maxChildSize: 0.95,
                expand: false,
                builder: (context, scrollController) {
                  return Column(
                    children: [
                      // Handle bar
                      Container(
                        margin: EdgeInsets.only(top: 8, bottom: 16),
                        width: 40,
                        height: 4,
                        decoration: BoxDecoration(
                          color: Colors.grey.shade300,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      // Title
                      Padding(
                        padding: EdgeInsets.symmetric(horizontal: 16),
                        child: Row(
                          children: [
                            Icon(Icons.person, color: Colors.blue, size: 28),
                            SizedBox(width: 12),
                            Text(
                              'Select Uma Character',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(height: 16),
                      // Character list
                      Expanded(
                        child: ListView.builder(
                          controller: scrollController,
                          itemCount: names.length + 1,
                          itemBuilder: (context, index) {
                            if (index == 0) {
                              // All Characters option
                              return ListTile(
                                leading: Icon(Icons.public, color: Colors.blue),
                                title: Text(
                                  'All Characters (No Filter)',
                                  style: TextStyle(fontWeight: FontWeight.bold),
                                ),
                                tileColor: selectedInModal == null
                                    ? Colors.blue.shade50
                                    : null,
                                onTap: () async {
                                  RecognitionDataService.setSelectedCharacter(
                                    null,
                                  );
                                  final prefs =
                                      await SharedPreferences.getInstance();
                                  await prefs.remove('selected_character');
                                  Navigator.pop(modalContext);

                                  // Show snackbar
                                  if (context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(
                                          'Filter removed - Searching all characters',
                                        ),
                                        duration: Duration(seconds: 2),
                                        backgroundColor: Colors.blue,
                                      ),
                                    );
                                  }
                                },
                              );
                            }

                            final characterName = names[index - 1];
                            return ListTile(
                              leading: Icon(
                                Icons.person_outline,
                                color: Colors.grey,
                              ),
                              title: Text(characterName),
                              tileColor: selectedInModal == characterName
                                  ? Colors.blue.shade50
                                  : null,
                              trailing: selectedInModal == characterName
                                  ? Icon(Icons.check, color: Colors.green)
                                  : null,
                              onTap: () async {
                                RecognitionDataService.setSelectedCharacter(
                                  characterName,
                                );
                                final prefs =
                                    await SharedPreferences.getInstance();
                                await prefs.setString(
                                  'selected_character',
                                  characterName,
                                );
                                Navigator.pop(modalContext);

                                // Show snackbar
                                if (context.mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(
                                        'Filtering events for: $characterName',
                                      ),
                                      duration: Duration(seconds: 2),
                                      backgroundColor: Colors.green,
                                    ),
                                  );
                                }
                              },
                            );
                          },
                        ),
                      ),
                    ],
                  );
                },
              );
            },
          );
        },
      );
    } catch (e) {
      print('❌ Error showing character modal: $e');
    }
  }

  /// Static callback to update character selection from external sources (e.g., Android overlay)
  static void Function(String?)? _onCharacterSelectionUpdated;

  /// Static method to update character selection from Android overlay
  static void updateCharacterSelectionGlobal(String? character) {
    if (_onCharacterSelectionUpdated != null) {
      _onCharacterSelectionUpdated!(character);
    } else {
      print('⚠️ No UI update callback registered');
    }
  }
}

class _ScreenCaptureScreenState extends State<ScreenCaptureScreen> {
  String _status = 'Not initialized';
  bool _isOverlayRunning = false;

  // Character selection
  List<String> _characterNames = [];
  String? _selectedCharacter; // null = All Characters
  bool _isLoadingCharacters = true;

  @override
  void initState() {
    super.initState();
    _initializeService();
    _loadCharacterNames();
    _loadSavedCharacter();

    // Register callback for external updates
    ScreenCaptureScreen._onCharacterSelectionUpdated = (character) {
      setState(() {
        _selectedCharacter = character;
      });
      print('🔄 UI updated from Android: ${character ?? "All Characters"}');
    };
  }

  @override
  void dispose() {
    // Unregister callback
    ScreenCaptureScreen._onCharacterSelectionUpdated = null;
    super.dispose();
  }

  /// Load all Uma character names for dropdown
  Future<void> _loadCharacterNames() async {
    try {
      final names = await RecognitionDataService.getAllCharacterNames();
      setState(() {
        _characterNames = names;
        _isLoadingCharacters = false;
      });
    } catch (e) {
      print('❌ Error loading character names: $e');
      setState(() => _isLoadingCharacters = false);
    }
  }

  /// Load saved character selection from preferences
  Future<void> _loadSavedCharacter() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getString('selected_character');
      if (saved != null && saved.isNotEmpty && saved != 'All Characters') {
        setState(() => _selectedCharacter = saved);
        RecognitionDataService.setSelectedCharacter(saved);
        print('✅ Loaded saved character: $saved');
      }
    } catch (e) {
      print('❌ Error loading saved character: $e');
    }
  }

  /// Save character selection to preferences
  Future<void> _saveCharacterSelection(String? character) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (character == null || character == 'All Characters') {
        await prefs.remove('selected_character');
      } else {
        await prefs.setString('selected_character', character);
      }
    } catch (e) {
      print('❌ Error saving character selection: $e');
    }
  }

  /// Handle character selection change
  void _onCharacterChanged(String? newValue) {
    setState(() {
      if (newValue == 'All Characters') {
        _selectedCharacter = null;
        RecognitionDataService.setSelectedCharacter(null);
      } else {
        _selectedCharacter = newValue;
        RecognitionDataService.setSelectedCharacter(newValue);
      }
    });
    _saveCharacterSelection(_selectedCharacter);
  }

  Future<void> _initializeService() async {
    setState(() => _status = 'Initializing...');

    final success = await ScreenCaptureManager.initialize();
    if (success) {
      _checkPermissions();
    } else {
      setState(() => _status = 'Failed to initialize');
    }
  }

  Future<void> _checkPermissions() async {
    final permissions = await ScreenCaptureManager.checkPermissions();
    setState(() {
      switch (permissions) {
        case PermissionStatus.granted:
          _status = 'Ready - All permissions granted';
          break;
        case PermissionStatus.screenCaptureNeeded:
          _status = 'Screen capture permission needed';
          break;
        case PermissionStatus.overlayNeeded:
          _status = 'Overlay permission needed';
          break;
        case PermissionStatus.denied:
          _status = 'Permissions denied';
          break;
      }
    });
  }

  Future<void> _requestScreenCapturePermission() async {
    setState(() => _status = 'Requesting screen capture permission...');

    final granted = await ScreenCaptureManager.requestScreenCapturePermission();
    if (granted) {
      _checkPermissions();
    } else {
      setState(() => _status = 'Screen capture permission denied');
    }
  }

  Future<void> _toggleOverlay() async {
    if (_isOverlayRunning) {
      final success = await ScreenCaptureManager.stopOverlay();
      if (success) {
        setState(() {
          _isOverlayRunning = false;
          _status = 'Overlay stopped';
        });
      }
    } else {
      final success = await ScreenCaptureManager.startOverlay();
      if (success) {
        setState(() {
          _isOverlayRunning = true;
          _status = 'Overlay started';
        });
      } else {
        setState(() => _status = 'Failed to start overlay');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Umazing Helper')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Character Selection Card
            Card(
              color: Colors.blue.shade50,
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.person, color: Colors.blue),
                        SizedBox(width: 8),
                        Text(
                          'Select Uma Character',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 12),
                    _isLoadingCharacters
                        ? Center(child: CircularProgressIndicator())
                        : Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              RawAutocomplete<String>(
                                initialValue: TextEditingValue(
                                  text: _selectedCharacter ?? '',
                                ),
                                optionsBuilder:
                                    (TextEditingValue textEditingValue) {
                                      if (textEditingValue.text.isEmpty) {
                                        return [
                                          '🌐 All Characters (No Filter)',
                                        ].followedBy(_characterNames);
                                      }
                                      // Filter characters based on search text
                                      return _characterNames.where((
                                        String option,
                                      ) {
                                        return option.toLowerCase().contains(
                                          textEditingValue.text.toLowerCase(),
                                        );
                                      });
                                    },
                                onSelected: (String selection) {
                                  if (selection ==
                                      '🌐 All Characters (No Filter)') {
                                    _onCharacterChanged(null);
                                  } else {
                                    _onCharacterChanged(selection);
                                  }
                                },
                                fieldViewBuilder:
                                    (
                                      BuildContext context,
                                      TextEditingController
                                      textEditingController,
                                      FocusNode focusNode,
                                      VoidCallback onFieldSubmitted,
                                    ) {
                                      // Update text controller when selection changes
                                      if (_selectedCharacter != null &&
                                          textEditingController.text !=
                                              _selectedCharacter) {
                                        textEditingController.text =
                                            _selectedCharacter!;
                                      } else if (_selectedCharacter == null &&
                                          textEditingController
                                              .text
                                              .isNotEmpty &&
                                          !focusNode.hasFocus) {
                                        textEditingController.clear();
                                      }

                                      return TextField(
                                        controller: textEditingController,
                                        focusNode: focusNode,
                                        readOnly:
                                            _selectedCharacter !=
                                            null, // Make read-only when character is selected
                                        decoration: InputDecoration(
                                          labelText: 'Character Filter',
                                          helperText:
                                              'Type to search or select from list',
                                          hintText:
                                              _selectedCharacter ??
                                              'All Characters',
                                          prefixIcon: Icon(Icons.search),
                                          // Always show clear button
                                          suffixIcon: IconButton(
                                            icon: Icon(Icons.clear),
                                            onPressed: () {
                                              textEditingController.clear();
                                              _onCharacterChanged(null);
                                              focusNode
                                                  .unfocus(); // Close keyboard
                                            },
                                          ),
                                          border: OutlineInputBorder(),
                                          filled: true,
                                          fillColor: Colors.white,
                                        ),
                                        onTap: () {
                                          // If a character is selected, clear it to allow searching
                                          if (_selectedCharacter != null) {
                                            textEditingController.clear();
                                            _onCharacterChanged(null);
                                          }
                                        },
                                      );
                                    },
                                optionsViewBuilder:
                                    (
                                      BuildContext context,
                                      AutocompleteOnSelected<String> onSelected,
                                      Iterable<String> options,
                                    ) {
                                      return Align(
                                        alignment: Alignment.topLeft,
                                        child: Material(
                                          elevation: 4.0,
                                          child: ConstrainedBox(
                                            constraints: BoxConstraints(
                                              maxHeight:
                                                  300, // Increased from 200
                                              maxWidth:
                                                  MediaQuery.of(
                                                    context,
                                                  ).size.width -
                                                  32,
                                            ),
                                            child: ListView.builder(
                                              padding: EdgeInsets.zero,
                                              shrinkWrap: true,
                                              itemCount: options.length,
                                              itemBuilder:
                                                  (
                                                    BuildContext context,
                                                    int index,
                                                  ) {
                                                    final String option =
                                                        options.elementAt(
                                                          index,
                                                        );
                                                    final bool isAllCharacters =
                                                        option.startsWith('🌐');

                                                    return ListTile(
                                                      leading: isAllCharacters
                                                          ? Icon(
                                                              Icons.public,
                                                              color:
                                                                  Colors.blue,
                                                            )
                                                          : Icon(
                                                              Icons
                                                                  .person_outline,
                                                              color:
                                                                  Colors.grey,
                                                            ),
                                                      title: Text(
                                                        option,
                                                        style: TextStyle(
                                                          fontWeight:
                                                              isAllCharacters
                                                              ? FontWeight.bold
                                                              : FontWeight
                                                                    .normal,
                                                        ),
                                                      ),
                                                      tileColor:
                                                          option ==
                                                              _selectedCharacter
                                                          ? Colors.blue.shade50
                                                          : null,
                                                      onTap: () {
                                                        onSelected(option);
                                                      },
                                                    );
                                                  },
                                            ),
                                          ),
                                        ),
                                      );
                                    },
                              ),
                            ],
                          ),
                    if (_selectedCharacter != null) ...[
                      SizedBox(height: 8),
                      Container(
                        padding: EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.green.shade100,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              Icons.check_circle,
                              color: Colors.green,
                              size: 16,
                            ),
                            SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Filtering events for: $_selectedCharacter',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.green.shade800,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),

            SizedBox(height: 16),

            // Status
            Card(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Text('Status: $_status', style: TextStyle(fontSize: 16)),
              ),
            ),

            SizedBox(height: 16),

            // Buttons
            ElevatedButton(
              onPressed: _checkPermissions,
              child: Text('Check Permissions'),
            ),

            ElevatedButton(
              onPressed: _requestScreenCapturePermission,
              child: Text('Request Screen Capture Permission'),
            ),

            ElevatedButton(
              onPressed: _toggleOverlay,
              child: Text(_isOverlayRunning ? 'Stop Overlay' : 'Start Overlay'),
            ),
          ],
        ),
      ),
    );
  }
}
