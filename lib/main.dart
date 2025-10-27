import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'services/recognition_data_service.dart';
import 'services/screen_recognition_service.dart';
import 'services/image_processor_service.dart';
import 'screens/screen_capture_screen.dart';

// Global navigator key to access context from anywhere
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  // Ensure Flutter is initialized
  WidgetsFlutterBinding.ensureInitialized();

  // Set preferred orientations (optional)
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Preload recognition data
  try {
    print('🚀 Loading recognition data...');
    await RecognitionDataService.preloadAllData();
    print('✅ Recognition data loaded successfully');
  } catch (e) {
    print('❌ Failed to load recognition data: $e');
  }

  // Setup image processing from Android broadcasts
  _setupImageReceiver();

  runApp(UmaHelperApp());
}

/// Setup receiver for images from Android overlay service
void _setupImageReceiver() {
  // Listen for method calls from Android
  const platform = MethodChannel('uma_screen_capture');

  platform.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'onImageCaptured':
        await _handleImageFromAndroid(call.arguments);
        break;
      case 'showCharacterModal':
        _showCharacterModalFromOverlay();
        break;
      case 'updateCharacterSelection':
        _updateCharacterSelectionFromAndroid(call.arguments);
        break;
      case 'syncCharacterSelectionUI':
        _syncCharacterSelectionUI(call.arguments);
        break;
      case 'getCharacterList':
        return await _getCharacterListForAndroid();
      default:
        print('Unknown method from Android: ${call.method}');
    }
  });

  print('📡 Image receiver setup completed');
}

/// Update character selection from Android overlay
void _updateCharacterSelectionFromAndroid(dynamic arguments) {
  try {
    final character = arguments['character'] as String?;
    RecognitionDataService.setSelectedCharacter(character);
    print(
      '✅ Character selection updated from Android: ${character ?? "All Characters"}',
    );
  } catch (e) {
    print('❌ Error updating character selection from Android: $e');
  }
}

/// Sync character selection UI when Android overlay makes a selection
void _syncCharacterSelectionUI(dynamic arguments) {
  try {
    final character = arguments?['character'] as String?;
    print('🔄 Syncing UI state from Android: ${character ?? "All Characters"}');

    // Notify ScreenCaptureScreen to update its UI
    ScreenCaptureScreen.updateCharacterSelectionGlobal(character);
  } catch (e) {
    print('❌ Error syncing character selection UI: $e');
  }
}

/// Get character list for Android native overlay
Future<List<String>> _getCharacterListForAndroid() async {
  try {
    final characters = await RecognitionDataService.getAllCharacterNames();
    print('📤 Sending ${characters.length} characters to Android');
    return characters;
  } catch (e) {
    print('❌ Error getting character list for Android: $e');
    return [];
  }
}

/// Show character selection modal from overlay button
void _showCharacterModalFromOverlay() {
  final context = navigatorKey.currentContext;
  if (context == null) {
    print('❌ Cannot show character modal: No navigator context available');
    return;
  }

  // Find the ScreenCaptureScreen in the widget tree and call its method
  // We'll need to access this through a global method
  ScreenCaptureScreen.showCharacterSelectionModalGlobal(context);
}

/// Handle image data received from Android overlay service
Future<void> _handleImageFromAndroid(dynamic arguments) async {
  try {
    final imageData = arguments['image_data'] as Uint8List?;
    final width = arguments['width'] as int?;
    final height = arguments['height'] as int?;
    final format = arguments['format'] as String?;
    final isTemp = arguments['temp_processing'] as bool? ?? false;

    if (imageData != null && width != null && height != null && isTemp) {
      print(
        '📱 Received raw ${format ?? 'RGBA'} image from Android (${imageData.length} bytes, ${width}x${height})',
      );

      // ✅ OPTIMIZATION 3: Process without blocking (fire and forget)
      // ✅ OPTIMIZATION 4: Raw RGBA bytes (no PNG decode overhead)
      // Allows UI to remain responsive while processing
      ImageProcessorService.processTemporaryImage(
        imageData,
        width: width,
        height: height,
        format: format ?? 'RGBA_8888',
      ).catchError((e) {
        print('❌ Background processing error: $e');
      });
    } else {
      print(
        '❌ Invalid image data received from Android (missing width/height)',
      );
    }
  } catch (e) {
    print('❌ Error handling image from Android: $e');
  }
}

class UmaHelperApp extends StatefulWidget {
  @override
  _UmaHelperAppState createState() => _UmaHelperAppState();
}

class _UmaHelperAppState extends State<UmaHelperApp>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _cleanupResources();
    super.dispose();
  }

  /// Clean up resources when app is closed
  Future<void> _cleanupResources() async {
    try {
      await ScreenRecognitionService.dispose();
      print('✅ App resources cleaned up');
    } catch (e) {
      print('❌ Error cleaning up resources: $e');
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    switch (state) {
      case AppLifecycleState.resumed:
        print('📱 App resumed');
        break;
      case AppLifecycleState.paused:
        print('📱 App paused');
        break;
      case AppLifecycleState.detached:
        print('📱 App detached');
        _cleanupResources();
        break;
      default:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Umazing Helper',
      navigatorKey: navigatorKey, // Add the global navigator key
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: ScreenCaptureScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}
