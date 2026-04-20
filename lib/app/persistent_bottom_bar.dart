import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/app/shifa_ai_nav_fab.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Bottom navigation: Home + Bookings | **center Shifa AI (round logo)** | Documents + Doctors.
class PersistentBottomBar extends StatelessWidget {
  final int? currentIndex;
  final StatefulNavigationShell? navigationShell;

  const PersistentBottomBar({
    super.key,
    this.currentIndex,
    this.navigationShell,
  });

  static bool _isCopilotPath(String path) {
    return path == AppRoutes.shifaAi || path.startsWith('${AppRoutes.shifaAi}/');
  }

  static int _indexFromPath(String path) {
    if (_isCopilotPath(path)) return -1;
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
  /// Raised center control — noticeably larger than side nav icons / tap targets.
  static const double _fabSize = 74;
  static const double _barHeight = 56;

  Widget _navItem(
    BuildContext context, {
    required int index,
    required int? effectiveIndex,
    required IconData iconData,
    required String label,
  }) {
    final isSelected = effectiveIndex != null && effectiveIndex >= 0 && index == effectiveIndex;
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
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
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
    final effectiveIndex = index >= 0 && index <= 3 ? index : null;
    final bottomInset = MediaQuery.of(context).viewPadding.bottom;
    final stackHeight = _barHeight + (_fabSize / 2) + bottomInset;
    final onCopilot = _isCopilotPath(path);

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          height: stackHeight,
          width: double.infinity,
          child: Stack(
            clipBehavior: Clip.none,
            alignment: Alignment.bottomCenter,
            children: [
              Align(
                alignment: Alignment.bottomCenter,
                child: Padding(
                  padding: EdgeInsets.only(bottom: bottomInset),
                  child: Material(
                    elevation: 8,
                    color: AppDesignSystem.background,
                    child: SizedBox(
                      height: _barHeight,
                      width: double.infinity,
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
                          const SizedBox(width: _fabSize),
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
                ),
              ),
              Positioned(
                left: 0,
                right: 0,
                bottom: bottomInset + _barHeight - (_fabSize / 2),
                child: Center(
                  child: ShifaAiNavFab(
                    size: _fabSize,
                    active: onCopilot,
                    onPressed: () => context.push(AppRoutes.shifaAi),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
