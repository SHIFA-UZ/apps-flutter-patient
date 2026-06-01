import 'dart:io';

import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:image_picker/image_picker.dart';

Future<XFile?> compressImage(XFile imageFile, {int quality = 85}) async {
  try {
    final file = File(imageFile.path);
    final fileSize = await file.length();

    if (fileSize < 500 * 1024) {
      return imageFile;
    }

    final targetPath = '${imageFile.path}_compressed.jpg';

    final compressedFile = await FlutterImageCompress.compressAndGetFile(
      imageFile.path,
      targetPath,
      quality: quality,
      minWidth: 1920,
      minHeight: 1920,
    );

    if (compressedFile != null) {
      return XFile(compressedFile.path);
    }

    return imageFile;
  } catch (e) {
    return imageFile;
  }
}
