import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

/// Shows an in-app explanation for why a permission is needed, before the system prompt.
/// The user must tap "Continue" to proceed — no cancel/dismiss option per Apple guideline 5.1.1(iv).
Future<void> showPermissionRationale({
  required BuildContext context,
  required String rationaleKey,
  String? title,
}) async {
  final l10n = AppLocalizations.of(context);
  if (l10n == null) return;

  final rationale = l10n.translate(rationaleKey);
  if (rationale == rationaleKey) return; // No translation, skip dialog

  await showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (ctx) => AlertDialog(
      title: Text(title ?? l10n.translate('permissionNeeded')),
      content: Text(rationale),
      actions: [
        FilledButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: Text(l10n.translate('continueButton')),
        ),
      ],
    ),
  );
}
