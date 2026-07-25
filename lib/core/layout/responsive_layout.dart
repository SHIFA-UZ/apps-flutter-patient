import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Shared breakpoints and padding for web / tablet / mobile.
class ResponsiveLayout {
  ResponsiveLayout._();

  /// Max width for primary page content (doctors list, forms, etc.).
  static const double contentMaxWidth = 840;

  static const double wideBreakpoint = 720;

  static bool isWide(BuildContext context, {double breakpoint = wideBreakpoint}) {
    return MediaQuery.sizeOf(context).width >= breakpoint;
  }

  /// Centers content on wide viewports; keeps 16px minimum gutter on phones.
  static double horizontalInset(
    BuildContext context, {
    double maxWidth = contentMaxWidth,
  }) {
    final width = MediaQuery.sizeOf(context).width;
    if (width <= maxWidth) return AppDesignSystem.screenPaddingH;
    return math.max(AppDesignSystem.screenPaddingH, (width - maxWidth) / 2);
  }

  static EdgeInsets symmetricPagePadding(
    BuildContext context, {
    double maxWidth = contentMaxWidth,
  }) {
    return EdgeInsets.symmetric(horizontal: horizontalInset(context, maxWidth: maxWidth));
  }
}
