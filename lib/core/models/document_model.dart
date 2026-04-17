import 'package:equatable/equatable.dart';

class DocumentModel extends Equatable {
  final String id;
  final String patientId;
  final String title;
  final String date;
  final String? fileUrl;
  final String? type;
  final String? doctorId;
  final String? doctorName;
  /// Who created/uploaded: "Doctor", "Patient", or "Unknown". Patients see all their docs.
  final String? creatorLabel;

  const DocumentModel({
    required this.id,
    required this.patientId,
    required this.title,
    required this.date,
    this.fileUrl,
    this.type,
    this.doctorId,
    this.doctorName,
    this.creatorLabel,
  });

  factory DocumentModel.fromJson(Map<String, dynamic> json) {
    return DocumentModel(
      id: json['id']?.toString() ?? '',
      patientId: json['patientId']?.toString() ?? '',
      title: json['title'] as String? ?? '',
      date: json['date'] as String? ?? json['createdAt'] as String? ?? '',
      fileUrl: (json['fileUrl'] ?? json['url']) as String?,
      type: json['type'] as String?,
      doctorId: json['doctorId']?.toString(),
      doctorName: json['doctorName'] as String?,
      creatorLabel: json['creatorLabel'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'patientId': patientId,
      'title': title,
      'date': date,
      'fileUrl': fileUrl,
      'type': type,
      'doctorId': doctorId,
      'doctorName': doctorName,
      'creatorLabel': creatorLabel,
    };
  }

  /// True when the document was uploaded by the patient (they can delete it).
  bool get isUploadedByPatient => creatorLabel == 'Patient';

  @override
  List<Object?> get props => [
        id,
        patientId,
        title,
        date,
        fileUrl,
        type,
        doctorId,
        doctorName,
        creatorLabel,
      ];
}
