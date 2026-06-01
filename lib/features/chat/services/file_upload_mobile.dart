import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

Future<String?> uploadFile({
  required ApiClient apiClient,
  required File file,
  required String fileName,
  String? thumbnailPath,
}) async {
  try {
    final fileBytes = await file.readAsBytes();
    final uri = Uri.parse('${apiClient.baseUrl}/api/messages/upload-attachment');
    final request = http.MultipartRequest('POST', uri);

    request.files.add(
      http.MultipartFile.fromBytes(
        'file',
        fileBytes,
        filename: fileName,
      ),
    );

    if (thumbnailPath != null) {
      final thumbnailFile = File(thumbnailPath);
      if (await thumbnailFile.exists()) {
        final thumbnailBytes = await thumbnailFile.readAsBytes();
        request.files.add(
          http.MultipartFile.fromBytes(
            'thumbnail',
            thumbnailBytes,
            filename: 'thumb_$fileName',
          ),
        );
      }
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

    throw Exception('Upload failed: ${response.statusCode} ${response.body}');
  } catch (e) {
    throw Exception('Error uploading file: $e');
  }
}
