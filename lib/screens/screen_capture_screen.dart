import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/screen_capture_manager.dart';
import '../services/recognition_data_service.dart';
import '../services/scan_button_preferences.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'settings_screen.dart';

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
    await ScreenCaptureManager.initialize();
  }

  Future<void> _toggleOverlay() async {
    if (_isOverlayRunning) {
      // Stop the overlay
      final success = await ScreenCaptureManager.stopOverlay();
      if (success) {
        setState(() {
          _isOverlayRunning = false;
        });
      }
    } else {
      // Check permissions first
      final permissions = await ScreenCaptureManager.checkPermissions();

      // Request screen capture permission if needed
      if (permissions == PermissionStatus.screenCaptureNeeded ||
          permissions == PermissionStatus.denied) {
        final granted =
            await ScreenCaptureManager.requestScreenCapturePermission();
        if (!granted) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('❌ Screen capture permission is required'),
                backgroundColor: Colors.red,
                duration: Duration(seconds: 3),
              ),
            );
          }
          return;
        }
      }

      // Check overlay permission
      if (permissions == PermissionStatus.overlayNeeded) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('❌ Please grant overlay permission in Settings'),
              backgroundColor: Colors.red,
              duration: Duration(seconds: 3),
              action: SnackBarAction(
                label: 'Open Settings',
                textColor: Colors.white,
                onPressed: () {
                  // The startOverlay call will open settings if permission not granted
                  ScreenCaptureManager.startOverlay();
                },
              ),
            ),
          );
        }
        return;
      }

      // All permissions granted, start overlay
      // Load button settings BEFORE starting overlay
      final prefs = await ScanButtonPreferences.getAllPreferences();
      final size = prefs['size']!;
      final opacity = prefs['opacity']!;

      print(
        '📤 Sending button settings to Android: size=$size, opacity=$opacity',
      );

      final success = await ScreenCaptureManager.startOverlay();
      if (success) {
        setState(() {
          _isOverlayRunning = true;
        });

        // Wait a bit for overlay to be fully initialized
        await Future.delayed(Duration(milliseconds: 100));

        // Send button appearance settings to Android overlay immediately after starting
        try {
          const platform = MethodChannel('uma_screen_capture');
          await platform.invokeMethod('updateScanButtonAppearance', {
            'size': size,
            'opacity': opacity,
          });
          print(
            '✅ Button settings sent to overlay: ${size}dp, ${(opacity * 100).toInt()}%',
          );
        } catch (e) {
          print('⚠️ Failed to send button settings: $e');
        }
      }
    }
  }

  /// Launch Uma Musume app if installed
  Future<void> _launchUmaMusume() async {
    const platform = MethodChannel('uma_screen_capture');
    try {
      final result = await platform.invokeMethod('launchUmaMusume');
      if (result == true) {
        print('✅ Uma Musume app launched');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('🎮 Launching Uma Musume...'),
              backgroundColor: Colors.green,
              duration: Duration(seconds: 2),
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('❌ Uma Musume app not found'),
                  SizedBox(height: 4),
                  Text(
                    'Check logcat for detected packages',
                    style: TextStyle(fontSize: 12),
                  ),
                ],
              ),
              backgroundColor: Colors.red,
              duration: Duration(seconds: 4),
            ),
          );
        }
      }
    } catch (e) {
      print('❌ Error launching Uma Musume: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Failed to launch app: $e'),
            backgroundColor: Colors.red,
            duration: Duration(seconds: 3),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Icon(Icons.stars, size: 24),
            SizedBox(width: 8),
            Text('Umazing Helper'),
          ],
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.settings),
            tooltip: 'Settings',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => SettingsScreen()),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Character Selection Card
            Card(
              elevation: 2,
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

            // Quick Actions Section
            Text(
              'Quick Actions',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.grey.shade700,
              ),
            ),
            SizedBox(height: 12),

            // Launch Uma Musume Button
            Card(
              elevation: 2,
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: Colors.pink.shade100,
                  child: Icon(
                    Icons.sports_esports,
                    color: Colors.pink.shade700,
                  ),
                ),
                title: Text('Launch Uma Musume'),
                subtitle: Text('Open the game if installed'),
                trailing: Icon(Icons.arrow_forward_ios, size: 16),
                onTap: _launchUmaMusume,
              ),
            ),

            SizedBox(height: 16),

            // Main Action Button (Start/Stop Overlay)
            ElevatedButton.icon(
              onPressed: _toggleOverlay,
              icon: Icon(_isOverlayRunning ? Icons.stop : Icons.play_arrow),
              label: Text(
                _isOverlayRunning ? 'Stop Overlay' : 'Start Overlay',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              style: ElevatedButton.styleFrom(
                padding: EdgeInsets.symmetric(vertical: 16),
                backgroundColor: _isOverlayRunning ? Colors.red : Colors.green,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),

            SizedBox(height: 24),

            // How to Use Card
            Card(
              color: Colors.amber.shade50,
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.help_outline,
                          color: Colors.amber.shade700,
                          size: 24,
                        ),
                        SizedBox(width: 8),
                        Text(
                          'How to Use',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                            color: Colors.amber.shade900,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 12),
                    _buildHowToStep(
                      '1',
                      'Select Uma (optional for better accuracy)',
                    ),
                    _buildHowToStep('2', 'Grant permissions and start overlay'),
                    _buildHowToStep('3', 'Open your game or app'),
                    _buildHowToStep('4', 'Tap the green button to scan'),
                    _buildHowToStep(
                      '5',
                      'Long-press (500ms) to customize region',
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHowToStep(String number, String text) {
    return Padding(
      padding: EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: Colors.amber.shade700,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                number,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: TextStyle(fontSize: 14, color: Colors.amber.shade900),
            ),
          ),
        ],
      ),
    );
  }
}
