import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:image_picker/image_picker.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/core/utils/permission_rationale.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_header.dart';
import 'package:shifa_patient_app_v1/core/widgets/document_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/empty_state.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/features/documents/providers/documents_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class DocumentsScreen extends ConsumerStatefulWidget {
  const DocumentsScreen({super.key});

  @override
  ConsumerState<DocumentsScreen> createState() => _DocumentsScreenState();
}

class _DocumentsScreenState extends ConsumerState<DocumentsScreen> {
  final _searchController = TextEditingController();
  String _searchQuery = '';
  String? _loadedPatientId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(profileProvider.notifier).loadProfile();
    });

    _searchController.addListener(() {
      setState(() {
        _searchQuery = _searchController.text.trim().toLowerCase();
      });
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final profileState = ref.watch(profileProvider);
    final documentsState = ref.watch(documentsProvider);
    final displayPhotoUrl = withCacheBuster(
      profileState.profile?.photoUrl,
      profileState.photoCacheKey,
    );

    ref.listen<ProfileState>(profileProvider, (previous, next) {
      final id = next.profile?.id;
      if (id != null && id.isNotEmpty && id != _loadedPatientId) {
        _loadedPatientId = id;
        ref.read(documentsProvider.notifier).loadDocuments();
      }
    });

    final filteredDocuments = documentsState.documents.where((doc) {
      if (_searchQuery.isEmpty) return true;
      return doc.title.toLowerCase().contains(_searchQuery);
    }).toList();

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      body: RefreshIndicator(
        onRefresh: () async {
          await ref.read(profileProvider.notifier).loadProfile();
          await ref.read(documentsProvider.notifier).loadDocuments();
        },
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: AppHeader(
                title: l10n.documents,
                showProfile: true,
                showNotification: true,
                showBack: false,
                onLogoTap: () => context.go(AppRoutes.home),
                onProfileTap: () => context.push(AppRoutes.account),
                profilePhotoUrl: displayPhotoUrl,
                onNotificationTap: () => context.push(AppRoutes.notifications),
                onChatTap: () => context.push(AppRoutes.chat),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppDesignSystem.screenPaddingH,
                  16,
                  AppDesignSystem.screenPaddingH,
                  8,
                ),
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: l10n.searchDocuments,
                    prefixIcon: const Icon(Icons.search),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    filled: true,
                    fillColor: AppDesignSystem.backgroundSecondary,
                  ),
                ),
              ),
            ),
            if (documentsState.isLoading)
              const SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: Center(child: CircularProgressIndicator()),
                ),
              )
            else if (documentsState.error != null)
              SliverToBoxAdapter(
                child: EmptyState(
                  icon: Icons.error_outline,
                  message: '${l10n.error}: ${documentsState.error}',
                  action: ShifaPrimaryButton(
                    label: l10n.retry,
                    onPressed: () {
                      final id = profileState.profile?.id;
                      if (id != null && id.isNotEmpty) {
                        ref.read(documentsProvider.notifier).loadDocuments();
                      }
                    },
                    width: ButtonWidth.hug,
                  ),
                ),
              )
            else if (filteredDocuments.isEmpty)
              SliverToBoxAdapter(
                child: EmptyState(
                  icon: Icons.folder_open,
                  message: l10n.noDocumentsFound,
                  action: TextButton.icon(
                    onPressed: _showUploadOptions,
                    icon: Icon(Icons.upload, size: 18, color: AppDesignSystem.primary),
                    label: Text(l10n.upload, style: TextStyle(color: AppDesignSystem.primary)),
                  ),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (context, index) {
                      final doc = filteredDocuments[index];
                      final formattedDate = _formatDate(doc.date);
                      final uploaderLabel = doc.creatorLabel == null
                          ? ''
                          : doc.creatorLabel == 'Doctor'
                              ? l10n.addedByDoctor
                              : doc.creatorLabel == 'Patient'
                                  ? l10n.addedByYou
                                  : doc.creatorLabel!;
                      return Padding(
                        padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                        child: DocumentCard(
                          document: doc,
                          formattedDate: formattedDate,
                          uploaderLabel: uploaderLabel,
                          onView: () => _openInAppViewer(context, doc),
                          onDelete: doc.isUploadedByPatient
                              ? () => _confirmDeleteDocument(context, doc)
                              : null,
                        ),
                      );
                    },
                    childCount: filteredDocuments.length,
                  ),
                ),
              ),
            SliverToBoxAdapter(
              child: SizedBox(height: AppDesignSystem.safeBottomWithNavBar(context)),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showUploadOptions,
        backgroundColor: AppDesignSystem.primary,
        label: Text(l10n.upload),
        icon: const Icon(Icons.upload),
        heroTag: 'documents_fab',
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }

  String _formatDate(String raw) {
    if (raw.isEmpty) return '';
    try {
      final parsed = DateTime.parse(raw);
      return DateFormat('dd.MM.yyyy').format(parsed);
    } catch (e) {
      return raw;
    }
  }

  void _openInAppViewer(BuildContext context, DocumentModel document) {
    if (document.fileUrl == null || document.fileUrl!.isEmpty) return;
    context.push(AppRoutes.documentViewer, extra: document);
  }

  Future<void> _confirmDeleteDocument(BuildContext context, DocumentModel doc) async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('delete')),
        content: Text(l10n.translate('deleteDocumentConfirmation')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.translate('cancel')),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.translate('delete')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await ref.read(documentsProvider.notifier).deleteDocument(doc.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('documentDeleted'))),
      );
    } catch (e) {
      if (!mounted) return;
      final l10n = AppLocalizations.of(context)!;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Delete document'))),
      );
    }
  }

  Future<void> _showUploadOptions() async {
    final l10n = AppLocalizations.of(context)!;
    final choice = await showModalBottomSheet<String>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt, color: Color(0xFF17C3B2)),
              title: Text(l10n.takePhoto ?? 'Take Photo'),
              onTap: () => Navigator.pop(context, 'camera'),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library, color: Color(0xFF17C3B2)),
              title: Text(l10n.chooseFromGallery ?? 'Choose from Gallery'),
              onTap: () => Navigator.pop(context, 'gallery'),
            ),
            ListTile(
              leading: const Icon(Icons.insert_drive_file, color: Color(0xFF17C3B2)),
              title: Text(l10n.uploadFile ?? 'Upload File'),
              onTap: () => Navigator.pop(context, 'file'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );

    if (choice == null) return;

    switch (choice) {
      case 'camera':
        await _uploadFromCamera();
        break;
      case 'gallery':
        await _uploadFromGallery();
        break;
      case 'file':
        await _uploadDocument();
        break;
    }
  }

  Future<void> _uploadFromCamera() async {
    final l10n = AppLocalizations.of(context)!;
    final patientId = ref.read(profileProvider).profile?.id;
    if (patientId == null || patientId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${l10n.profile} ${l10n.error.toLowerCase()}')),
      );
      return;
    }

    final proceed = await showPermissionRationale(
      context: context,
      rationaleKey: 'permissionRationaleCamera',
    );
    if (!mounted || !proceed) return;

    ref.read(appLockTemporaryDisableProvider.notifier).disable();
    try {
      final picker = ImagePicker();
      final image = await picker.pickImage(
        source: ImageSource.camera,
        imageQuality: 85,
      );

      if (image == null) {
        ref.read(appLockTemporaryDisableProvider.notifier).enable();
        return;
      }

      final file = File(image.path);
      final bytes = await file.readAsBytes();

      final title = await _askForTitle('photo_${DateTime.now().millisecondsSinceEpoch}.jpg');
      if (title == null || title.isEmpty) {
        ref.read(appLockTemporaryDisableProvider.notifier).enable();
        return;
      }

      final selectedDate = await _askForDate();
      final dateString = selectedDate == null
          ? null
          : DateFormat('yyyy-MM-dd').format(selectedDate);

      await ref.read(documentsProvider.notifier).uploadDocument(
            fileBytes: bytes,
            fileName: '${title}.jpg',
            title: title,
            date: dateString,
          );
      
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.documentUploadedSuccessfully)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.uploadFailed)),
        );
      }
    } finally {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
    }
  }

  Future<void> _uploadFromGallery() async {
    final l10n = AppLocalizations.of(context)!;
    final patientId = ref.read(profileProvider).profile?.id;
    if (patientId == null || patientId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${l10n.profile} ${l10n.error.toLowerCase()}')),
      );
      return;
    }

    ref.read(appLockTemporaryDisableProvider.notifier).disable();
    try {
      final picker = ImagePicker();
      final image = await picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
      );

      if (image == null) {
        ref.read(appLockTemporaryDisableProvider.notifier).enable();
        return;
      }

      final file = File(image.path);
      final bytes = await file.readAsBytes();

      final title = await _askForTitle('photo_${DateTime.now().millisecondsSinceEpoch}.jpg');
      if (title == null || title.isEmpty) {
        ref.read(appLockTemporaryDisableProvider.notifier).enable();
        return;
      }

      final selectedDate = await _askForDate();
      final dateString = selectedDate == null
          ? null
          : DateFormat('yyyy-MM-dd').format(selectedDate);

      await ref.read(documentsProvider.notifier).uploadDocument(
            fileBytes: bytes,
            fileName: '${title}.jpg',
            title: title,
            date: dateString,
          );
      
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.documentUploadedSuccessfully)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.uploadFailed}: ${userFriendlyError(l10n, e, logContext: 'Upload from camera')}'),
          ),
        );
      }
    } finally {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
    }
  }

  Future<void> _uploadDocument() async {
    final l10n = AppLocalizations.of(context)!;
    final patientId = ref.read(profileProvider).profile?.id;
    if (patientId == null || patientId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${l10n.profile} ${l10n.error.toLowerCase()}')),
      );
      return;
    }

    ref.read(appLockTemporaryDisableProvider.notifier).disable();
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['pdf'],
      withData: true,
    );

    if (result == null || result.files.isEmpty) {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
      return;
    }
    
    final file = result.files.first;
    final bytes = file.bytes;
    if (bytes == null) {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.couldNotReadFileBytes ?? 'Could not read file bytes')),
      );
      return;
    }

    final title = await _askForTitle(file.name);
    if (title == null || title.isEmpty) {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
      return;
    }

    final selectedDate = await _askForDate();
    final dateString = selectedDate == null
        ? null
        : DateFormat('yyyy-MM-dd').format(selectedDate);

    try {
      await ref.read(documentsProvider.notifier).uploadDocument(
            fileBytes: bytes,
            fileName: file.name,
            title: title,
            date: dateString,
          );
      
      
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.documentUploadedSuccessfully)),
        );
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.uploadFailed}: ${userFriendlyError(l10n, e, logContext: 'Upload document')}'),
          ),
        );
      }
    } finally {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
    }
  }

  Future<String?> _askForTitle(String fileName) async {
    final controller = TextEditingController(
      text: fileName.replaceAll(RegExp(r'\.pdf$', caseSensitive: false), ''),
    );

    return showDialog<String>(
      context: context,
      builder: (dialogContext) {
        final dialogL10n = AppLocalizations.of(dialogContext)!;
        return AlertDialog(
          title: Text(dialogL10n.documentTitle),
          content: TextField(
            controller: controller,
            decoration: InputDecoration(hintText: dialogL10n.enterDocumentTitle),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(dialogL10n.cancel),
            ),
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(controller.text.trim()),
              child: Text(dialogL10n.save),
            ),
          ],
        );
      },
    );
  }

  Future<DateTime?> _askForDate() async {
    final now = DateTime.now();
    return showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(now.year - 20),
      lastDate: DateTime(now.year + 1),
    );
  }
}
