// lib/features/bookings/presentation/screens/sign_appointment_screen.dart
import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' show parseAppointmentDateTime;
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';

class SignAppointmentScreen extends ConsumerStatefulWidget {
  const SignAppointmentScreen({super.key, required this.appointmentId});
  final String appointmentId;

  @override
  ConsumerState<SignAppointmentScreen> createState() => _SignAppointmentScreenState();
}

class _SignAppointmentScreenState extends ConsumerState<SignAppointmentScreen> {
  final List<List<Offset>> _strokes = [];
  final GlobalKey _signatureKey = GlobalKey();
  bool _loading = true;
  bool _submitting = false;
  String? _error;
  Map<String, dynamic>? _summary;

  @override
  void initState() {
    super.initState();
    _loadSummary();
  }

  Future<void> _loadSummary() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final repo = ref.read(bookingsProvider.notifier);
      final summary = await repo.getAppointmentSummary(widget.appointmentId);
      if (mounted) {
        setState(() {
          _summary = summary;
          _loading = false;
          if (summary['alreadySigned'] == true) {
            _error = AppLocalizations.of(context)!.translate('signatureAlreadySubmitted');
          }
        });
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        setState(() {
          _loading = false;
          _error = userFriendlyError(l10n, e, logContext: 'Sign appointment');
        });
      }
    }
  }

  void _clearSignature() {
    setState(() => _strokes.clear());
  }

  Future<Uint8List?> _captureSignaturePng() async {
    try {
      final ro = _signatureKey.currentContext?.findRenderObject();
      if (ro is! RenderRepaintBoundary) return null;
      final boundary = ro;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      return byteData?.buffer.asUint8List();
    } catch (e) {
      AppLogger.debug('Capture signature error: $e');
      return null;
    }
  }

  Future<void> _submitSignature() async {
    if (_strokes.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppLocalizations.of(context)!.pleaseSignAbove),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      final pngBytes = await _captureSignaturePng();
      if (pngBytes == null || pngBytes.isEmpty) {
        if (mounted) {
          setState(() => _submitting = false);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(AppLocalizations.of(context)!.errorSaving),
              backgroundColor: Colors.red,
            ),
          );
        }
        return;
      }
      final base64 = base64Encode(pngBytes);
      final repo = ref.read(bookingsProvider.notifier);
      await repo.submitSignature(widget.appointmentId, base64);
      if (!mounted) return;
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppLocalizations.of(context)!.signatureSubmittedSuccess),
          backgroundColor: Colors.green,
        ),
      );
      context.pop();
    } catch (e) {
      if (mounted) {
        setState(() => _submitting = false);
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.errorSaving}: ${userFriendlyError(l10n, e, logContext: 'Sign appointment')}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.signAppointmentSummary),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _summary == null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.error_outline, size: 48, color: Colors.red.shade300),
                        const SizedBox(height: 16),
                        Text(_error!, textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        ShifaSecondaryButton(
                          label: l10n.close,
                          onPressed: () => context.pop(),
                        ),
                      ],
                    ),
                  ),
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Scrollable summary only; signature pad stays outside scroll so it gets touches
                    Expanded(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (_summary != null) ...[
                              Text(
                                l10n.appointmentSummaryPreview,
                                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                      fontWeight: FontWeight.bold,
                                    ),
                              ),
                              const SizedBox(height: 8),
                              Card(
                                child: Padding(
                                  padding: const EdgeInsets.all(16),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      _summaryRow(l10n.doctor, _summary!['doctorName']?.toString()),
                                      _summaryRow(l10n.date, _formatDate(_summary!['startAt'])),
                                      _summaryRow(l10n.time, _formatTime(_summary!['startAt'], _summary!['endAt'])),
                                      if (_summary!['location'] != null)
                                        _summaryRow(l10n.location, _summary!['location']?.toString()),
                                      if (_summary!['reason'] != null && _summary!['reason'].toString().isNotEmpty)
                                        _summaryRow(l10n.reason, _summary!['reason']?.toString()),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                l10n.confirmAppointmentSummaryReflectsDiscussion,
                                style: Theme.of(context).textTheme.bodyMedium,
                              ),
                              const SizedBox(height: 16),
                              Text(
                                l10n.yourSignature,
                                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                      fontWeight: FontWeight.bold,
                                    ),
                              ),
                              const SizedBox(height: 8),
                            ],
                          ],
                        ),
                      ),
                    ),
                    // Signature pad + buttons: fixed below scroll, so no gesture conflict
                    if (_summary != null)
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            RepaintBoundary(
                              key: _signatureKey,
                              child: Container(
                                height: 180,
                                decoration: BoxDecoration(
                                  border: Border.all(color: Colors.grey.shade400),
                                  borderRadius: BorderRadius.circular(8),
                                  color: Colors.grey.shade50,
                                ),
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(8),
                                  child: Listener(
                                    behavior: HitTestBehavior.opaque,
                                    onPointerDown: (event) {
                                      setState(() =>
                                          _strokes.add([event.localPosition]));
                                    },
                                    onPointerMove: (event) {
                                      setState(() {
                                        if (_strokes.isNotEmpty) {
                                          _strokes.last.add(event.localPosition);
                                        }
                                      });
                                    },
                                    child: LayoutBuilder(
                                      builder: (context, constraints) {
                                        return CustomPaint(
                                          painter: _SignaturePainter(strokes: _strokes),
                                          size: Size(constraints.maxWidth, constraints.maxHeight),
                                        );
                                      },
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(height: 12),
                            Row(
                              children: [
                                ShifaSecondaryButton(
                                  label: l10n.clear,
                                  icon: Icons.clear,
                                  onPressed: _strokes.isEmpty ? null : _clearSignature,
                                  width: ButtonWidth.hug,
                                ),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: ShifaPrimaryButton(
                                    label: l10n.confirm ?? 'Confirm',
                                    icon: Icons.check,
                                    onPressed: _submitting ? null : _submitSignature,
                                    isLoading: _submitting,
                                    width: ButtonWidth.hug,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
    );
  }

  Widget _summaryRow(String label, String? value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: RichText(
        text: TextSpan(
          style: TextStyle(color: Theme.of(context).textTheme.bodyMedium?.color, fontSize: 14),
          children: [
            TextSpan(
              text: '$label: ',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            TextSpan(text: value ?? '—'),
          ],
        ),
      ),
    );
  }

  String _formatDate(dynamic startAt) {
    if (startAt == null) return '—';
    try {
      final localDt = parseAppointmentDateTime(startAt.toString());
      return DateFormat.yMd().format(localDt);
    } catch (_) {
      final s = startAt.toString();
      return s.length >= 10 ? s.substring(0, 10) : s;
    }
  }

  String _formatTime(dynamic startAt, dynamic endAt) {
    if (startAt == null) return '—';
    try {
      final startLocal = parseAppointmentDateTime(startAt.toString());
      final startTime = DateFormat('HH:mm').format(startLocal);
      if (endAt != null && endAt.toString().isNotEmpty) {
        final endLocal = parseAppointmentDateTime(endAt.toString());
        return '$startTime – ${DateFormat('HH:mm').format(endLocal)}';
      }
      return startTime;
    } catch (_) {
      final s = startAt.toString();
      final e = endAt?.toString() ?? '';
      final startTime = s.length >= 16 ? s.substring(11, 16) : (s.length >= 11 ? s.substring(11) : '');
      final endTime = e.length >= 16 ? e.substring(11, 16) : (e.length >= 11 ? e.substring(11) : '');
      if (endTime.isNotEmpty) return '$startTime – $endTime';
      return startTime;
    }
  }
}

class _SignaturePainter extends CustomPainter {
  _SignaturePainter({required this.strokes});
  final List<List<Offset>> strokes;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;
    for (final stroke in strokes) {
      if (stroke.length < 2) continue;
      for (var i = 0; i < stroke.length - 1; i++) {
        canvas.drawLine(stroke[i], stroke[i + 1], paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _SignaturePainter old) => old.strokes != strokes;
}
