import 'dart:io';

Future<bool> voiceFileExists(String path) async {
  final file = File(path);
  return file.exists();
}
