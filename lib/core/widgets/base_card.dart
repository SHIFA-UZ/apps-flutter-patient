import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Reusable card: 16px radius, 16px padding, subtle shadow.
class BaseCard extends StatelessWidget {
  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry? padding;
  final Color? color;
  final bool elevated;

  const BaseCard({
    super.key,
    required this.child,
    this.onTap,
    this.padding,
    this.color,
    this.elevated = true,
  });

  @override
  Widget build(BuildContext context) {
    final content = Padding(
      padding: padding ?? const EdgeInsets.all(AppDesignSystem.cardPadding),
      child: child,
    );

    return Container(
      decoration: BoxDecoration(
        color: color ?? AppDesignSystem.background,
        borderRadius: BorderRadius.circular(AppDesignSystem.cardRadius),
        border: Border.all(color: AppDesignSystem.border, width: 1),
        boxShadow: elevated ? AppDesignSystem.cardShadow : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignSystem.cardRadius),
          child: content,
        ),
      ),
    );
  }
}
