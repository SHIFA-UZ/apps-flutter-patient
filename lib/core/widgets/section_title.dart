import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Section title with 8px spacing below.
class SectionTitle extends StatelessWidget {
  final String title;
  final Widget? trailing;

  const SectionTitle({super.key, required this.title, this.trailing});

  @override
  Widget build(BuildContext context) {
    final child = Text(title, style: AppDesignSystem.h2);
    if (trailing != null) {
      return Padding(
        padding: const EdgeInsets.only(bottom: AppDesignSystem.sectionTitleSpacing),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(child: child),
            trailing!,
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: AppDesignSystem.sectionTitleSpacing),
      child: child,
    );
  }
}
