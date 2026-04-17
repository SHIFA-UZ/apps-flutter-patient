import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/features/documents/data/documents_repository.dart';
import 'package:shifa_patient_app_v1/features/documents/providers/documents_provider.dart';

class MockDocumentsRepository extends Mock implements DocumentsRepository {}

void main() {
  late MockDocumentsRepository repo;
  late DocumentsNotifier notifier;

  final sampleDoc = DocumentModel(
    id: '1',
    patientId: 'p',
    title: 'Test',
    date: '2025-01-01',
    fileUrl: 'https://x/u',
    creatorLabel: 'Patient',
  );

  setUp(() {
    repo = MockDocumentsRepository();
    notifier = DocumentsNotifier(repo);
  });

  group('DocumentsNotifier', () {
    test('loadDocuments sets list on success', () async {
      when(() => repo.getDocuments()).thenAnswer((_) async => [sampleDoc]);
      await notifier.loadDocuments();
      expect(notifier.state.isLoading, isFalse);
      expect(notifier.state.error, isNull);
      expect(notifier.state.documents, [sampleDoc]);
    });

    test('loadDocuments sets error on failure', () async {
      when(() => repo.getDocuments()).thenThrow(Exception('boom'));
      await notifier.loadDocuments();
      expect(notifier.state.isLoading, isFalse);
      expect(notifier.state.documents, isEmpty);
      expect(notifier.state.error, isNotNull);
    });

    test(
      'uploadDocument prepends doc and clears error when list non-empty',
      () async {
        notifier.state = DocumentsState(documents: [sampleDoc]);
        final newDoc = DocumentModel(
          id: '2',
          patientId: 'p',
          title: 'New',
          date: '2025-02-01',
          creatorLabel: 'Patient',
        );
        when(
          () => repo.uploadDocument(
            fileBytes: [1, 2, 3],
            fileName: 'f.pdf',
            title: 'New',
            date: any(named: 'date'),
            isChatAttachment: any(named: 'isChatAttachment'),
          ),
        ).thenAnswer((_) async => newDoc);

        await notifier.uploadDocument(
          fileBytes: [1, 2, 3],
          fileName: 'f.pdf',
          title: 'New',
        );

        expect(notifier.state.documents.first.id, '2');
        expect(notifier.state.error, isNull);
      },
    );

    test(
      'upload failure sets error and rethrows when documents already loaded',
      () async {
        notifier.state = DocumentsState(documents: [sampleDoc]);
        when(
          () => repo.uploadDocument(
            fileBytes: any(named: 'fileBytes'),
            fileName: any(named: 'fileName'),
            title: any(named: 'title'),
            date: any(named: 'date'),
            isChatAttachment: any(named: 'isChatAttachment'),
          ),
        ).thenThrow(Exception('upload failed'));

        expect(
          () => notifier.uploadDocument(
            fileBytes: [1],
            fileName: 'x',
            title: 't',
          ),
          throwsException,
        );
        expect(notifier.state.error, contains('upload failed'));
        expect(notifier.state.documents, [sampleDoc]);
      },
    );

    test('deleteDocument removes from state', () async {
      notifier.state = DocumentsState(documents: [sampleDoc]);
      when(() => repo.deleteDocument('1')).thenAnswer((_) async {});
      await notifier.deleteDocument('1');
      expect(notifier.state.documents, isEmpty);
    });
  });
}
