import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

/// Shows an in-app explanation for why a permission is needed, before the system prompt.
/// Use before requesting camera, microphone, location, or notifications.
/// Returns true if user tapped OK (proceed to request), false if cancelled.
Future<bool> showPermissionRationale({
  required BuildContext context,
  required String rationaleKey,
  String? title,
}) async {
  final l10n = AppLocalizations.of(context);
  if (l10n == null) return true;

  final rationale = l10n.translate(rationaleKey);
  if (rationale == rationaleKey) return true; // No translation, skip dialog

  final result = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(title ?? l10n.translate('permissionNeeded')),
      content: Text(rationale),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(false),
          child: Text(l10n.translate('cancel')),
        ),
        FilledButton(
          onPressed: () => Navigator.of(ctx).pop(true),
          child: Text(l10n.translate('ok')),
        ),
      ],
    ),
  );
  return result ?? false;
}
