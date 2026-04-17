import 'package:flutter/material.dart';

class AppDesignSystem {
  AppDesignSystem._();

  /// Bottom padding for scroll content when a bottom nav bar is shown.
  /// Use this so content and FAB don't sit under the nav bar or system gesture area.
  static double safeBottomWithNavBar(BuildContext context) {
    final padding = MediaQuery.of(context).viewPadding;
    return AppDesignSystem.bottomNavHeight + padding.bottom;
  }

  // ── Color tokens (shared across Patient + Doctor apps) ──
  static const Color primary = Color(0xFF00BBB0);
  static const Color primaryLight = Color(0xFF59C2BC);
  static const Color secondaryLight = Color(0xFFCCF1EF);
  static const Color primaryDark = Color(0xFF129B8A);
  static const Color destructiveRed = Color(0xFFDC2F2F);
  static const Color destructiveLight = Color(0xFFEB5454);
  static const Color destructiveSecondaryLight = Color(0xFFF5C1C1);
  static const Color disabledGrey = Color(0xFFC6C6C6);
  static const Color white = Color(0xFFFFFFFF);
  static const Color textPrimary = Color(0xFF1A1A1A);
  static const Color textSecondary = Color(0xFF6B7280);
  static const Color textTertiary = Color(0xFF9CA3AF);
  static const Color border = Color(0xFFE5E7EB);
  static const Color background = Color(0xFFFFFFFF);
  static const Color backgroundSecondary = Color(0xFFF9FAFB);
  static const Color backgroundTertiary = Color(0xFFF3F4F6);

  static List<BoxShadow> get cardShadow => [
        BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 8, offset: const Offset(0, 2)),
      ];

  static const double h1Size = 24;
  static const double h2Size = 18;
  static const double body1Size = 16;
  static const double body2Size = 14;
  static const double captionSize = 12;

  static TextStyle get h1 => TextStyle(fontSize: h1Size, fontWeight: FontWeight.w600, color: textPrimary, height: 1.25);
  static TextStyle get h2 => TextStyle(fontSize: h2Size, fontWeight: FontWeight.w600, color: textPrimary, height: 1.3);
  static TextStyle get body1 => TextStyle(fontSize: body1Size, fontWeight: FontWeight.normal, color: textPrimary, height: 1.4);
  static TextStyle get body2 => TextStyle(fontSize: body2Size, fontWeight: FontWeight.normal, color: textPrimary, height: 1.4);
  static TextStyle get caption => TextStyle(fontSize: captionSize, fontWeight: FontWeight.normal, color: textSecondary, height: 1.35);

  static const double screenPaddingH = 16;
  static const double sectionTitleSpacing = 8;
  static const double sectionToSectionSpacing = 24;
  static const double cardSpacing = 12;
  static const double cardRadius = 16;
  static const double cardPadding = 16;
  static const double headerHeight = 140;
  static const double headerRadius = 24;
  static const double headerPadding = 16;
  /// Height for bottom nav bar (icon + label). 80 avoids overflow on typical densities.
  static const double bottomNavHeight = 80;
  static const double fabSafeBottom = 80;
}
