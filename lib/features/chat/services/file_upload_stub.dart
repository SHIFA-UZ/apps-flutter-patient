import 'package:shifa_patient_app_v1/core/network/api_client.dart';

Future<String?> uploadFile({
  required ApiClient apiClient,
  required dynamic file,
  required String fileName,
  String? thumbnailPath,
}) async {
  throw UnsupportedError('uploadFile(File) is not supported on web; use uploadFileBytes');
}
