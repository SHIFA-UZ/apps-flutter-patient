import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

class DoctorQuickFilterChips extends StatelessWidget {
  final bool online;
  final bool nearMe;
  final bool today;
  final bool thisWeek;
  final bool topRated;
  final bool verified;
  final bool germany;
  final bool uzbekistan;
  final bool isFetchingLocation;
  final VoidCallback onOnlineChanged;
  final VoidCallback onNearMeChanged;
  final VoidCallback onTodayChanged;
  final VoidCallback onThisWeekChanged;
  final VoidCallback onTopRatedChanged;
  final VoidCallback onVerifiedChanged;
  final VoidCallback onGermanyChanged;
  final VoidCallback onUzbekistanChanged;
  final VoidCallback onMoreFilters;

  const DoctorQuickFilterChips({
    super.key,
    required this.online,
    required this.nearMe,
    required this.today,
    required this.thisWeek,
    required this.topRated,
    required this.verified,
    required this.germany,
    required this.uzbekistan,
    required this.isFetchingLocation,
    required this.onOnlineChanged,
    required this.onNearMeChanged,
    required this.onTodayChanged,
    required this.onThisWeekChanged,
    required this.onTopRatedChanged,
    required this.onVerifiedChanged,
    required this.onGermanyChanged,
    required this.onUzbekistanChanged,
    required this.onMoreFilters,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH),
      child: Row(
        children: [
          _chip(
            label: l10n.filterOnline,
            selected: online,
            onSelected: (_) => onOnlineChanged(),
          ),
          _chip(
            label: l10n.filterNearMe,
            selected: nearMe,
            onSelected: (_) => onNearMeChanged(),
            trailing: isFetchingLocation
                ? const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : null,
          ),
          _chip(label: l10n.filterToday, selected: today, onSelected: (_) => onTodayChanged()),
          _chip(label: l10n.filterThisWeek, selected: thisWeek, onSelected: (_) => onThisWeekChanged()),
          _chip(label: l10n.filterTopRated, selected: topRated, onSelected: (_) => onTopRatedChanged()),
          _chip(label: l10n.filterVerified, selected: verified, onSelected: (_) => onVerifiedChanged()),
          _chip(label: l10n.countryUzbekistan, selected: uzbekistan, onSelected: (_) => onUzbekistanChanged()),
          _chip(label: l10n.countryGermany, selected: germany, onSelected: (_) => onGermanyChanged()),
          ActionChip(
            label: Text(l10n.moreFilters),
            avatar: const Icon(Icons.tune, size: 18),
            onPressed: onMoreFilters,
          ),
        ],
      ),
    );
  }

  Widget _chip({
    required String label,
    required bool selected,
    required ValueChanged<bool> onSelected,
    Widget? trailing,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: FilterChip(
        label: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(label),
            if (trailing != null) ...[const SizedBox(width: 6), trailing],
          ],
        ),
        selected: selected,
        onSelected: onSelected,
        showCheckmark: false,
        selectedColor: AppDesignSystem.primary.withValues(alpha: 0.15),
        checkmarkColor: AppDesignSystem.primary,
      ),
    );
  }
}
