import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/base_card.dart';

/// Task card visually consistent with BaseCard but highlighted (teal tint).
class RemoteCareTaskCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final String? trailingLabel;
  final VoidCallback? onTap;

  const RemoteCareTaskCard({
    super.key,
    required this.title,
    required this.subtitle,
    this.trailingLabel,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final trailing = trailingLabel ?? l10n?.translate('view') ?? 'View';
    return BaseCard(
      onTap: onTap,
      color: AppDesignSystem.primary.withOpacity(0.08),
      elevated: true,
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppDesignSystem.primary,
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(Icons.task_alt, color: Colors.white, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppDesignSystem.body2.copyWith(fontWeight: FontWeight.w600),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          Text(
            trailing,
            style: AppDesignSystem.caption.copyWith(
              fontWeight: FontWeight.w600,
              color: AppDesignSystem.primary,
            ),
          ),
          const SizedBox(width: 4),
          Icon(Icons.chevron_right, size: 20, color: AppDesignSystem.textTertiary),
        ],
      ),
    );
  }
}
