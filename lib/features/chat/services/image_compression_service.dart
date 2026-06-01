// lib/features/chat/services/image_compression_service.dart
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:image_picker/image_picker.dart';

import 'image_compression_io.dart'
    if (dart.library.html) 'image_compression_stub.dart' as impl;

class ImageCompressionService {
  /// Compress image before upload. On web, returns the original [imageFile].
  static Future<XFile?> compressImage(XFile imageFile, {int quality = 85}) async {
    if (kIsWeb) {
      return imageFile;
    }
    return impl.compressImage(imageFile, quality: quality);
  }
}
