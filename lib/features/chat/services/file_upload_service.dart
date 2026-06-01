// lib/features/chat/services/file_upload_service.dart
import 'dart:typed_data';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'file_upload_mobile.dart'
    if (dart.library.html) 'file_upload_stub.dart' as mobile_upload;

/// Upload file to backend and return public URL
/// This service handles image, voice, and document uploads
class FileUploadService {
  static Future<String?> uploadFile({
    required ApiClient apiClient,
    required dynamic file,
    required String fileName,
    String? thumbnailPath,
  }) =>
      mobile_upload.uploadFile(
        apiClient: apiClient,
        file: file,
        fileName: fileName,
        thumbnailPath: thumbnailPath,
      );

  static Future<String?> uploadFileBytes({
    required ApiClient apiClient,
    required Uint8List fileBytes,
    required String fileName,
    Uint8List? thumbnailBytes,
  }) async {
    try {
      final uri = Uri.parse('${apiClient.baseUrl}/api/messages/upload-attachment');
      final request = http.MultipartRequest('POST', uri);

      request.files.add(
        http.MultipartFile.fromBytes(
          'file',
          fileBytes,
          filename: fileName,
        ),
      );

      if (thumbnailBytes != null) {
        request.files.add(
          http.MultipartFile.fromBytes(
            'thumbnail',
            thumbnailBytes,
            filename: 'thumb_$fileName',
          ),
        );
      }

      final token = await apiClient.getAuthToken();
      if (token != null) {
        request.headers['Authorization'] = 'Bearer $token';
      }

      final streamedResponse = await request.send();
      final response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final json = jsonDecode(response.body) as Map<String, dynamic>;
        return json['url'] as String?;
      }

      throw Exception(AppLogger.safeApiFailureMessage(response.statusCode, response.body));
    } catch (e) {
      throw Exception('Error uploading file: $e');
    }
  }
}
