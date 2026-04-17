// lib/features/bookings/presentation/screens/video_call_screen.dart
import 'dart:async';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
// Conditional import: daily_flutter only works on native platforms, not web
import 'package:daily_flutter/daily_flutter.dart' if (dart.library.html) 'package:shifa_patient_app_v1/core/services/daily_flutter_stub.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/daily_video_service.dart';

class VideoCallScreen extends ConsumerStatefulWidget {
  final String appointmentId;

  const VideoCallScreen({super.key, required this.appointmentId});

  @override
  ConsumerState<VideoCallScreen> createState() => _VideoCallScreenState();
}

class _VideoCallScreenState extends ConsumerState<VideoCallScreen> {
  // Daily.co video call state
  dynamic _callClient; // CallClient on mobile, null on web
  DailyVideoService? _videoService;
  bool _isVideoInitialized = false;
  bool _isVideoLoading = true;
  bool _isMuted = false;
  bool _isVideoOff = false;
  String? _videoError;
  StreamSubscription? _eventSubscription;
  String? _roomUrl; // For web
  String? _token; // For web
  Map<String, VideoViewController> _videoControllers = {}; // Track controllers for each participant
  int _joinRetryCount = 0; // One auto-retry on join timeout

  // Helper to reliably detect if we're on web platform
  bool get _isWebPlatform {
    if (kIsWeb) return true;
    if (_roomUrl != null && _token != null && _callClient == null) return true;
    if (_isVideoInitialized && _callClient == null && _roomUrl != null) return true;
    return false;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initializeVideoCall();
    });
  }

  @override
  void dispose() {
    // Re-enable app lock before disposing
    // Must be done before super.dispose() to avoid using ref after disposal
    try {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
    } catch (e) {
      // Ignore error if ref is already disposed
      debugPrint('Failed to re-enable app lock on dispose: $e');
    }

    _eventSubscription?.cancel();
    _videoControllers.values.forEach((controller) => controller.dispose());
    _videoControllers.clear();
    _endVideoCall();
    super.dispose();
  }

  /// True if the exception is a join timeout (Daily SDK 10s timeout).
  static bool _isJoinTimeout(Object e) {
    final s = e.toString().toLowerCase();
    return s.contains('timeoutexception') &&
        (s.contains('join failed') || s.contains('future not completed'));
  }

  /// User-friendly message for video call errors (timeout vs generic).
  String _userFriendlyError(Object e, AppLocalizations l10n) {
    if (_isJoinTimeout(e)) {
      return l10n.translate('videoCallConnectionTimeout') ??
          'Connection timed out. Please check your internet and try again.';
    }
    return userFriendlyError(l10n, e, logContext: 'Video call');
  }

  Future<void> _initializeVideoCall() async {
    ref.read(appLockTemporaryDisableProvider.notifier).disable();

    try {
      setState(() {
        _isVideoLoading = true;
        _videoError = null;
      });

      final apiClient = ref.read(apiClientProvider);
      _videoService = DailyVideoService(apiClient);

      final appointmentId = int.tryParse(widget.appointmentId) ?? 0;
      if (appointmentId == 0) {
        throw Exception('Invalid appointment ID');
      }

      final tokenData = await _videoService!.getVideoToken(
        appointmentId: appointmentId,
      );

      final bool isWebPlatform = kIsWeb;

      if (isWebPlatform) {
        _roomUrl = tokenData.roomUrl;
        _token = tokenData.token;
        setState(() {
          _isVideoInitialized = true;
          _isVideoLoading = false;
        });
      } else {
        _callClient = await CallClient.create();
        _eventSubscription = (_callClient as CallClient).events.listen((event) {
          _handleCallEvent(event);
        });

        await (_callClient as CallClient).join(
          url: Uri.parse(tokenData.roomUrl),
          token: tokenData.token,
        );

        setState(() {
          _isVideoInitialized = true;
          _isVideoLoading = false;
          _joinRetryCount = 0;
        });

        // Route audio to speaker (loudspeaker) so voice is not only in earpiece
        WidgetsBinding.instance.addPostFrameCallback((_) {
          _selectSpeakerOutput();
        });
      }
    } catch (e) {
      debugPrint('Failed to initialize video call: $e');

      final l10n = AppLocalizations.of(context)!;
      final isTimeout = _isJoinTimeout(e);
      final shouldRetry = isTimeout && _joinRetryCount < 1;

      if (shouldRetry && mounted) {
        _joinRetryCount++;
        setState(() {
          _isVideoLoading = true;
          _videoError = null;
        });
        await Future<void>.delayed(const Duration(seconds: 2));
        if (mounted) _initializeVideoCall();
        return;
      }

      setState(() {
        _videoError = _userFriendlyError(e, l10n);
        _isVideoLoading = false;
        _isVideoInitialized = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.translate('failedToStartVideoCall')}: ${_userFriendlyError(e, l10n)}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  /// Prefer loudspeaker over earpiece for video calls so both speakers work.
  void _selectSpeakerOutput() {
    if (_isWebPlatform || _callClient == null) return;
    try {
      final client = _callClient as CallClient;
      final devices = client.availableDevices;
      final speakers = devices.speaker;
      if (speakers.isEmpty) return;
      // Prefer device whose label suggests main speaker (e.g. "Speaker", "Loudspeaker") over earpiece
      var preferred = speakers.first;
      for (final d in speakers) {
        final l = d.label.toLowerCase();
        if (l.contains('speaker') || l.contains('loud') || l.contains('external')) {
          preferred = d;
          break;
        }
        if (l.contains('earpiece') || l.contains('receiver') || l.contains('ear')) continue;
        preferred = d;
      }
      client.setAudioDevice(deviceId: preferred.deviceId);
    } catch (e) {
      debugPrint('Video call: could not set speaker output: $e');
    }
  }

  void _handleCallEvent(dynamic event) {
    if (_isWebPlatform) {
      return;
    }
    
    // Handle events - the actual Event type uses freezed unions
    event.whenOrNull?.call(
      availableDevicesUpdated: (availableDevices) {
        _selectSpeakerOutput();
      },
      callStateUpdated: (stateData) {
        stateData.whenOrNull?.call(
          left: () {
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(AppLocalizations.of(context)!.translate('videoCallEnded')),
                ),
              );
              Navigator.of(context).pop();
            }
          },
          error: (error) {
            if (mounted) {
              setState(() {
                _videoError = 'Call error occurred: $error';
              });
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(AppLocalizations.of(context)!.translate('callErrorOccurred')),
                  backgroundColor: Colors.red,
                ),
              );
            }
          },
        );
      },
      participantJoined: (participant) {
        debugPrint('Participant joined: ${participant.id}');
        setState(() {}); // Trigger rebuild to show new participant
      },
      participantUpdated: (participant) {
        debugPrint('Participant updated: ${participant.id}');
        setState(() {}); // Trigger rebuild
      },
      participantLeft: (participant) {
        debugPrint('Participant left: ${participant.id}');
        // Clean up controller for this participant
        final controller = _videoControllers.remove(participant.id.toString());
        controller?.dispose();
        setState(() {}); // Trigger rebuild
      },
    );
  }

  Future<void> _toggleMute() async {
    if (_isWebPlatform || _callClient == null) {
      setState(() => _isMuted = !_isMuted);
      return;
    }
    
    try {
      // Use updateInputs to toggle microphone
      await (_callClient as CallClient).updateInputs(
        inputs: InputSettingsUpdate.set(
          microphone: MicrophoneInputSettingsUpdate.set(
            isEnabled: BoolUpdate.set(!_isMuted),
          ),
        ),
      );
      setState(() => _isMuted = !_isMuted);
    } catch (e) {
      debugPrint('Failed to toggle mute: $e');
    }
  }

  Future<void> _toggleVideo() async {
    if (_isWebPlatform || _callClient == null) {
      setState(() => _isVideoOff = !_isVideoOff);
      return;
    }
    
    try {
      // Use updateInputs to toggle camera
      await (_callClient as CallClient).updateInputs(
        inputs: InputSettingsUpdate.set(
          camera: CameraInputSettingsUpdate.set(
            isEnabled: BoolUpdate.set(!_isVideoOff),
          ),
        ),
      );
      setState(() => _isVideoOff = !_isVideoOff);
    } catch (e) {
      debugPrint('Failed to toggle video: $e');
    }
  }

  Future<void> _endVideoCall() async {
    if (_isWebPlatform) {
      _roomUrl = null;
      _token = null;
      setState(() {
        _isVideoInitialized = false;
      });
      return;
    }
    
    if (_callClient != null) {
      try {
        await (_callClient as CallClient).leave();
        await _eventSubscription?.cancel();
        _callClient = null;
        _eventSubscription = null;
        setState(() {
          _isVideoInitialized = false;
        });
      } catch (e) {
        debugPrint('Error ending call: $e');
      }
    }
  }

  Widget _buildVideoView() {
    if (_isWebPlatform) {
      if (_roomUrl == null || _token == null) {
        return const Center(child: CircularProgressIndicator());
      }
      return _buildWebVideoView();
    } else {
      if (_callClient == null) {
        return const Center(child: CircularProgressIndicator());
      }
      
      // Get participants - it's a getter, not a method
      final participantsObj = (_callClient as CallClient).participants;
      final localParticipant = participantsObj.local;
      final remoteParticipants = participantsObj.remote.values.toList();
      
      if (remoteParticipants.isEmpty && localParticipant == null) {
        return Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const CircularProgressIndicator(),
              const SizedBox(height: 16),
              Text(AppLocalizations.of(context)!.translate('waitingForParticipants'), style: const TextStyle(color: Colors.white)),
            ],
          ),
        );
      }
      
      return Container(
        color: Colors.black,
        child: remoteParticipants.isEmpty
            ? _buildSingleParticipantView(localParticipant)
            : _buildMultipleParticipantsView(localParticipant, remoteParticipants),
      );
    }
  }

  Widget _buildWebVideoView() {
    final iframeUrl = '$_roomUrl?t=$_token';
    return Container(
      color: Colors.black,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.videocam, size: 64, color: Colors.white54),
            const SizedBox(height: 16),
            Text(
              AppLocalizations.of(context)!.translate('videoCallReady'),
              style: const TextStyle(color: Colors.white, fontSize: 18),
            ),
            const SizedBox(height: 8),
            Text(
              AppLocalizations.of(context)!.translate('clickBelowToJoinCall'),
              style: const TextStyle(color: Colors.white70, fontSize: 14),
            ),
            const SizedBox(height: 24),
            ShifaPrimaryButton(
              label: AppLocalizations.of(context)!.translate('joinVideoCall'),
              icon: Icons.open_in_new,
              onPressed: () async {
                await launchUrlString(
                  iframeUrl,
                  mode: LaunchMode.externalApplication,
                );
              },
              width: ButtonWidth.hug,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSingleParticipantView(Participant? participant) {
    if (participant == null) {
      return const Center(
        child: Icon(Icons.person, size: 100, color: Colors.white54),
      );
    }
    
    // Get or create controller for this participant
    final participantId = participant.id.toString();
    final controller = _videoControllers.putIfAbsent(
      participantId,
      () => VideoViewController(),
    );
    
    // Set the track if available
    final videoTrack = participant.media?.camera.track;
    if (videoTrack != null) {
      controller.setTrack(videoTrack);
    }
    
    return Center(
      child: videoTrack != null
          ? VideoView(controller: controller)
          : const Center(
              child: Icon(Icons.person, size: 100, color: Colors.white54),
            ),
    );
  }

  Widget _buildMultipleParticipantsView(Participant? localParticipant, List<Participant> remoteParticipants) {
    if (remoteParticipants.isEmpty) {
      return _buildSingleParticipantView(localParticipant);
    }
    
    final remoteParticipant = remoteParticipants.first;
    
    // Get or create controllers
    final remoteId = remoteParticipant.id.toString();
    final remoteController = _videoControllers.putIfAbsent(
      remoteId,
      () => VideoViewController(),
    );
    
    final remoteVideoTrack = remoteParticipant.media?.camera.track;
    if (remoteVideoTrack != null) {
      remoteController.setTrack(remoteVideoTrack);
    }
    
    VideoViewController? localController;
    MediaStreamTrack? localVideoTrack;
    if (localParticipant != null) {
      final localId = localParticipant.id.toString();
      localController = _videoControllers.putIfAbsent(
        localId,
        () => VideoViewController(),
      );
      localVideoTrack = localParticipant.media?.camera.track;
      if (localVideoTrack != null) {
        localController!.setTrack(localVideoTrack);
      }
    }
    
    return Stack(
      children: [
        // Remote participant (large)
        Positioned.fill(
          child: remoteVideoTrack != null
            ? VideoView(controller: remoteController)
            : const Center(
                child: Icon(Icons.person, size: 100, color: Colors.white54),
              ),
        ),
        // Local participant (small, bottom right)
        if (localController != null && localVideoTrack != null)
          Positioned(
            bottom: 100,
            right: 16,
            child: Container(
              width: 120,
              height: 160,
              decoration: BoxDecoration(
                color: Colors.grey[800],
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.white, width: 2),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: VideoView(controller: localController),
              ),
            ),
          ),
      ],
    );
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
          onPressed: () {
            _endVideoCall();
            Navigator.of(context).pop();
          },
        ),
        title: Text(l10n.videoCall),
      ),
      body: _isVideoLoading
          ? const Center(child: CircularProgressIndicator())
          : _videoError != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, size: 64, color: Colors.red),
                      const SizedBox(height: 16),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24),
                        child: Text(
                          _videoError ?? '',
                          style: const TextStyle(color: Colors.white70, fontSize: 14),
                          textAlign: TextAlign.center,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ShifaPrimaryButton(
                        label: AppLocalizations.of(context)!.retry,
                        onPressed: _initializeVideoCall,
                        width: ButtonWidth.hug,
                      ),
                    ],
                  ),
                )
              : _isVideoInitialized
                  ? Stack(
                      children: [
                        _buildVideoView(),
                        // Controls - hide on web as Daily.co Prebuilt has its own controls
                        if (!_isWebPlatform)
                          Align(
                            alignment: Alignment.bottomCenter,
                            child: _CallControls(
                              onMute: _toggleMute,
                              onVideo: _toggleVideo,
                              onEndCall: () {
                                _endVideoCall();
                                Navigator.of(context).pop();
                              },
                              isMuted: _isMuted,
                              isVideoOff: _isVideoOff,
                            ),
                          ),
                      ],
                    )
                  : const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.videocam_off, size: 64, color: Colors.white54),
                          SizedBox(height: 16),
                          Text(
                            'Video call not available',
                            style: TextStyle(color: Colors.white70, fontSize: 18),
                          ),
                        ],
                      ),
                    ),
    );
  }
}

