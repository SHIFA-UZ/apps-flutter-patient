// Mobile/native video call using Daily Flutter SDK.
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:daily_flutter/daily_flutter.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/daily_video_service.dart';

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
  ConsumerState<VideoCallScreen> createState() => _VideoCallScreenState();
}

class _VideoCallScreenState extends ConsumerState<VideoCallScreen> {
  // Daily.co video call state
  CallClient? _callClient;
  DailyVideoService? _videoService;
  bool _isVideoInitialized = false;
  bool _isVideoLoading = true;
  bool _isMuted = false;
  bool _isVideoOff = false;
  String? _videoErrorRaw;
  StreamSubscription? _eventSubscription;
  final Map<String, VideoViewController> _videoControllers = {};
  int _joinRetryCount = 0;

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

  /// Requests OS camera + microphone access before [CallClient.join] so iOS can grant
  /// permissions before Daily initializes WebRTC (avoids a null local video track).
  Future<bool> _ensureCallMediaPermissions() async {
    var cam = await Permission.camera.status;
    if (!cam.isGranted) cam = await Permission.camera.request();
    var mic = await Permission.microphone.status;
    if (!mic.isGranted) mic = await Permission.microphone.request();
    return cam.isGranted && mic.isGranted;
  }

  void _syncMediaToggleStateFromClient() {
    if (_callClient == null || !mounted) return;
    final inputs = _callClient!.inputs;
    setState(() {
      _isVideoOff = !inputs.camera.isEnabled;
      _isMuted = !inputs.microphone.isEnabled;
    });
  }

  /// If the local camera track is still missing shortly after join (iOS timing), nudge inputs once.
  Future<void> _retryCameraIfNeededAfterJoin() async {
    if (_callClient == null) return;
    await Future<void>.delayed(const Duration(milliseconds: 600));
    if (!mounted || _callClient == null) return;
    final local = _callClient!.participants.local;
    final media = local.media;
    final track = media?.camera.track;
    if (track != null) return;
    try {
      await _callClient!.updateInputs(
        inputs: InputSettingsUpdate.set(
          camera: CameraInputSettingsUpdate.set(
            isEnabled: BoolUpdate.set(true),
          ),
        ),
      );
      if (mounted) setState(() {});
    } catch (e) {
      debugPrint('Video call: camera retry after join failed: $e');
    }
  }

  Future<void> _initializeVideoCall() async {
    ref.read(appLockTemporaryDisableProvider.notifier).disable();

    try {
      setState(() {
        _isVideoLoading = true;
        _videoErrorRaw = null;
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

      final permitted = await _ensureCallMediaPermissions();
      if (!permitted) {
        throw Exception(
          'Camera and microphone access are required for video consultations. '
          'You can enable them in your device Settings.',
        );
      }

      _callClient = await CallClient.create();
      _eventSubscription = _callClient!.events.listen(_handleCallEvent);

      await _callClient!.join(
        url: Uri.parse(tokenData.roomUrl),
        token: tokenData.token,
        clientSettings: ClientSettingsUpdate.set(
          inputs: InputSettingsUpdate.set(
            camera: CameraInputSettingsUpdate.set(
              isEnabled: BoolUpdate.set(true),
            ),
            microphone: MicrophoneInputSettingsUpdate.set(
              isEnabled: BoolUpdate.set(true),
            ),
          ),
          publishing: PublishingSettingsUpdate.set(
            camera: CameraPublishingSettingsUpdate.set(
              isPublishing: BoolUpdate.set(true),
            ),
            microphone: MicrophonePublishingSettingsUpdate.set(
              isPublishing: BoolUpdate.set(true),
            ),
          ),
        ),
      );

      setState(() {
        _isVideoInitialized = true;
        _isVideoLoading = false;
        _joinRetryCount = 0;
      });
      _syncMediaToggleStateFromClient();
      unawaited(_retryCameraIfNeededAfterJoin());

      WidgetsBinding.instance.addPostFrameCallback((_) {
        _selectSpeakerOutput();
      });
    } catch (e) {
      debugPrint('Failed to initialize video call: $e');

      final isTimeout = _isJoinTimeout(e);
      final shouldRetry = isTimeout && _joinRetryCount < 1;

      if (shouldRetry && mounted) {
        _joinRetryCount++;
        setState(() {
          _isVideoLoading = true;
          _videoErrorRaw = null;
        });
        await Future<void>.delayed(const Duration(seconds: 2));
        if (mounted) _initializeVideoCall();
        return;
      }

      if (!mounted) return;
      final raw = _unwrapVideoErrorMessage(e);
      setState(() {
        _videoErrorRaw = raw;
        _isVideoLoading = false;
        _isVideoInitialized = false;
      });

      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        final friendly = translateError(l10n, raw);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${l10n.translate('failedToStartVideoCall') ?? 'Failed to start video call'}: $friendly',
            ),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  /// Prefer loudspeaker over earpiece for video calls so both speakers work.
  void _selectSpeakerOutput() {
    if (_callClient == null) return;
    try {
      final client = _callClient!;
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
    // Handle events - the actual Event type uses freezed unions
    event.whenOrNull?.call(
      availableDevicesUpdated: (availableDevices) {
        _selectSpeakerOutput();
      },
      inputsUpdated: (inputs) {
        if (mounted) {
          setState(() {
            _isVideoOff = !inputs.camera.isEnabled;
            _isMuted = !inputs.microphone.isEnabled;
          });
        }
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
                _videoErrorRaw = 'Call error occurred';
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
    if (_callClient == null) return;

    try {
      await _callClient!.updateInputs(
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
    if (_callClient != null) {
      try {
        await _callClient!.leave();
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
    if (_callClient == null) {
      return const Center(child: CircularProgressIndicator());
    }

    final participantsObj = _callClient!.participants;
    final localParticipant = participantsObj.local;
    final remoteParticipants = participantsObj.remote.values.toList();

    return Container(
      color: Colors.black,
      child: remoteParticipants.isEmpty
          ? _buildSingleParticipantView(localParticipant)
          : _buildMultipleParticipantsView(localParticipant, remoteParticipants),
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
                  : Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.videocam_off, size: 64, color: Colors.white54),
                          const SizedBox(height: 16),
                          Text(
                            l10n.translate('videoCallNotAvailableShort') ??
                                'Video call not available',
                            style: const TextStyle(color: Colors.white70, fontSize: 18),
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
