import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';

/// Read-only summary from `GET /patients/me/treatment-plans/{id}` (doctor-facing plans / ledger).
class TreatmentPlanSummaryScreen extends ConsumerStatefulWidget {
  final String planId;

  const TreatmentPlanSummaryScreen({super.key, required this.planId});

  @override
  ConsumerState<TreatmentPlanSummaryScreen> createState() =>
      _TreatmentPlanSummaryScreenState();
}

class _TreatmentPlanSummaryScreenState extends ConsumerState<TreatmentPlanSummaryScreen> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic>? _payload;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final api = ref.read(apiClientProvider);
      final res = await api.get('/patients/me/treatment-plans/${widget.planId}');
      if (!mounted) return;
      final l10n = AppLocalizations.of(context)!;
      if (res.statusCode != 200) {
        setState(() {
          _loading = false;
          _error = l10n.treatmentPlanLoadFailedWithCode('${res.statusCode}');
        });
        return;
      }
      final data = res.data;
      setState(() {
        _loading = false;
        _payload = data is Map<String, dynamic> ? data : null;
      });
    } catch (e) {
      if (!mounted) return;
      final l10n = AppLocalizations.of(context)!;
      setState(() {
        _loading = false;
        _error = l10n.treatmentPlanUnexpectedError(e.toString());
      });
    }
  }

  String _money(num minor, String currency) =>
      '${(minor.toDouble() / 100).toStringAsFixed(2)} $currency';

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.treatmentPlanScreenTitle),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.canPop() ? context.pop() : context.go('/bookings'),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(child: Text(_error!, textAlign: TextAlign.center))
                : _payload == null
                    ? Center(child: Text(l10n.treatmentPlanNoData))
                    : _SummaryBody(
                        payload: _payload!,
                        formatMoney: _money,
                        l10n: l10n,
                      ),
      ),
    );
  }
}

class _SummaryBody extends StatelessWidget {
  final Map<String, dynamic> payload;
  final String Function(num minor, String currency) formatMoney;
  final AppLocalizations l10n;

  const _SummaryBody({
    required this.payload,
    required this.formatMoney,
    required this.l10n,
  });

  List<Map<String, dynamic>> _asMapList(dynamic raw) {
    if (raw is! List) return const [];
    return raw.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
  }

  List<Widget> _installmentItemRows({
    required Map<String, dynamic> plan,
    required String defaultCurrency,
    required ThemeData theme,
  }) {
    final items = _asMapList(plan['items']);
    final planCur = plan['currency']?.toString() ?? defaultCurrency;
    return [
      for (final item in items) ...[
        const Divider(height: 20),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.treatmentPlanItemDue(item['dueDate']?.toString() ?? '—'),
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Text(
                    '#${item['sequenceNumber']}',
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  formatMoney(
                    (item['amountMinor'] as num?) ?? 0,
                    item['currency']?.toString() ?? planCur,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  item['status']?.toString() ?? '',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ],
        ),
      ],
    ];
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final currency = payload['currency']?.toString() ?? 'UZS';
    final total = (payload['totalMinor'] as num?) ?? 0;
    final paid = (payload['paidMinor'] as num?) ?? 0;
    final owed = (payload['owedMinor'] as num?) ?? 0;
    final status = payload['status']?.toString() ?? '';
    final notes = payload['notes']?.toString();
    final title = payload['title']?.toString();
    final diagnosis = payload['diagnosis']?.toString();
    final planPaymentStatus = payload['planPaymentStatus']?.toString() ?? '';
    final lines = _asMapList(payload['lines']);
    final installmentPlans = _asMapList(payload['installmentPlans']);

    return ListView(
      children: [
        if (title != null && title.trim().isNotEmpty) ...[
          Text(l10n.treatmentPlanTitleHeading, style: theme.textTheme.labelLarge),
          const SizedBox(height: 4),
          Text(title, style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w600)),
          const SizedBox(height: 16),
        ],
        Text(l10n.treatmentPlanStatusLine(status), style: theme.textTheme.titleMedium),
        if (planPaymentStatus.isNotEmpty) ...[
          const SizedBox(height: 6),
          Text(
            l10n.treatmentPlanPaymentStatusLineLong(planPaymentStatus),
            style: theme.textTheme.bodyMedium,
          ),
        ],
        if (diagnosis != null && diagnosis.trim().isNotEmpty) ...[
          const SizedBox(height: 16),
          Text(l10n.treatmentPlanDiagnosisLabel, style: theme.textTheme.titleSmall),
          const SizedBox(height: 4),
          Text(diagnosis),
        ],
        const SizedBox(height: 20),
        Text('${l10n.treatmentPlanTotalLabel}: ${formatMoney(total, currency)}'),
        Text('${l10n.treatmentPlanPaidLabel}: ${formatMoney(paid, currency)}'),
        Text(
          '${l10n.treatmentPlanOutstandingLabel}: ${formatMoney(owed, currency)}',
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        ),
        if (lines.isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(l10n.treatmentPlanServicesHeading, style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          Card(
            margin: EdgeInsets.zero,
            child: Column(
              children: [
                for (var i = 0; i < lines.length; i++) ...[
                  if (i > 0) const Divider(height: 1),
                      Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    child: Builder(
                      builder: (context) {
                        final line = lines[i];
                        final disc = (line['discountMinor'] as num?) ?? 0;
                        final lineCur = line['currency']?.toString() ?? currency;
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    line['title']?.toString() ?? '—',
                                    style: theme.textTheme.bodyLarge?.copyWith(
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    l10n.treatmentPlanQuantityShort(
                                      (line['quantity'] as num?)?.toInt() ?? 0,
                                    ),
                                    style: theme.textTheme.bodySmall,
                                  ),
                                  if (disc > 0)
                                    Text(
                                      l10n.treatmentPlanDiscountLine(formatMoney(disc, lineCur)),
                                      style: theme.textTheme.bodySmall,
                                    ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  formatMoney(
                                    (line['lineTotalMinor'] as num?) ?? 0,
                                    lineCur,
                                  ),
                                  style: theme.textTheme.titleSmall,
                                ),
                                const SizedBox(height: 4),
                                Chip(
                                  label: Text(
                                    line['status']?.toString() ?? '',
                                    style: const TextStyle(fontSize: 11),
                                  ),
                                  visualDensity: VisualDensity.compact,
                                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                                  padding: EdgeInsets.zero,
                                  labelPadding: const EdgeInsets.symmetric(horizontal: 8),
                                ),
                              ],
                            ),
                          ],
                        );
                      },
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
        if (installmentPlans.isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(l10n.treatmentPlanInstallmentsHeading, style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          for (final plan in installmentPlans) ...[
            Card(
              margin: const EdgeInsets.only(bottom: 12),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            l10n.treatmentPlanInstallmentPlanLabel,
                            style: theme.textTheme.titleSmall,
                          ),
                        ),
                        Chip(
                          label: Text(
                            plan['status']?.toString() ?? '',
                            style: const TextStyle(fontSize: 11),
                          ),
                          visualDensity: VisualDensity.compact,
                          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      formatMoney(
                        (plan['totalAmountMinor'] as num?) ?? 0,
                        plan['currency']?.toString() ?? currency,
                      ),
                      style: theme.textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 8),
                    ..._installmentItemRows(plan: plan, defaultCurrency: currency, theme: theme),
                  ],
                ),
              ),
            ),
          ],
        ],
        if (notes != null && notes.trim().isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(l10n.treatmentPlanNotesLabel, style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          Text(notes),
        ],
      ],
    );
  }
}
