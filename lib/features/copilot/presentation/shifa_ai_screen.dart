import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
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
    _textController.clear();
    final l10n = AppLocalizations.of(context)!;
    final lang = copilotBackendLanguage(ref.read(profileProvider).profile?.language);
    final preview = await ref.read(copilotChatProvider.notifier).sendUserMessage(text, lang);
    if (!mounted) return;
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

  Future<void> _onSuggestDoctors() async {
    final l10n = AppLocalizations.of(context)!;
    final chat = ref.read(copilotChatProvider);
    final lastUser = chat.messages.where((m) => m.role == 'user').toList();
    final symptoms = lastUser.isNotEmpty ? lastUser.last.content : _textController.text.trim();
    if (symptoms.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('copilotInputHint'))),
      );
      return;
    }

    setState(() => _loadingSuggestions = true);
    try {
      final list = await ref.read(copilotChatProvider.notifier).fetchSuggestedDoctors(symptoms);
      setState(() {
        _suggestedDoctors = list;
        _loadingSuggestions = false;
      });
      if (list.isEmpty && mounted) {
        ref.read(copilotChatProvider.notifier).appendAssistantMessage(
              l10n.translate('copilotNoProviderOnPlatform'),
            );
      }
    } catch (e) {
      setState(() => _loadingSuggestions = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    }
  }

  Future<void> _transcribeVoice() async {
    final l10n = AppLocalizations.of(context)!;
    await showDialog<void>(
      context: context,
      builder: (ctx) => VoiceRecordingDialog(
        onRecordingComplete: (filePath, _) async {
          Navigator.of(ctx).pop();
          try {
            final text = await ref.read(copilotApiProvider).transcribeAudio(filePath);
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
              ],
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
          if (_suggestedDoctors.isNotEmpty) ...[
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH),
                child: Text(
                  l10n.translate('copilotSuggestedDoctors'),
                  style: AppDesignSystem.h2.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
            ),
            SizedBox(
              height: 168,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH, vertical: 8),
                itemCount: _suggestedDoctors.length,
                itemBuilder: (context, i) {
                  final d = _suggestedDoctors[i];
                  final photo = withCacheBuster(d.photoUrl, null);
                  return Card(
                    margin: const EdgeInsets.only(right: 10),
                    child: SizedBox(
                      width: 220,
                      child: Padding(
                        padding: const EdgeInsets.all(8),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                CircleAvatar(
                                  radius: 18,
                                  backgroundImage: photo != null && photo.isNotEmpty
                                      ? NetworkImage(photo)
                                      : null,
                                  child: photo == null || photo.isEmpty
                                      ? const Icon(Icons.person, size: 20)
                                      : null,
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    d.fullName,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: AppDesignSystem.caption.copyWith(fontWeight: FontWeight.w600),
                                  ),
                                ),
                              ],
                            ),
                            if (d.profession != null)
                              Text(
                                d.profession!,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                              ),
                            const Spacer(),
                            Wrap(
                              spacing: 0,
                              runSpacing: 0,
                              children: [
                                TextButton(
                                  onPressed: () => context.push('${AppRoutes.doctors}/${d.id}'),
                                  child: Text(l10n.translate('copilotViewProfile')),
                                ),
                                TextButton(
                                  onPressed: () => _openManualBooking(d),
                                  child: Text(l10n.translate('copilotBookManual')),
                                ),
                                TextButton(
                                  onPressed: () => _showAutoBookSheet(d),
                                  child: Text(l10n.translate('copilotAutoBook')),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
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
