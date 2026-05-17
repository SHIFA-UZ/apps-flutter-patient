import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/subscription/patient_subscription.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/state/subscription/patient_subscription_provider.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/voice_recording_dialog.dart';
import 'package:shifa_patient_app_v1/features/copilot/providers/copilot_api_provider.dart';
import 'package:shifa_patient_app_v1/features/copilot/providers/copilot_chat_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

String copilotBackendLanguage(String? profileLanguage) {
  final l = (profileLanguage ?? 'en').toLowerCase();
  if (l.startsWith('ru')) return 'RU';
  if (l.startsWith('uz')) return 'UZ';
  return 'EN';
}

class ShifaAiScreen extends ConsumerStatefulWidget {
  const ShifaAiScreen({super.key});

  @override
  ConsumerState<ShifaAiScreen> createState() => _ShifaAiScreenState();
}

class _ShifaAiScreenState extends ConsumerState<ShifaAiScreen> {
  final _textController = TextEditingController();
  final _scrollController = ScrollController();
  List<DoctorModel> _suggestedDoctors = [];
  bool _loadingSuggestions = false;

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _onSend() async {
    final text = _textController.text;
    if (text.trim().isEmpty) return;
    final userText = text.trim();
    _textController.clear();
    final l10n = AppLocalizations.of(context)!;
    final lang = copilotBackendLanguage(ref.read(profileProvider).profile?.language);
    await ref.read(copilotChatProvider.notifier).sendUserMessage(userText, lang);
    final suggested = await ref.read(copilotChatProvider.notifier).autoSuggestDoctorsFromChat(lang);
    final preview = await ref.read(copilotChatProvider.notifier).resolveBookingPreviewFromChat(lang);
    if (!mounted) return;
    setState(() => _suggestedDoctors = suggested);
    _scrollToBottom();
    if (preview != null && preview.isNotEmpty) {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.translate('copilotConfirmBookFromChatTitle')),
          content: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(preview, style: AppDesignSystem.body1),
                const SizedBox(height: 12),
                Text(
                  l10n.translate('copilotConfirmBookFromChatExplainer'),
                  style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
            TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.translate('confirm')),
            ),
          ],
        ),
      );
      if (ok == true && mounted) {
        await ref.read(copilotChatProvider.notifier).confirmBookingFromChat(lang);
        _scrollToBottom();
      }
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 200),
        curve: Curves.easeOut,
      );
    });
  }

  Future<void> _onClearChat() async {
    final l10n = AppLocalizations.of(context)!;
    final chatState = ref.read(copilotChatProvider);
    if (chatState.isSending) return;
    if (chatState.messages.isEmpty &&
        _suggestedDoctors.isEmpty &&
        (chatState.streamingText == null || chatState.streamingText!.isEmpty)) {
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('copilotClearChatTitle')),
        content: Text(l10n.translate('copilotClearChatMessage')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.translate('copilotClearChatConfirm')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    _textController.clear();
    await ref.read(copilotChatProvider.notifier).clearConversation();
    if (!mounted) return;
    setState(() => _suggestedDoctors = []);
    if (_scrollController.hasClients) {
      _scrollController.jumpTo(0);
    }
  }

  Future<void> _onSuggestDoctors() async {
    final l10n = AppLocalizations.of(context)!;
    final chat = ref.read(copilotChatProvider);
    if (chat.messages.where((m) => m.role == 'user').isEmpty &&
        _textController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('copilotInputHint'))),
      );
      return;
    }

    setState(() => _loadingSuggestions = true);
    try {
      final lang = copilotBackendLanguage(ref.read(profileProvider).profile?.language);
      final list = await ref
          .read(copilotChatProvider.notifier)
          .autoSuggestDoctorsFromChat(lang);
      if (!mounted) return;
      setState(() {
        _suggestedDoctors = list;
        _loadingSuggestions = false;
      });
      if (list.isEmpty && mounted) {
        // The provider already injects the clarifying question as an assistant turn when the server
        // needs more info; only show the "no provider on platform" message if the specialty was
        // inferred but no matching doctor was found.
        final chatAfter = ref.read(copilotChatProvider);
        final lastIds = chatAfter.lastSuggestedDoctorIds;
        final lastMsg = chatAfter.messages.isNotEmpty ? chatAfter.messages.last : null;
        final lastWasAssistantQuestion = lastMsg?.role == 'assistant' &&
            (lastMsg?.content.trim().endsWith('?') ?? false);
        if (lastIds != null && lastIds.isEmpty && !lastWasAssistantQuestion) {
          ref.read(copilotChatProvider.notifier).appendAssistantMessage(
                l10n.translate('copilotNoProviderOnPlatform'),
              );
        }
      }
      _scrollToBottom();
    } catch (e) {
      setState(() => _loadingSuggestions = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    }
  }

  Widget _buildDoctorSuggestionsBubble(BuildContext context, AppLocalizations l10n) {
    final btnStyle = TextButton.styleFrom(
      minimumSize: Size.zero,
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      visualDensity: VisualDensity.compact,
    );
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.92),
        decoration: BoxDecoration(
          color: Colors.grey.shade200,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              l10n.translate('copilotSuggestedDoctors'),
              style: AppDesignSystem.caption.copyWith(
                fontWeight: FontWeight.w700,
                color: AppDesignSystem.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            for (final d in _suggestedDoctors) ...[
              _DoctorSuggestionCard(
                doctor: d,
                photoUrl: withCacheBuster(d.photoUrl, null),
                btnStyle: btnStyle,
                onProfile: () => context.push('${AppRoutes.doctors}/${d.id}'),
                onManualBook: () => _openManualBooking(d),
                onAutoBook: () => _showAutoBookSheet(d),
                nextSlotLabel: l10n.translate('copilotNextSlot'),
                confidenceLabel: l10n.translate('copilotConfidence'),
                confidenceHigh: l10n.translate('copilotConfidenceHigh'),
                confidenceMedium: l10n.translate('copilotConfidenceMedium'),
                confidenceLow: l10n.translate('copilotConfidenceLow'),
                profileLabel: l10n.translate('copilotViewProfile'),
                bookManualLabel: l10n.translate('copilotBookManual'),
                autoBookLabel: l10n.translate('copilotAutoBook'),
              ),
              const SizedBox(height: 8),
            ],
          ],
        ),
      ),
    );
  }

  List<String> _quickFollowUpOptions(String? assistantText) {
    final t = (assistantText ?? '').toLowerCase();
    if (t.contains('video consultation') || t.contains('in-person') || t.contains('visit type')) {
      return const ['Video consultation', 'In-person visit'];
    }
    if (t.contains('date and time') || t.contains('future preferred')) {
      return const ['Tomorrow morning', 'Tomorrow afternoon', 'This weekend'];
    }
    return const [];
  }

  Future<void> _transcribeVoice() async {
    final l10n = AppLocalizations.of(context)!;
    await showDialog<void>(
      context: context,
      builder: (ctx) => VoiceRecordingDialog(
        onRecordingComplete: (filePath, _) async {
          Navigator.of(ctx).pop();
          try {
            final lang = (ref.read(profileProvider).profile?.language ?? 'en').toLowerCase();
            final hint = lang.startsWith('uz')
                ? 'uz'
                : lang.startsWith('ru')
                    ? 'ru'
                    : 'en';
            final text = await ref.read(copilotApiProvider).transcribeAudio(filePath, languageHint: hint);
            if (mounted) {
              setState(() {
                _textController.text = text;
                _textController.selection = TextSelection.fromPosition(
                  TextPosition(offset: _textController.text.length),
                );
              });
            }
          } catch (e) {
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('${l10n.translate('copilotTranscribeError')}: $e')),
              );
            }
          }
        },
        onCancel: () => Navigator.of(ctx).pop(),
      ),
    );
  }

  Future<void> _openManualBooking(DoctorModel doctor) async {
    final l10n = AppLocalizations.of(context)!;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('copilotBookingTitle')),
        content: Text(l10n.translate('copilotContinueToBooking')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(l10n.cancel)),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text(l10n.translate('confirm'))),
        ],
      ),
    );
    if (ok == true && mounted) {
      context.push('${AppRoutes.bookings}/flow/${doctor.id}');
    }
  }

  Future<void> _showAutoBookSheet(DoctorModel doctor) async {
    final l10n = AppLocalizations.of(context)!;
    final rootContext = context;
    final now = DateTime.now();
    var chosen = DateTime(now.year, now.month, now.day, now.hour + 1, 0);
    var consent = false;
    var isVideo = false;
    var loading = false;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetCtx) {
        return StatefulBuilder(
          builder: (ctx, setModalState) {
            Future<void> pickDate() async {
              final d = await showDatePicker(
                context: ctx,
                initialDate: chosen,
                firstDate: DateTime.now(),
                lastDate: DateTime.now().add(const Duration(days: 365)),
              );
              if (d != null) {
                chosen = DateTime(d.year, d.month, d.day, chosen.hour, chosen.minute);
                setModalState(() {});
              }
            }

            Future<void> pickTime() async {
              final t = await showTimePicker(
                context: ctx,
                initialTime: TimeOfDay.fromDateTime(chosen),
              );
              if (t != null) {
                chosen = DateTime(chosen.year, chosen.month, chosen.day, t.hour, t.minute);
                setModalState(() {});
              }
            }

            return Padding(
              padding: EdgeInsets.only(
                left: 20,
                right: 20,
                top: 16,
                bottom: 16 + MediaQuery.viewInsetsOf(ctx).bottom,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(doctor.fullName, style: AppDesignSystem.h2),
                  const SizedBox(height: 8),
                  Text(
                    l10n.translate('copilotAutoBookExplainer'),
                    style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                  ),
                  const SizedBox(height: 16),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(l10n.translate('copilotPreferredDate')),
                    subtitle: Text(DateFormat.yMMMd().format(chosen)),
                    trailing: const Icon(Icons.calendar_today),
                    onTap: loading ? null : () => pickDate(),
                  ),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(l10n.translate('copilotPreferredTime')),
                    subtitle: Text(
                      MaterialLocalizations.of(ctx).formatTimeOfDay(TimeOfDay.fromDateTime(chosen)),
                    ),
                    trailing: const Icon(Icons.schedule),
                    onTap: loading ? null : () => pickTime(),
                  ),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(l10n.translate('videoConsultation')),
                    value: isVideo,
                    onChanged: loading ? null : (v) => setModalState(() => isVideo = v),
                  ),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(l10n.translate('copilotConsentAutoBook')),
                    value: consent,
                    onChanged: loading ? null : (v) => setModalState(() => consent = v ?? false),
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: loading || !consent
                        ? null
                        : () async {
                            setModalState(() => loading = true);
                            try {
                              final local = DateTime(
                                chosen.year,
                                chosen.month,
                                chosen.day,
                                chosen.hour,
                                chosen.minute,
                              );
                              await ref.read(copilotApiProvider).bookCopilotAppointment(
                                    doctorId: doctor.id,
                                    preferredStartUtc: local.toUtc(),
                                    isVideo: isVideo,
                                    reason: l10n.translate('copilotBookedViaAiReason'),
                                    consentConfirmed: true,
                                  );
                              ref.invalidate(bookingsProvider);
                              if (sheetCtx.mounted) Navigator.of(sheetCtx).pop();
                              if (rootContext.mounted) {
                                ScaffoldMessenger.of(rootContext).showSnackBar(
                                  SnackBar(content: Text(l10n.translate('copilotBookedSuccess'))),
                                );
                              }
                            } catch (e) {
                              if (sheetCtx.mounted) {
                                ScaffoldMessenger.of(sheetCtx).showSnackBar(
                                  SnackBar(content: Text(e.toString())),
                                );
                              }
                            } finally {
                              if (ctx.mounted) setModalState(() => loading = false);
                            }
                          },
                    child: loading
                        ? const SizedBox(
                            height: 22,
                            width: 22,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : Text(l10n.translate('copilotAutoBookSubmit')),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final canUseShifaAi = ref.watch(patientFeatureProvider(PatientFeature.shifaAi));
    // Defensive guard for direct navigation (deep link, refresh, etc.). The
    // bottom-bar FAB and router redirect both already gate this, but if a PRO
    // patient lands here anyway we bounce back to home on the next frame.
    if (!canUseShifaAi) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) context.go(AppRoutes.home);
      });
      return const Scaffold(body: SizedBox.shrink());
    }
    final chatState = ref.watch(copilotChatProvider);
    ref.listen<CopilotChatState>(copilotChatProvider, (prev, next) {
      if (next.messages.length != (prev?.messages.length ?? 0) ||
          next.streamingText != prev?.streamingText) {
        _scrollToBottom();
      }
    });

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      appBar: AppBar(
        backgroundColor: AppDesignSystem.primary,
        foregroundColor: Colors.white,
        title: Text(l10n.translate('shifaAiTitle')),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline),
            tooltip: l10n.translate('copilotClearChat'),
            onPressed: chatState.isSending ? null : _onClearChat,
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              controller: _scrollController,
              padding: const EdgeInsets.all(AppDesignSystem.screenPaddingH),
              children: [
                ...chatState.messages.map((m) {
                  final isUser = m.role == 'user';
                  return Align(
                    alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.85),
                      decoration: BoxDecoration(
                        color: isUser
                            ? AppDesignSystem.primary.withValues(alpha: 0.12)
                            : Colors.grey.shade200,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        m.content,
                        style: AppDesignSystem.body1.copyWith(
                          color: AppDesignSystem.textPrimary,
                        ),
                      ),
                    ),
                  );
                }),
                if (chatState.streamingText != null && chatState.streamingText!.isNotEmpty)
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.85),
                      decoration: BoxDecoration(
                        color: Colors.grey.shade200,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        chatState.streamingText!,
                        style: AppDesignSystem.body1.copyWith(color: AppDesignSystem.textPrimary),
                      ),
                    ),
                  ),
                if (_suggestedDoctors.isNotEmpty) _buildDoctorSuggestionsBubble(context, l10n),
              ],
            ),
          ),
          if (chatState.isThinking || chatState.isAnalyzingSymptoms || chatState.isFindingDoctors)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    SizedBox(
                      height: 12,
                      width: 12,
                      child: CircularProgressIndicator(strokeWidth: 1.5, color: AppDesignSystem.primary),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      l10n.translate('copilotThinking'),
                      style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                    ),
                  ],
                ),
              ),
            ),
          if (chatState.lastError != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Material(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Expanded(child: Text(chatState.lastError!, style: TextStyle(color: Colors.red.shade900))),
                      IconButton(
                        icon: const Icon(Icons.close, size: 20),
                        onPressed: () => ref.read(copilotChatProvider.notifier).clearError(),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
            child: Text(
              l10n.translate('shifaAiDisclaimer'),
              textAlign: TextAlign.center,
              style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textTertiary),
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      TextButton.icon(
                        onPressed: _loadingSuggestions ? null : _onSuggestDoctors,
                        icon: _loadingSuggestions
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.medical_services_outlined, size: 20),
                        label: Text(l10n.translate('copilotSuggestDoctors')),
                      ),
                    ],
                  ),
                  Builder(
                    builder: (context) {
                      final lastAssistant = chatState.messages.isNotEmpty && chatState.messages.last.role == 'assistant'
                          ? chatState.messages.last.content
                          : null;
                      final options = _quickFollowUpOptions(lastAssistant);
                      if (options.isEmpty) return const SizedBox.shrink();
                      return SizedBox(
                        height: 36,
                        child: ListView(
                          scrollDirection: Axis.horizontal,
                          children: options.map((o) {
                            return Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: OutlinedButton(
                                onPressed: chatState.isSending
                                    ? null
                                    : () {
                                        _textController.text = o;
                                        _onSend();
                                      },
                                child: Text(o),
                              ),
                            );
                          }).toList(),
                        ),
                      );
                    },
                  ),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.mic),
                        onPressed: chatState.isSending ? null : _transcribeVoice,
                        tooltip: l10n.translate('recordVoice'),
                      ),
                      Expanded(
                        child: TextField(
                          controller: _textController,
                          minLines: 1,
                          maxLines: 4,
                          enabled: !chatState.isSending,
                          decoration: InputDecoration(
                            hintText: l10n.translate('copilotInputHint'),
                            filled: true,
                            fillColor: Colors.grey.shade100,
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: BorderSide.none,
                            ),
                          ),
                          onSubmitted: (_) => _onSend(),
                        ),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        onPressed: chatState.isSending ? null : _onSend,
                        style: FilledButton.styleFrom(
                          padding: const EdgeInsets.all(12),
                          shape: const CircleBorder(),
                        ),
                        child: chatState.isSending
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Icon(Icons.send),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DoctorSuggestionCard extends StatelessWidget {
  const _DoctorSuggestionCard({
    required this.doctor,
    required this.photoUrl,
    required this.btnStyle,
    required this.onProfile,
    required this.onManualBook,
    required this.onAutoBook,
    required this.nextSlotLabel,
    required this.confidenceLabel,
    required this.confidenceHigh,
    required this.confidenceMedium,
    required this.confidenceLow,
    required this.profileLabel,
    required this.bookManualLabel,
    required this.autoBookLabel,
  });

  final DoctorModel doctor;
  final String? photoUrl;
  final ButtonStyle btnStyle;
  final VoidCallback onProfile;
  final VoidCallback onManualBook;
  final VoidCallback onAutoBook;
  final String nextSlotLabel;
  final String confidenceLabel;
  final String confidenceHigh;
  final String confidenceMedium;
  final String confidenceLow;
  final String profileLabel;
  final String bookManualLabel;
  final String autoBookLabel;

  String _formatNextSlot(String iso) {
    final dt = DateTime.tryParse(iso)?.toLocal();
    if (dt == null) return iso;
    return DateFormat('EEE, d MMM, HH:mm').format(dt);
  }

  String _confidenceLabelFor(double rating) {
    if (rating >= 4.5) return confidenceHigh;
    if (rating >= 3.8) return confidenceMedium;
    return confidenceLow;
  }

  @override
  Widget build(BuildContext context) {
    final reason = doctor.recommendationReason?.trim();
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.black12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 22,
                backgroundImage: photoUrl != null && photoUrl!.isNotEmpty ? NetworkImage(photoUrl!) : null,
                child: photoUrl == null || photoUrl!.isEmpty ? const Icon(Icons.person, size: 22) : null,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      doctor.fullName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppDesignSystem.body1.copyWith(fontWeight: FontWeight.w600),
                    ),
                    if (doctor.profession != null && doctor.profession!.trim().isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        doctor.profession!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                      ),
                    ],
                  ],
                ),
              ),
              if (doctor.rating != null)
                Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.star, size: 14, color: Colors.amber),
                      const SizedBox(width: 2),
                      Text(
                        doctor.rating!.toStringAsFixed(1),
                        style: AppDesignSystem.caption.copyWith(fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          if (reason != null && reason.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              reason,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
              style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary, height: 1.3),
            ),
          ],
          if (doctor.nextAvailableStartAt != null && doctor.nextAvailableStartAt!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Row(
              children: [
                const Icon(Icons.event_available, size: 14, color: Colors.black54),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    '$nextSlotLabel ${_formatNextSlot(doctor.nextAvailableStartAt!)}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                  ),
                ),
              ],
            ),
          ],
          if (doctor.rating != null) ...[
            const SizedBox(height: 4),
            Text(
              '$confidenceLabel ${_confidenceLabelFor(doctor.rating!)}',
              style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
            ),
          ],
          const Divider(height: 14),
          Wrap(
            spacing: 4,
            runSpacing: 2,
            children: [
              TextButton(
                style: btnStyle,
                onPressed: onProfile,
                child: Text(profileLabel, style: const TextStyle(fontSize: 12)),
              ),
              TextButton(
                style: btnStyle,
                onPressed: onManualBook,
                child: Text(bookManualLabel, style: const TextStyle(fontSize: 12)),
              ),
              TextButton(
                style: btnStyle,
                onPressed: onAutoBook,
                child: Text(autoBookLabel, style: const TextStyle(fontSize: 12)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
