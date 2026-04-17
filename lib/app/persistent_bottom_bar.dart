import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Bottom navigation: full-width tappable items (icon + label), SafeArea inset, state-based tab switch via shell.
class PersistentBottomBar extends StatelessWidget {
  final int? currentIndex;
  final StatefulNavigationShell? navigationShell;

  const PersistentBottomBar({
    super.key,
    this.currentIndex,
    this.navigationShell,
  });

  static int _indexFromPath(String path) {
    if (path.startsWith(AppRoutes.home) || path == AppRoutes.home) return 0;
    if (path.startsWith(AppRoutes.bookings)) return 1;
    if (path.startsWith(AppRoutes.documents)) return 2;
    if (path.startsWith(AppRoutes.doctors)) return 3;
    return -1;
  }

  void _onTabTapped(int index, BuildContext context) {
    final path = _pathForIndex(index);
    if (path == null) return;
    if (navigationShell != null) {
      navigationShell!.goBranch(index);
      // Navigate to this tab's root so we always show Home/Bookings/Documents/Doctors root, not a nested screen.
      context.go(path);
      return;
    }
    context.go(path);
  }

  static String? _pathForIndex(int index) {
    switch (index) {
      case 0:
        return AppRoutes.home;
      case 1:
        return AppRoutes.bookings;
      case 2:
        return AppRoutes.documents;
      case 3:
        return AppRoutes.doctors;
      default:
        return null;
    }
  }

  static const double _iconSize = 24;

  Widget _navItem(
    BuildContext context, {
    required int index,
    required int effectiveIndex,
    required IconData iconData,
    required String label,
  }) {
    final isSelected = index == effectiveIndex;
    final color = isSelected ? AppDesignSystem.primary : AppDesignSystem.textTertiary;
    return Expanded(
      child: Material(
        color: AppDesignSystem.background,
        child: InkWell(
          onTap: () => _onTabTapped(index, context),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(iconData, size: _iconSize, color: color),
              const SizedBox(height: 4),
              Text(
                label,
                style: AppDesignSystem.caption.copyWith(
                  color: color,
                  fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final path = GoRouterState.of(context).uri.path;
    final index = currentIndex ?? _indexFromPath(path);
    final effectiveIndex = index >= 0 && index <= 3 ? index : 0;
    final bottomInset = MediaQuery.of(context).viewPadding.bottom;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Material(
          elevation: 8,
          color: AppDesignSystem.background,
          child: SizedBox(
            height: AppDesignSystem.bottomNavHeight,
            child: Row(
              children: [
                _navItem(
                  context,
                  index: 0,
                  effectiveIndex: effectiveIndex,
                  iconData: Icons.home_rounded,
                  label: l10n.home ?? 'Home',
                ),
                _navItem(
                  context,
                  index: 1,
                  effectiveIndex: effectiveIndex,
                  iconData: Icons.calendar_today_rounded,
                  label: l10n.bookings,
                ),
                _navItem(
                  context,
                  index: 2,
                  effectiveIndex: effectiveIndex,
                  iconData: Icons.folder_rounded,
                  label: l10n.documents,
                ),
                _navItem(
                  context,
                  index: 3,
                  effectiveIndex: effectiveIndex,
                  iconData: Icons.medical_services_rounded,
                  label: l10n.doctors,
                ),
              ],
            ),
          ),
        ),
        SizedBox(height: bottomInset),
      ],
    );
  }
}
