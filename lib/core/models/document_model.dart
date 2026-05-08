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

  /// Optional uploader-chosen category code (e.g. "MRI", "BLOOD_TEST"). Null
  /// when uploaded before categories existed or when not tagged.
  final String? category;

  /// True when the backend marks the document as visible to every doctor of
  /// the patient (patient uploads, or doctor uploads of medical results).
  final bool isSharedWithTeam;

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
    this.category,
    this.isSharedWithTeam = false,
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
      category: json['category'] as String?,
      isSharedWithTeam: json['isSharedWithTeam'] as bool? ?? false,
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
      'category': category,
      'isSharedWithTeam': isSharedWithTeam,
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
        category,
        isSharedWithTeam,
      ];
}
