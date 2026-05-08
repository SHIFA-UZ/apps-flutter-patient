import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/base_card.dart';
import 'package:shifa_patient_app_v1/features/documents/domain/document_category.dart';

/// Document card: file-type icon, formatted name, date and uploader hierarchy, View button.
/// When [onDelete] is non-null, a delete icon is shown (for patient-uploaded docs only).
class DocumentCard extends StatelessWidget {
  final DocumentModel document;
  final String formattedDate;
  final String uploaderLabel;
  final VoidCallback? onView;
  final VoidCallback? onDelete;

  const DocumentCard({
    super.key,
    required this.document,
    required this.formattedDate,
    required this.uploaderLabel,
    this.onView,
    this.onDelete,
  });

  static IconData _iconForType(String? type, String? fileUrl) {
    final t = (type ?? '').toLowerCase();
    final url = (fileUrl ?? '').toLowerCase();
    if (t.contains('pdf') || url.endsWith('.pdf')) return Icons.picture_as_pdf;
    if (t.contains('image') || url.contains('image') || url.endsWith('.jpg') || url.endsWith('.jpeg') || url.endsWith('.png')) return Icons.image;
    return Icons.description;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final icon = _iconForType(document.type, document.fileUrl);
    final title = document.title.trim().isEmpty
        ? (l10n.translate('document') ?? 'Document')
        : document.title;

    return BaseCard(
      onTap: document.fileUrl != null && document.fileUrl!.isNotEmpty ? onView : null,
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: AppDesignSystem.primary.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: AppDesignSystem.primary, size: 26),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppDesignSystem.body2.copyWith(fontWeight: FontWeight.w600),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Wrap(
                  spacing: 6,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      formattedDate,
                      style: AppDesignSystem.caption,
                    ),
                    if (findPatientCategory(document.category) != null)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: AppDesignSystem.primary.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              findPatientCategory(document.category)!.icon,
                              size: 10,
                              color: AppDesignSystem.primary,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              findPatientCategory(document.category)!
                                  .label(l10n),
                              style: AppDesignSystem.caption.copyWith(
                                color: AppDesignSystem.primary,
                                fontSize: 10,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
                if (uploaderLabel.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      uploaderLabel,
                      style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textTertiary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          if (onDelete != null)
            IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: onDelete,
              tooltip: AppLocalizations.of(context)!.translate('delete'),
              color: AppDesignSystem.textTertiary,
              iconSize: 22,
            ),
          TextButton(
            onPressed: document.fileUrl != null && document.fileUrl!.isNotEmpty ? onView : null,
            child: Text(AppLocalizations.of(context)!.translate('view') ?? 'View'),
            style: TextButton.styleFrom(
              foregroundColor: AppDesignSystem.primary,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            ),
          ),
        ],
      ),
    );
  }
}
