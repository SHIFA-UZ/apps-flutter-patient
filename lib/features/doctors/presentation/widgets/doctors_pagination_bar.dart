import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

class DoctorsPaginationBar extends StatelessWidget {
  final int currentPage;
  final int totalPages;
  final int totalCount;
  final ValueChanged<int> onPageSelected;

  const DoctorsPaginationBar({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.totalCount,
    required this.onPageSelected,
  });

  @override
  Widget build(BuildContext context) {
    if (totalPages <= 1) return const SizedBox.shrink();

    final l10n = AppLocalizations.of(context)!;
    final pages = _visiblePages(currentPage, totalPages);

    return Padding(
      padding: const EdgeInsets.only(top: 8, bottom: 16),
      child: Column(
        children: [
          Text(
            l10n.doctorsPageSummary(currentPage, totalPages, totalCount),
            style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
          ),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _NavButton(
                icon: Icons.chevron_left,
                enabled: currentPage > 1,
                tooltip: l10n.previousPage,
                onTap: () => onPageSelected(currentPage - 1),
              ),
              const SizedBox(width: 4),
              ...pages.map((page) {
                if (page == -1) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Text('…', style: AppDesignSystem.caption),
                  );
                }
                final selected = page == currentPage;
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  child: Material(
                    color: selected
                        ? AppDesignSystem.primary.withValues(alpha: 0.15)
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(8),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(8),
                      onTap: selected ? null : () => onPageSelected(page),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        child: Text(
                          '$page',
                          style: AppDesignSystem.body2.copyWith(
                            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                            color: selected
                                ? AppDesignSystem.primary
                                : AppDesignSystem.textPrimary,
                          ),
                        ),
                      ),
                    ),
                  ),
                );
              }),
              const SizedBox(width: 4),
              _NavButton(
                icon: Icons.chevron_right,
                enabled: currentPage < totalPages,
                tooltip: l10n.nextPage,
                onTap: () => onPageSelected(currentPage + 1),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Returns page numbers to show; -1 = ellipsis.
  List<int> _visiblePages(int current, int total) {
    if (total <= 7) {
      return List.generate(total, (i) => i + 1);
    }
    final pages = <int>{1, total, current, current - 1, current + 1};
    final sorted = pages.where((p) => p >= 1 && p <= total).toList()..sort();
    final result = <int>[];
    for (var i = 0; i < sorted.length; i++) {
      if (i > 0 && sorted[i] - sorted[i - 1] > 1) {
        result.add(-1);
      }
      result.add(sorted[i]);
    }
    return result;
  }
}

class _NavButton extends StatelessWidget {
  final IconData icon;
  final bool enabled;
  final String tooltip;
  final VoidCallback onTap;

  const _NavButton({
    required this.icon,
    required this.enabled,
    required this.tooltip,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: tooltip,
      onPressed: enabled ? onTap : null,
      icon: Icon(
        icon,
        color: enabled ? AppDesignSystem.primary : AppDesignSystem.textTertiary,
      ),
    );
  }
}
