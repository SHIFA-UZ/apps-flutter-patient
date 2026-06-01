// Web video call: Daily.co Prebuilt iframe embed.
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/daily_video_service.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/daily_video_embed_web.dart';

String _unwrapVideoErrorMessage(Object e) {
  var s = e.toString().trim();
  for (var i = 0; i < 8; i++) {
    final next = s.replaceFirst(RegExp(r'^Exception:\s*'), '').trim();
    if (next == s) break;
    s = next;
  }
  return s;
}

class VideoCallScreen extends ConsumerStatefulWidget {
  final String appointmentId;

  const VideoCallScreen({super.key, required this.appointmentId});

  @override
  ConsumerState<VideoCallScreen> createState() => _VideoCallScreenWebState();
}

class _VideoCallScreenWebState extends ConsumerState<VideoCallScreen> {
  bool _isVideoInitialized = false;
  bool _isVideoLoading = true;
  String? _videoErrorRaw;
  String? _roomUrl;
  String? _token;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _initializeVideoCall());
  }

  @override
  void dispose() {
    try {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
    } catch (_) {}
    super.dispose();
  }

  Future<void> _initializeVideoCall() async {
    ref.read(appLockTemporaryDisableProvider.notifier).disable();

    try {
      setState(() {
        _isVideoLoading = true;
        _videoErrorRaw = null;
      });

      final apiClient = ref.read(apiClientProvider);
      final videoService = DailyVideoService(apiClient);

      final appointmentId = int.tryParse(widget.appointmentId) ?? 0;
      if (appointmentId == 0) {
        throw Exception('Invalid appointment ID');
      }

      final tokenData = await videoService.getVideoToken(
        appointmentId: appointmentId,
      );

      if (!mounted) return;
      setState(() {
        _roomUrl = tokenData.roomUrl;
        _token = tokenData.token;
        _isVideoInitialized = true;
        _isVideoLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      final raw = _unwrapVideoErrorMessage(e);
      setState(() {
        _videoErrorRaw = raw;
        _isVideoLoading = false;
        _isVideoInitialized = false;
      });

      final l10n = AppLocalizations.of(context)!;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            '${l10n.translate('failedToStartVideoCall') ?? 'Failed to start video call'}: ${translateError(l10n, raw)}',
          ),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: const Color(0xFF17C3B2),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(l10n.videoCall),
      ),
      body: _isVideoLoading
          ? const Center(child: CircularProgressIndicator())
          : _videoErrorRaw != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, size: 64, color: Colors.red),
                      const SizedBox(height: 16),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24),
                        child: Text(
                          translateError(l10n, _videoErrorRaw!),
                          style: const TextStyle(color: Colors.white70, fontSize: 14),
                          textAlign: TextAlign.center,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ShifaPrimaryButton(
                        label: l10n.retry,
                        onPressed: _initializeVideoCall,
                        width: ButtonWidth.hug,
                      ),
                    ],
                  ),
                )
              : _isVideoInitialized && _roomUrl != null && _token != null
                  ? DailyVideoEmbedWeb(roomUrl: _roomUrl!, token: _token!)
                  : Center(
                      child: Text(
                        l10n.translate('videoCallNotAvailableShort') ??
                            'Video call not available',
                        style: const TextStyle(color: Colors.white70, fontSize: 18),
                      ),
                    ),
    );
  }
}
