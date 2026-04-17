import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/features/documents/providers/documents_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class DocumentDetailsScreen extends ConsumerStatefulWidget {
  final String documentId;

  const DocumentDetailsScreen({super.key, required this.documentId});

  @override
  ConsumerState<DocumentDetailsScreen> createState() => _DocumentDetailsScreenState();
}

class _DocumentDetailsScreenState extends ConsumerState<DocumentDetailsScreen> {
  bool _requestedLoad = false;

  @override
  Widget build(BuildContext context) {
    final documentsState = ref.watch(documentsProvider);
    final profileState = ref.watch(profileProvider);
    final l10n = AppLocalizations.of(context)!;
    final doc = _findDocument(documentsState.documents, widget.documentId);

    if (doc == null && !_requestedLoad && profileState.profile != null) {
      _requestedLoad = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(documentsProvider.notifier).loadDocuments();
      });
    }

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(doc?.title ?? l10n.translate('document')),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await ref.read(documentsProvider.notifier).loadDocuments();
        },
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.translate('information'), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    const SizedBox(height: 12),
                    Text('${l10n.translate('date')}: ${_formatDate(doc?.date)}'),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            ShifaPrimaryButton(
              label: l10n.openDocument,
              icon: Icons.visibility,
              onPressed: doc?.fileUrl != null && doc!.fileUrl!.isNotEmpty
                  ? () => context.push(AppRoutes.documentViewer, extra: doc)
                  : null,
            ),
          ],
        ),
        ),
      ),
    );
  }

  DocumentModel? _findDocument(List<DocumentModel> docs, String id) {
    try {
      return docs.firstWhere((d) => d.id == id);
    } catch (e) {
      return null;
    }
  }

  String _formatDate(String? raw) {
    if (raw == null || raw.isEmpty) return '';
    try {
      final parsed = DateTime.parse(raw);
      return DateFormat('dd.MM.yyyy').format(parsed);
    } catch (e) {
      return raw;
    }
  }

}
