import 'file_delete_stub.dart'
    if (dart.library.io) 'file_delete_io.dart' as impl;

Future<void> deletePathIfExists(String path) => impl.deletePathIfExists(path);
