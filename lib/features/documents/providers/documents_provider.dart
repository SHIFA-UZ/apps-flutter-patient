import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/features/documents/data/documents_repository.dart';

class DocumentsState {
  final List<DocumentModel> documents;
  final bool isLoading;
  final String? error;

  DocumentsState({
    this.documents = const [],
    this.isLoading = false,
    this.error,
  });

  DocumentsState copyWith({
    List<DocumentModel>? documents,
    bool? isLoading,
    String? error,
  }) {
    return DocumentsState(
      documents: documents ?? this.documents,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

class DocumentsNotifier extends StateNotifier<DocumentsState> {
  final DocumentsRepository _repository;

  DocumentsNotifier(this._repository) : super(DocumentsState());

  Future<void> loadDocuments() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final docs = await _repository.getDocuments();
      state = state.copyWith(documents: docs, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  Future<DocumentModel?> uploadDocument({
    required List<int> fileBytes,
    required String fileName,
    required String title,
    String? date,
    String? category,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final doc = await _repository.uploadDocument(
        fileBytes: fileBytes,
        fileName: fileName,
        title: title,
        date: date,
        category: category,
      );
      state = state.copyWith(
        documents: [doc, ...state.documents],
        isLoading: false,
      );
      return doc;
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
      rethrow;
    }
  }

  /// Deletes a document (only allowed for documents uploaded by the patient). Removes from state on success.
  Future<void> deleteDocument(String documentId) async {
    try {
      await _repository.deleteDocument(documentId);
      state = state.copyWith(
        documents: state.documents.where((d) => d.id != documentId).toList(),
      );
    } catch (e) {
      rethrow;
    }
  }
}

final documentsProvider = StateNotifierProvider<DocumentsNotifier, DocumentsState>((ref) {
  final repository = ref.watch(documentsRepositoryProvider);
  return DocumentsNotifier(repository);
});
