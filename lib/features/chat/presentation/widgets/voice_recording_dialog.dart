// lib/features/chat/presentation/widgets/voice_recording_dialog.dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:record/record.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

class _VoiceRecordingDialog extends StatefulWidget {
  final Function(String filePath, int durationSeconds) onRecordingComplete;
  final VoidCallback onCancel;

  const _VoiceRecordingDialog({
    required this.onRecordingComplete,
    required this.onCancel,
  });

  @override
  State<_VoiceRecordingDialog> createState() => _VoiceRecordingDialogState();
}

class _VoiceRecordingDialogState extends State<_VoiceRecordingDialog> {
  final AudioRecorder _audioRecorder = AudioRecorder();
  bool _isRecording = false;
  String? _recordingPath;
  Timer? _timer;
  Duration _duration = Duration.zero;
  double _slideOffset = 0.0;
  bool _shouldCancel = false;

  @override
  void initState() {
    super.initState();
    _startRecording();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _audioRecorder.dispose();
    super.dispose();
  }

  Future<void> _startRecording() async {
    try {
      if (await _audioRecorder.hasPermission()) {
        final directory = await getTemporaryDirectory();
        final timestamp = DateTime.now().millisecondsSinceEpoch;
        _recordingPath = '${directory.path}/voice_$timestamp.m4a';
        
        await _audioRecorder.start(
          const RecordConfig(
            encoder: AudioEncoder.aacLc,
            bitRate: 32000,
            sampleRate: 16000,
            numChannels: 1,
          ),
          path: _recordingPath!,
        );

        setState(() => _isRecording = true);
        _startTimer();
      } else {
        if (mounted) {
          final l10n = AppLocalizations.of(context)!;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(l10n.translate('microphonePermissionDenied')),
              backgroundColor: Colors.red,
            ),
          );
        }
        widget.onCancel();
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.translate('errorRecordingVoice')),
            backgroundColor: Colors.red,
          ),
        );
      }
      widget.onCancel();
    }
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _duration = Duration(seconds: _duration.inSeconds + 1);
      });
    });
  }

  String _formatDuration(Duration duration) {
    final minutes = duration.inMinutes;
    final seconds = duration.inSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  Future<void> _stopRecording({bool cancel = false}) async {
    _timer?.cancel();
    await _audioRecorder.stop();
    
    if (cancel || _shouldCancel) {
      // Delete recording file
      if (_recordingPath != null) {
        final file = File(_recordingPath!);
        if (await file.exists()) {
          await file.delete();
        }
      }
      widget.onCancel();
    } else if (_recordingPath != null) {
      widget.onRecordingComplete(_recordingPath!, _duration.inSeconds);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final brand = Theme.of(context).colorScheme.primary;

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Recording indicator
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.mic,
                size: 40,
                color: Colors.red.shade700,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              l10n.translate('voiceMessage'),
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              _formatDuration(_duration),
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: brand,
              ),
            ),
            const SizedBox(height: 24),
            // Instructions
            Text(
              _shouldCancel
                  ? l10n.translate('slideUpToCancel')
                  : l10n.translate('slideLeftToCancel'),
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey.shade600,
              ),
            ),
            const SizedBox(height: 16),
            // Action buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Cancel button
                TextButton(
                  onPressed: () => _stopRecording(cancel: true),
                  child: Text(l10n.cancel),
                ),
                const SizedBox(width: 16),
                // Stop and send button
                FilledButton.icon(
                  onPressed: () => _stopRecording(),
                  icon: const Icon(Icons.send),
                  label: Text(l10n.translate('sendVoice')),
                  style: FilledButton.styleFrom(
                    backgroundColor: brand,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// Export as VoiceRecordingDialog
class VoiceRecordingDialog extends _VoiceRecordingDialog {
  const VoiceRecordingDialog({
    required super.onRecordingComplete,
    required super.onCancel,
  });
}
