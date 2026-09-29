import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

/// Lets the patient pick Click or Stripe before a checkout WebView opens.
class PaymentGatewayChooser extends StatelessWidget {
  const PaymentGatewayChooser({super.key, required this.onSelected});

  final void Function(String gateway) onSelected;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          l10n.translate('choosePaymentMethod'),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 6),
        Text(
          l10n.translate('choosePaymentMethodHint'),
          style: theme.textTheme.bodyMedium?.copyWith(color: Colors.grey.shade700),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () => onSelected('CLICK'),
          icon: const Icon(Icons.account_balance_wallet_outlined),
          label: Text(l10n.translate('payWithClick')),
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(
          onPressed: () => onSelected('STRIPE'),
          icon: const Icon(Icons.credit_card),
          label: Text(l10n.translate('payWithStripe')),
        ),
      ],
    );
  }
}

Future<String?> pickPaymentGateway(BuildContext context) {
  return showModalBottomSheet<String>(
    context: context,
    showDragHandle: true,
    useSafeArea: true,
    builder: (ctx) => Padding(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
      child: PaymentGatewayChooser(
        onSelected: (gateway) => Navigator.of(ctx).pop(gateway),
      ),
    ),
  );
}
