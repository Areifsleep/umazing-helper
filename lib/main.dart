import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'services/recognition_data_service.dart';
import 'services/screen_recognition_service.dart';
import 'services/image_processor_service.dart';
import 'screens/screen_capture_screen.dart';

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
      default:
        print('Unknown method from Android: ${call.method}');
    }
  });

  print('📡 Image receiver setup completed');
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
      title: 'Uma Helper',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: ScreenCaptureScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}
