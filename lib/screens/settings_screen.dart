import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/scan_button_preferences.dart';

class SettingsScreen extends StatefulWidget {
  @override
  _SettingsScreenState createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  double _scanButtonSize = ScanButtonPreferences.defaultSize;
  double _scanButtonOpacity = ScanButtonPreferences.defaultOpacity;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPreferences();
  }

  Future<void> _loadPreferences() async {
    final prefs = await ScanButtonPreferences.getAllPreferences();
    setState(() {
      _scanButtonSize = prefs['size']!;
      _scanButtonOpacity = prefs['opacity']!;
      _isLoading = false;
    });
  }

  Future<void> _saveScanButtonSize(double size) async {
    await ScanButtonPreferences.setScanButtonSize(size);
    setState(() => _scanButtonSize = size);
    _notifyAndroid();
    _showSavedSnackbar();
  }

  Future<void> _saveScanButtonOpacity(double opacity) async {
    await ScanButtonPreferences.setScanButtonOpacity(opacity);
    setState(() => _scanButtonOpacity = opacity);
    _notifyAndroid();
    _showSavedSnackbar();
  }

  Future<void> _resetToDefaults() async {
    await ScanButtonPreferences.resetToDefaults();
    await _loadPreferences();
    _notifyAndroid();

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('✅ Settings reset to defaults'),
          backgroundColor: Colors.blue,
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  /// Notify Android overlay service to update scan button appearance
  Future<void> _notifyAndroid() async {
    try {
      const platform = MethodChannel('uma_screen_capture');
      await platform.invokeMethod('updateScanButtonAppearance', {
        'size': _scanButtonSize,
        'opacity': _scanButtonOpacity,
      });
      print('📱 Android notified of scan button appearance change');
    } catch (e) {
      print('❌ Error notifying Android: $e');
    }
  }

  void _showSavedSnackbar() {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('✅ Settings saved'),
          duration: Duration(seconds: 1),
          backgroundColor: Colors.green,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Settings'),
        actions: [
          IconButton(
            icon: Icon(Icons.restore),
            tooltip: 'Reset to defaults',
            onPressed: _resetToDefaults,
          ),
        ],
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : ListView(
              padding: EdgeInsets.all(16),
              children: [
                // Scan Button Section
                _buildSectionHeader(
                  icon: Icons.touch_app,
                  title: 'Scan Button Appearance',
                  subtitle: 'Customize the overlay scan button',
                ),
                SizedBox(height: 16),

                // Size Slider
                _buildSliderCard(
                  icon: Icons.photo_size_select_small,
                  title: 'Button Size',
                  value: _scanButtonSize,
                  min: 40,
                  max: 100,
                  divisions: 30,
                  label: '${_scanButtonSize.toInt()} dp',
                  onChanged: (value) {
                    setState(() => _scanButtonSize = value);
                  },
                  onChangeEnd: _saveScanButtonSize,
                  valueDisplay: '${_scanButtonSize.toInt()} dp',
                  description:
                      'Small buttons are less obtrusive, large buttons are easier to tap',
                ),

                SizedBox(height: 16),

                // Opacity Slider
                _buildSliderCard(
                  icon: Icons.opacity,
                  title: 'Button Transparency',
                  value: _scanButtonOpacity,
                  min: 0.1,
                  max: 1.0,
                  divisions: 9,
                  label: '${(_scanButtonOpacity * 100).toInt()}%',
                  onChanged: (value) {
                    setState(() => _scanButtonOpacity = value);
                  },
                  onChangeEnd: _saveScanButtonOpacity,
                  valueDisplay:
                      '${(_scanButtonOpacity * 100).toInt()}% opacity',
                  description:
                      'Lower opacity makes the button more see-through',
                ),
              ],
            ),
    );
  }

  Widget _buildSectionHeader({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return Row(
      children: [
        Icon(icon, size: 32, color: Colors.blue),
        SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 4),
              Text(
                subtitle,
                style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSliderCard({
    required IconData icon,
    required String title,
    required double value,
    required double min,
    required double max,
    required int divisions,
    required String label,
    required ValueChanged<double> onChanged,
    required ValueChanged<double> onChangeEnd,
    required String valueDisplay,
    required String description,
  }) {
    return Card(
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: Colors.grey.shade700),
                SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        valueDisplay,
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.blue,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            SizedBox(height: 8),
            Slider(
              value: value,
              min: min,
              max: max,
              divisions: divisions,
              label: label,
              onChanged: onChanged,
              onChangeEnd: onChangeEnd,
              activeColor: Colors.blue,
            ),
            Text(
              description,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