class _CallControls extends StatelessWidget {
  final VoidCallback onMute;
  final VoidCallback onVideo;
  final VoidCallback onEndCall;
  final bool isMuted;
  final bool isVideoOff;

  const _CallControls({
    required this.onMute,
    required this.onVideo,
    required this.onEndCall,
    required this.isMuted,
    required this.isVideoOff,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Mute button
          _ControlButton(
            icon: isMuted ? Icons.mic_off : Icons.mic,
            onPressed: onMute,
            backgroundColor: isMuted ? Colors.red : Colors.white24,
          ),
          // Video toggle button
          _ControlButton(
            icon: isVideoOff ? Icons.videocam_off : Icons.videocam,
            onPressed: onVideo,
            backgroundColor: isVideoOff ? Colors.red : Colors.white24,
          ),
          // End call button
          _ControlButton(
            icon: Icons.call_end,
            onPressed: onEndCall,
            backgroundColor: Colors.red,
            size: 56,
          ),
        ],
      ),
    );
  }
}

class _ControlButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onPressed;
  final Color backgroundColor;
  final double size;

  const _ControlButton({
    required this.icon,
    required this.onPressed,
    required this.backgroundColor,
    this.size = 48,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: backgroundColor,
        shape: BoxShape.circle,
      ),
      child: IconButton(
        icon: Icon(icon, color: Colors.white),
        onPressed: onPressed,
      ),
    );
  }
}
