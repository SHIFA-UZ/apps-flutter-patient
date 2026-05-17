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

  @override
  Widget build(BuildContext context) {
    final currency = payload['currency']?.toString() ?? 'UZS';
    final total = (payload['totalMinor'] as num?) ?? 0;
    final paid = (payload['paidMinor'] as num?) ?? 0;
    final owed = (payload['owedMinor'] as num?) ?? 0;
    final status = payload['status']?.toString() ?? '';
    final notes = payload['notes']?.toString();

    return ListView(
      children: [
        Text(
          l10n.treatmentPlanStatusLine(status),
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 16),
        Text('${l10n.treatmentPlanTotalLabel}: ${formatMoney(total, currency)}'),
        Text('${l10n.treatmentPlanPaidLabel}: ${formatMoney(paid, currency)}'),
        Text(
          '${l10n.treatmentPlanOutstandingLabel}: ${formatMoney(owed, currency)}',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
        ),
        if (notes != null && notes.trim().isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(l10n.treatmentPlanNotesLabel, style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: 8),
          Text(notes),
        ],
      ],
    );
  }
}
