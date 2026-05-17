import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:pdfx/pdfx.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// Opens a doctor certificate [url] inside the app (PDF / image / WebView fallback).
class CertificateViewerScreen extends StatefulWidget {
  final String url;
  final String title;

  const CertificateViewerScreen({
    super.key,
    required this.url,
    required this.title,
  });

  @override
  State<CertificateViewerScreen> createState() => _CertificateViewerScreenState();
}

enum _BodyKind { loading, pdf, image, web }

class _CertificateViewerScreenState extends State<CertificateViewerScreen> {
  static const _pdfMagic = [0x25, 0x50, 0x44, 0x46];
  static const _jpegMagic = [0xFF, 0xD8, 0xFF];
  static const _pngMagic = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
  static const _webpMagic = [0x52, 0x49, 0x46, 0x46];
  static const _supportedImageExt = {'jpg', 'jpeg', 'png', 'webp', 'gif'};

  bool _loading = true;
  Uint8List? _bytes;
  _BodyKind _kind = _BodyKind.loading;
  PdfControllerPinch? _pdfController;
  WebViewController? _webController;

  @override
  void initState() {
    super.initState();
    _prepare();
  }

  @override
  void dispose() {
    _pdfController?.dispose();
    super.dispose();
  }

  static String _pathExt(String url) {
    try {
      final path = Uri.parse(url).path;
      final i = path.lastIndexOf('.');
      if (i < 0 || i >= path.length - 1) return '';
      return path.substring(i + 1).toLowerCase();
    } catch (_) {
      return '';
    }
  }

  static _BodyKind _kindHintFromUrl(String url) {
    final ext = _pathExt(url);
    if (ext == 'pdf') return _BodyKind.pdf;
    if (_supportedImageExt.contains(ext)) return _BodyKind.image;
    return _BodyKind.web;
  }

  static _BodyKind _kindFromBytes(Uint8List bytes) {
    if (bytes.length >= 4 &&
        bytes[0] == _pdfMagic[0] &&
        bytes[1] == _pdfMagic[1] &&
        bytes[2] == _pdfMagic[2] &&
        bytes[3] == _pdfMagic[3]) {
      return _BodyKind.pdf;
    }
    if (bytes.length >= 3 &&
        bytes[0] == _jpegMagic[0] &&
        bytes[1] == _jpegMagic[1] &&
        bytes[2] == _jpegMagic[2]) {
      return _BodyKind.image;
    }
    if (bytes.length >= _pngMagic.length) {
      var png = true;
      for (int i = 0; i < _pngMagic.length; i++) {
        if (bytes[i] != _pngMagic[i]) {
          png = false;
          break;
        }
      }
      if (png) return _BodyKind.image;
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
      return _BodyKind.image;
    }
    return _BodyKind.web;
  }

  Future<void> _prepare() async {
    final hint = _kindHintFromUrl(widget.url);
    if (hint == _BodyKind.web) {
      if (mounted) _mountWebView();
      return;
    }

    if (mounted) {
      setState(() {
        _loading = true;
        _bytes = null;
        _pdfController?.dispose();
        _pdfController = null;
        _webController = null;
        _kind = _BodyKind.loading;
      });
    }

    try {
      final uri = Uri.parse(widget.url);
      final res = await http.get(uri).timeout(const Duration(seconds: 45));
      if (res.statusCode < 200 || res.statusCode >= 300) {
        throw Exception('HTTP ${res.statusCode}');
      }
      final data = res.bodyBytes;
      if (data.isEmpty) throw Exception('empty');

      final sniff = _kindFromBytes(data);
      final kind = sniff != _BodyKind.web ? sniff : hint;

      if (!mounted) return;

      if (kind == _BodyKind.pdf) {
        try {
          final ctrl = PdfControllerPinch(
            document: PdfDocument.openData(data),
            initialPage: 1,
          );
          setState(() {
            _pdfController = ctrl;
            _bytes = data;
            _kind = _BodyKind.pdf;
            _loading = false;
          });
        } catch (_) {
          if (mounted) _mountWebView();
        }
        return;
      }

      if (kind == _BodyKind.image) {
        setState(() {
          _bytes = data;
          _kind = _BodyKind.image;
          _loading = false;
        });
        return;
      }

      if (mounted) _mountWebView();
    } catch (_) {
      if (mounted) _mountWebView();
    }
  }

  void _mountWebView() {
    _pdfController?.dispose();
    _pdfController = null;
    final controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..loadRequest(Uri.parse(widget.url));
    setState(() {
      _webController = controller;
      _kind = _BodyKind.web;
      _loading = false;
      _bytes = null;
    });
  }

  Future<void> _retry() async {
    setState(() {
      _loading = true;
      _webController = null;
    });
    await _prepare();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      body: _buildBody(l10n),
    );
  }

  Widget _buildBody(AppLocalizations l10n) {
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

    if (_kind == _BodyKind.pdf && _pdfController != null) {
      return PdfViewPinch(controller: _pdfController!);
    }

    if (_kind == _BodyKind.image && _bytes != null) {
      return InteractiveViewer(
        minScale: 0.5,
        maxScale: 4.0,
        child: Center(
          child: Image.memory(
            _bytes!,
            fit: BoxFit.contain,
            errorBuilder: (_, __, ___) => _unsupportedWithRetry(l10n),
          ),
        ),
      );
    }

    if (_kind == _BodyKind.web && _webController != null) {
      return WebViewWidget(controller: _webController!);
    }

    return _unsupportedWithRetry(l10n);
  }

  Widget _unsupportedWithRetry(AppLocalizations l10n) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.insert_drive_file, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              l10n.translate('unsupportedDocumentType'),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            ShifaPrimaryButton(
              label: l10n.retry,
              onPressed: _retry,
              width: ButtonWidth.hug,
            ),
          ],
        ),
      ),
    );
  }
}
