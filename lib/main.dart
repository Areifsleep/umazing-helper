import 'package:flutter/material.dart';
import 'screens/screen_capture_screen.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Screen Capture App',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: ScreenCaptureScreen(),
    );
  }
}
