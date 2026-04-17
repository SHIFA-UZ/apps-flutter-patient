import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';

void main() {
  group('DocumentModel', () {
    test('fromJson maps core fields', () {
      final doc = DocumentModel.fromJson({
        'id': 'd1',
        'patientId': 'p1',
        'title': 'Lab',
        'date': '2025-01-01',
        'fileUrl': 'https://example.com/f.pdf',
        'creatorLabel': 'Patient',
      });
      expect(doc.id, 'd1');
      expect(doc.title, 'Lab');
      expect(doc.isUploadedByPatient, isTrue);
    });

    test('fromJson uses createdAt when date missing', () {
      final doc = DocumentModel.fromJson({
        'id': '1',
        'patientId': 'p',
        'title': 't',
        'createdAt': '2025-06-15',
      });
      expect(doc.date, '2025-06-15');
    });
  });
}
