import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class DocumentsRepository {
  final ApiClient _apiClient;

  DocumentsRepository(this._apiClient);

  /// Fetches document bytes from [url] with auth headers (for in-app viewer).
  Future<Uint8List> getDocumentBytes(String url) async {
    try {
      final response = await _apiClient.dio.get<List<int>>(
        url,
        options: Options(responseType: ResponseType.bytes),
      );
      final data = response.data;
      if (data == null) throw Exception('Empty response');
      return Uint8List.fromList(data);
    } catch (e) {
      if (e is DioException) {
        final msg = e.response?.data is String
            ? e.response!.data as String
            : e.message ?? 'Failed to load document';
        throw Exception(msg);
      }
      throw Exception('Failed to load document: $e');
    }
  }

  /// Fetches the current patient's documents (GET /patients/me/documents).
  Future<List<DocumentModel>> getDocuments() async {
    try {
      final response = await _apiClient.get('/patients/me/documents');
      final data = response.data as List<dynamic>;
      return data.map((e) => DocumentModel.fromJson(e as Map<String, dynamic>)).toList();
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to load documents';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to load documents: $e');
    }
  }

  /// Uploads a document for the current patient (POST /patients/me/documents).
  Future<DocumentModel> uploadDocument({
    required List<int> fileBytes,
    required String fileName,
    required String title,
    String? date,
    bool isChatAttachment = false,
  }) async {
    try {
      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(
          fileBytes,
          filename: fileName,
        ),
        'title': title,
        if (date != null) 'date': date,
        'isChatAttachment': isChatAttachment.toString(),
      });

      final response = await _apiClient.dio.post(
        '/patients/me/documents',
        data: formData,
      );

      return DocumentModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to upload document';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to upload document: $e');
    }
  }

  /// Deletes a document. Backend only allows deletion of documents uploaded by the current patient.
  Future<void> deleteDocument(String documentId) async {
    try {
      await _apiClient.delete('/patients/me/documents/$documentId');
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to delete document';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to delete document: $e');
    }
  }
}

final documentsRepositoryProvider = Provider<DocumentsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DocumentsRepository(apiClient);
});
