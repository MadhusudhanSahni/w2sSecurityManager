import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FingerPrintScanner extends StatefulWidget {
  @override
  _FingerPrintScannerState createState() => _FingerPrintScannerState();
}

class _FingerPrintScannerState extends State<FingerPrintScanner> {
  static const platform = MethodChannel('fingerprint_scanner');
  String _fingerprintStatus = "Not Scanned";
  bool _isScanning = false;

  // Method to initiate fingerprint scan via MethodChannel
  Future<void> _scanFingerprint() async {
    setState(() {
      _isScanning = true; // Show loading state
      _fingerprintStatus = "Scanning...";
    });

    try {
      // Call the native code through the MethodChannel
      final String result = await platform.invokeMethod('scanFingerprint');
      setState(() {
        _fingerprintStatus = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _fingerprintStatus = "Failed to scan fingerprint: ${e.message}";
      });
    } finally {
      setState(() {
        _isScanning = false; // Hide loading state
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Fingerprint Scanner"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_isScanning)
              CircularProgressIndicator()
            else
              Text(
                _fingerprintStatus,
                style: TextStyle(fontSize: 18),
                textAlign: TextAlign.center,
              ),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _isScanning ? null : _scanFingerprint,
              child: Text("Scan Fingerprint"),
            ),
          ],
        ),
      ),
    );
  }
}

void main() {
  runApp(MaterialApp(
    home: FingerPrintScanner(),
  ));
}
