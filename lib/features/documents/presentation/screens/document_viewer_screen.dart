import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pdfx/pdfx.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/features/documents/data/documents_repository.dart';

/// In-app document viewer. Opens PDFs and images inside the app with zoom/scroll.
/// No external browser or auto-download.
class DocumentViewerScreen extends ConsumerStatefulWidget {
  final DocumentModel document;

  const DocumentViewerScreen({super.key, required this.document});

  @override
  ConsumerState<DocumentViewerScreen> createState() =>
      _DocumentViewerScreenState();
}

enum _ViewerType { pdf, image, unsupported }

class _DocumentViewerScreenState extends ConsumerState<DocumentViewerScreen> {
  Uint8List? _bytes;
  String? _error;
  bool _loading = true;
  PdfControllerPinch? _pdfController;
  _ViewerType? _resolvedViewerType;

  static const _supportedImageExtensions = ['jpg', 'jpeg', 'png', 'webp'];
  static const _pdfExtension = 'pdf';

  static const _pdfMagic = [0x25, 0x50, 0x44, 0x46]; // %PDF
  static const _jpegMagic = [0xFF, 0xD8, 0xFF];
  static const _pngMagic = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
  static const _webpMagic = [0x52, 0x49, 0x46, 0x46]; // RIFF; WebP also has WEBP at 8-11

  @override
  void initState() {
    super.initState();
    _loadDocument();
  }

  @override
  void dispose() {
    _pdfController?.dispose();
    super.dispose();
  }

  _ViewerType _getViewerTypeFromUrl() {
    final url = widget.document.fileUrl ?? '';
    final type = widget.document.type?.toLowerCase();
    final pathOnly = url.split('?').first;
    final ext = pathOnly.contains('.')
        ? pathOnly.split('.').last.toLowerCase()
        : '';
    final effective = (type != null && type.isNotEmpty) ? type : ext;
    if (effective == _pdfExtension) return _ViewerType.pdf;
    if (_supportedImageExtensions.contains(effective)) return _ViewerType.image;
    return _ViewerType.unsupported;
  }

  _ViewerType _getViewerTypeFromBytes(Uint8List bytes) {
    if (bytes.length < 12) return _ViewerType.unsupported;
    if (bytes.length >= _pdfMagic.length &&
        bytes[0] == _pdfMagic[0] &&
        bytes[1] == _pdfMagic[1] &&
        bytes[2] == _pdfMagic[2] &&
        bytes[3] == _pdfMagic[3]) {
      return _ViewerType.pdf;
    }
    if (bytes.length >= _jpegMagic.length &&
        bytes[0] == _jpegMagic[0] &&
        bytes[1] == _jpegMagic[1] &&
        bytes[2] == _jpegMagic[2]) {
      return _ViewerType.image;
    }
    if (bytes.length >= _pngMagic.length) {
      bool png = true;
      for (int i = 0; i < _pngMagic.length; i++) {
        if (bytes[i] != _pngMagic[i]) {
          png = false;
          break;
        }
      }
      if (png) return _ViewerType.image;
    }
    if (bytes.length >= 12 &&
        bytes[0] == _webpMagic[0] &&
        bytes[1] == _webpMagic[1] &&
        bytes[2] == _webpMagic[2] &&
        bytes[3] == _webpMagic[3] &&
        bytes[8] == 0x57 &&
        bytes[9] == 0x45 &&
        bytes[10] == 0x42 &&
        bytes[11] == 0x50) {
      return _ViewerType.image;
    }
    return _ViewerType.unsupported;
  }

  Future<void> _loadDocument() async {
    final url = widget.document.fileUrl;
    if (url == null || url.isEmpty) {
      setState(() {
        _loading = false;
        _error = 'No document URL';
        _resolvedViewerType = null;
      });
      return;
    }

    final fromUrl = _getViewerTypeFromUrl();

    setState(() {
      _loading = true;
      _error = null;
      _bytes = null;
      _resolvedViewerType = null;
    });

    try {
      final repo = ref.read(documentsRepositoryProvider);
      final data = await repo.getDocumentBytes(url);
      if (!mounted) return;

      final resolved = _getViewerTypeFromBytes(data);
      final viewerType = resolved != _ViewerType.unsupported ? resolved : fromUrl;

      if (viewerType == _ViewerType.pdf) {
        _pdfController = PdfControllerPinch(
          document: PdfDocument.openData(data),
          initialPage: 1,
        );
      } else {
        _pdfController?.dispose();
        _pdfController = null;
      }

      setState(() {
        _bytes = data;
        _resolvedViewerType = viewerType;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      final l10n = AppLocalizations.of(context)!;
      setState(() {
        _error = translateError(l10n, e.toString());
        _loading = false;
        _resolvedViewerType = null;
      });
    }
  }

  Future<void> _download() async {
    if (_bytes == null) return;
    // Optional: save to device storage (path_provider + file). For now just show a snackbar.
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
              AppLocalizations.of(context)!.translate('downloadStarted')),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final viewerType = _resolvedViewerType ?? _getViewerTypeFromUrl();

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(
          widget.document.title,
          overflow: TextOverflow.ellipsis,
        ),
        actions: [
          if (_bytes != null && viewerType != _ViewerType.unsupported)
            IconButton(
              icon: const Icon(Icons.download),
              onPressed: _download,
              tooltip: l10n.translate('download'),
            ),
        ],
      ),
      body: _buildBody(l10n, viewerType),
    );
  }

  Widget _buildBody(AppLocalizations l10n, _ViewerType viewerType) {
    if (viewerType == _ViewerType.unsupported) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.insert_drive_file, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 16),
              Text(
                l10n.translate('unsupportedDocumentType'),
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[700], fontSize: 16),
              ),
            ],
          ),
        ),
      );
    }

    if (_loading) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text(l10n.translate('loadingDocument')),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 64, color: Colors.red[300]),
              const SizedBox(height: 16),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.red[700]),
              ),
              const SizedBox(height: 24),
              ShifaPrimaryButton(
                label: l10n.retry,
                onPressed: _loadDocument,
                width: ButtonWidth.hug,
              ),
            ],
          ),
        ),
      );
    }

    if (_bytes == null) return const SizedBox.shrink();

    if (viewerType == _ViewerType.pdf && _pdfController != null) {
      return PdfViewPinch(controller: _pdfController!);
    }

    if (viewerType == _ViewerType.image) {
      return InteractiveViewer(
        minScale: 0.5,
        maxScale: 4.0,
        child: Center(
          child: Image.memory(
            _bytes!,
            fit: BoxFit.contain,
            errorBuilder: (_, __, ___) => const Icon(Icons.broken_image, size: 64),
          ),
        ),
      );
    }

    return const SizedBox.shrink();
  }
}
