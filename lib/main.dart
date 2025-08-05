import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const UmaMusumeGuideApp());
}

class UmaMusumeGuideApp extends StatelessWidget {
  const UmaMusumeGuideApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Uma Musume Event Guide',
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
      home: const HomeScreen(),
    );
  }
}
