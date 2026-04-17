import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/services/local_notification_service.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';

class VisitSummaryScreen extends ConsumerStatefulWidget {
  final String appointmentId;

  const VisitSummaryScreen({super.key, required this.appointmentId});

  @override
  ConsumerState<VisitSummaryScreen> createState() => _VisitSummaryScreenState();
}

class _VisitSummaryScreenState extends ConsumerState<VisitSummaryScreen> {
  final TextEditingController _questionController = TextEditingController();
  String? _answer;
  List<String> _citations = const [];
  bool _asking = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final locale = Localizations.localeOf(context).languageCode;
      await ref.read(bookingsRepositoryProvider).generateVisitSummary(
            widget.appointmentId,
            language: locale,
          );
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _questionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final locale = Localizations.localeOf(context).languageCode;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.translate('visitSummaryTitle')),
      ),
      body: FutureBuilder<VisitSummaryResponse>(
        future: ref.read(bookingsRepositoryProvider).getVisitSummary(
              widget.appointmentId,
              language: locale,
            ),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('${l10n.error}: ${snapshot.error}'),
            );
          }

          final data = snapshot.data;
          if (data == null || data.status != 'ready' || data.content == null) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.translate('visitSummaryPreparing'),
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }

          final content = data.content!;
          final carePlan = _asStringList(content['carePlan']);
          final nextSteps = _asStringList(content['nextSteps']);
          final meds = _asMapList(content['medicationGuidance']);
          final redFlags = _asMapList(content['redFlags']);

          return RefreshIndicator(
            onRefresh: () async => setState(() {}),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _SectionCard(
                  title: l10n.translate('visitSummaryWhatHappened'),
                  child: Text((content['summaryPlain'] ?? '').toString()),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: l10n.translate('visitSummaryCarePlan'),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: carePlan.isEmpty
                        ? const [Text('-')]
                        : carePlan.asMap().entries.map((entry) {
                            final item = entry.value;
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 8),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Expanded(child: Text('• $item')),
                                  IconButton(
                                    tooltip: l10n.translate('remindMe'),
                                    icon: const Icon(Icons.notifications_active_outlined, size: 20),
                                    onPressed: () async {
                                      try {
                                        final selected = await _pickReminderDateTime(context);
                                        if (!mounted || selected == null) return;

                                        await LocalNotificationService().scheduleChecklistReminder(
                                          appointmentId: widget.appointmentId,
                                          checklistItem: item,
                                          title: l10n.translate('visitSummaryChecklistReminderTitle'),
                                          scheduledAt: selected,
                                        );
                                        if (!mounted) return;
                                        final materialL10n = MaterialLocalizations.of(context);
                                        final when =
                                            '${materialL10n.formatShortDate(selected)} ${materialL10n.formatTimeOfDay(TimeOfDay.fromDateTime(selected))}';
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          SnackBar(
                                            content: Text('${l10n.translate('visitSummaryReminderCreated')}: $when'),
                                          ),
                                        );
                                      } catch (e) {
                                        if (!mounted) return;
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          SnackBar(content: Text('${l10n.error}: $e')),
                                        );
                                      }
                                    },
                                  ),
                                ],
                              ),
                            );
                          }).toList(),
                  ),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: l10n.translate('visitSummaryMedications'),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: meds.map((m) {
                      final name = (m['name'] ?? '').toString();
                      final instructions = (m['instructions'] ?? '').toString();
                      final missedDose = (m['missedDose'] ?? '').toString();
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
                            if (instructions.isNotEmpty) Text(instructions),
                            if (missedDose.isNotEmpty)
                              Text(
                                '${l10n.translate('visitSummaryMissedDose')}: $missedDose',
                                style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
                              ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: l10n.translate('visitSummaryRedFlags'),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: redFlags.map((rf) {
                      final sign = (rf['sign'] ?? '').toString();
                      final urgency = (rf['urgency'] ?? '').toString();
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Text('• $sign ${urgency.isNotEmpty ? "($urgency)" : ""}'),
                      );
                    }).toList(),
                  ),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: l10n.translate('visitSummaryNextSteps'),
                  child: _Bullets(items: nextSteps),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: l10n.translate('visitSummaryAskTitle'),
                  child: Column(
                    children: [
                      TextField(
                        controller: _questionController,
                        minLines: 1,
                        maxLines: 3,
                        decoration: InputDecoration(
                          hintText: l10n.translate('visitSummaryAskHint'),
                          border: const OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Align(
                        alignment: Alignment.centerRight,
                        child: ShifaPrimaryButton(
                          label: l10n.translate('send'),
                          isLoading: _asking,
                          onPressed: _asking
                              ? null
                              : () async {
                                  final q = _questionController.text.trim();
                                  if (q.isEmpty) return;
                                  setState(() => _asking = true);
                                  try {
                                    final result = await ref.read(bookingsRepositoryProvider).askVisitSummary(
                                          widget.appointmentId,
                                          question: q,
                                          language: locale,
                                        );
                                    if (!mounted) return;
                                    setState(() {
                                      _answer = result.answer;
                                      _citations = result.citations;
                                    });
                                  } finally {
                                    if (mounted) setState(() => _asking = false);
                                  }
                                },
                          width: ButtonWidth.hug,
                        ),
                      ),
                      if (_answer != null && _answer!.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.grey.shade100,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(_answer!),
                        ),
                        if (_citations.isNotEmpty) ...[
                          const SizedBox(height: 8),
                          Text(
                            '${l10n.translate('visitSummarySources')}: ${_citations.join(', ')}',
                            style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
                          ),
                        ],
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  (content['disclaimer'] ?? '').toString(),
                  style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  List<String> _asStringList(dynamic value) {
    if (value is! List) return const [];
    return value.map((e) => e?.toString() ?? '').where((e) => e.isNotEmpty).toList();
    }

  List<Map<String, dynamic>> _asMapList(dynamic value) {
    if (value is! List) return const [];
    return value.whereType<Map>().map((e) => e.map((k, v) => MapEntry(k.toString(), v))).toList();
  }

  Future<DateTime?> _pickReminderDateTime(BuildContext context) async {
    final l10n = AppLocalizations.of(context)!;
    final now = DateTime.now();
    final quick = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.translate('visitSummaryQuickReminderTitle'),
                  style: Theme.of(sheetContext).textTheme.titleMedium,
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    ActionChip(
                      label: Text(l10n.translate('visitSummaryQuick15Min')),
                      onPressed: () => Navigator.of(sheetContext).pop('15m'),
                    ),
                    ActionChip(
                      label: Text(l10n.translate('visitSummaryTonight')),
                      onPressed: () => Navigator.of(sheetContext).pop('tonight'),
                    ),
                    ActionChip(
                      label: Text(l10n.translate('visitSummaryTomorrowMorning')),
                      onPressed: () => Navigator.of(sheetContext).pop('tomorrow'),
                    ),
                    ActionChip(
                      label: Text(l10n.translate('visitSummaryCustom')),
                      onPressed: () => Navigator.of(sheetContext).pop('custom'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );

    if (quick == null) return null;
    if (quick == '15m') return now.add(const Duration(minutes: 15));
    if (quick == 'tonight') {
      final tonight = DateTime(now.year, now.month, now.day, 20, 0);
      return tonight.isAfter(now) ? tonight : now.add(const Duration(minutes: 1));
    }
    if (quick == 'tomorrow') {
      return DateTime(now.year, now.month, now.day + 1, 9, 0);
    }
    if (quick != 'custom') return null;

    final date = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365)),
    );
    if (date == null) return null;

    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(now.add(const Duration(hours: 1))),
    );
    if (time == null) return null;

    final selected = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    if (selected.isBefore(now.add(const Duration(minutes: 1)))) {
      return now.add(const Duration(minutes: 1));
    }
    return selected;
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final Widget child;

  const _SectionCard({required this.title, required this.child});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            child,
          ],
        ),
      ),
    );
  }
}

class _Bullets extends StatelessWidget {
  final List<String> items;
  const _Bullets({required this.items});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const Text('—');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: items.map((e) => Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Text('• $e'),
      )).toList(),
    );
  }
}

