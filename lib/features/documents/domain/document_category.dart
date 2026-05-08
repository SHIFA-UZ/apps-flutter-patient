import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

/// Document category codes shared with the backend.
///
/// Patient-uploaded documents are always visible to all doctors of the
/// patient, regardless of category. The category is used as a tag so doctors
/// can quickly understand the type of document the patient added.
class DocumentCategory {
  /// Stable wire/storage code (must match the Kotlin enum
  /// `PatientDocumentCategory.code`).
  final String code;
  final IconData icon;

  const DocumentCategory({
    required this.code,
    required this.icon,
  });

  /// Localized label, looked up via the key `documentCategory_<code>` and
  /// falling back to a humanised version of [code].
  String label(AppLocalizations l10n) {
    final translated = l10n.translate('documentCategory_$code');
    if (translated != null && translated.isNotEmpty) return translated;
    return code
        .split('_')
        .map((p) => p.isEmpty ? p : '${p[0]}${p.substring(1).toLowerCase()}')
        .join(' ');
  }
}

const List<DocumentCategory> kPatientDocumentCategories = [
  DocumentCategory(code: 'BLOOD_TEST', icon: Icons.bloodtype),
  DocumentCategory(code: 'URINE_TEST', icon: Icons.science),
  DocumentCategory(code: 'STOOL_TEST', icon: Icons.science),
  DocumentCategory(code: 'LAB_RESULT', icon: Icons.biotech),
  DocumentCategory(code: 'MRI', icon: Icons.medical_services),
  DocumentCategory(code: 'CT_SCAN', icon: Icons.medical_services),
  DocumentCategory(code: 'XRAY', icon: Icons.medical_information),
  DocumentCategory(code: 'ULTRASOUND', icon: Icons.monitor_heart),
  DocumentCategory(code: 'MAMMOGRAPHY', icon: Icons.medical_services),
  DocumentCategory(code: 'ECG', icon: Icons.monitor_heart),
  DocumentCategory(code: 'EEG', icon: Icons.psychology),
  DocumentCategory(code: 'ENDOSCOPY', icon: Icons.medical_information),
  DocumentCategory(code: 'BIOPSY', icon: Icons.biotech),
  DocumentCategory(code: 'PATHOLOGY', icon: Icons.biotech),
  DocumentCategory(code: 'IMAGING_OTHER', icon: Icons.image),
  DocumentCategory(code: 'PRESCRIPTION', icon: Icons.medication),
  DocumentCategory(code: 'VACCINATION_RECORD', icon: Icons.vaccines),
  DocumentCategory(code: 'DISCHARGE_SUMMARY', icon: Icons.local_hospital),
  DocumentCategory(code: 'REFERRAL', icon: Icons.assignment),
  DocumentCategory(code: 'HOSPITAL_REPORT', icon: Icons.local_hospital),
  DocumentCategory(code: 'ALLERGY_REPORT', icon: Icons.warning_amber),
  DocumentCategory(code: 'OTHER_MEDICAL', icon: Icons.folder_shared),
];

DocumentCategory? findPatientCategory(String? code) {
  if (code == null || code.isEmpty) return null;
  for (final c in kPatientDocumentCategories) {
    if (c.code == code) return c;
  }
  return null;
}
