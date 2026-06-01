import 'voice_file_exists_stub.dart'
    if (dart.library.io) 'voice_file_exists_io.dart' as impl;

Future<bool> voiceFileExists(String path) => impl.voiceFileExists(path);
