import 'package:flutter/material.dart';
import 'dart:typed_data';
import '../services/screen_capture_manager.dart';

class ScreenCaptureScreen extends StatefulWidget {
  @override
  _ScreenCaptureScreenState createState() => _ScreenCaptureScreenState();
}

class _ScreenCaptureScreenState extends State<ScreenCaptureScreen> {
  String _status = 'Not initialized';
  Uint8List? _capturedImage;
  bool _isOverlayRunning = false;

  @override
  void initState() {
    super.initState();
    _initializeService();
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

  Future<void> _captureScreen() async {
    setState(() => _status = 'Capturing screen...');

    final imageData = await ScreenCaptureManager.captureScreen();
    if (imageData != null) {
      setState(() {
        _capturedImage = imageData;
        _status = 'Screen captured successfully (${imageData.length} bytes)';
      });
    } else {
      setState(() => _status = 'Failed to capture screen');
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
      appBar: AppBar(title: Text('Screen Capture Test')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
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
              onPressed: _captureScreen,
              child: Text('Capture Screen'),
            ),

            ElevatedButton(
              onPressed: _toggleOverlay,
              child: Text(_isOverlayRunning ? 'Stop Overlay' : 'Start Overlay'),
            ),

            SizedBox(height: 16),

            // Display captured image
            if (_capturedImage != null)
              Expanded(
                child: Card(
                  child: Column(
                    children: [
                      Padding(
                        padding: EdgeInsets.all(8.0),
                        child: Text('Captured Screen:'),
                      ),
                      Expanded(
                        child: Image.memory(
                          _capturedImage!,
                          fit: BoxFit.contain,
                        ),
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
}
