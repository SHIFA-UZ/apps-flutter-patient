import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Width mode for all Shifa buttons.
enum ButtonWidth { hug, fill }

// ─────────────────────────────────────────
// 1. PRIMARY BUTTON
// ─────────────────────────────────────────

/// Primary action button. One per page/view.
///
/// ```dart
/// ShifaPrimaryButton(label: 'Save', onPressed: () {})
/// ShifaPrimaryButton(label: 'Delete', onPressed: () {}, destructive: true)
/// ShifaPrimaryButton(label: 'Book', icon: Icons.add, onPressed: () {}, width: ButtonWidth.fill)
/// ShifaPrimaryButton.icon(icon: Icons.add, onPressed: () {}) // icon-only
/// ```
class ShifaPrimaryButton extends StatelessWidget {
  const ShifaPrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.destructive = false,
    this.width = ButtonWidth.fill,
    this.isLoading = false,
  }) : _iconOnly = false;

  const ShifaPrimaryButton.icon({
    super.key,
    required this.icon,
    required this.onPressed,
    this.destructive = false,
    this.isLoading = false,
  })  : label = '',
        width = ButtonWidth.hug,
        _iconOnly = true;

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool destructive;
  final ButtonWidth width;
  final bool isLoading;
  final bool _iconOnly;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !isLoading;

    Color bg;
    if (!enabled) {
      bg = AppDesignSystem.disabledGrey;
    } else if (destructive) {
      bg = AppDesignSystem.destructiveRed;
    } else {
      bg = AppDesignSystem.primary;
    }

    Color hoverBg = destructive ? AppDesignSystem.destructiveLight : AppDesignSystem.primaryLight;

    final style = ButtonStyle(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return AppDesignSystem.disabledGrey;
        if (states.contains(WidgetState.pressed) || states.contains(WidgetState.hovered)) return hoverBg;
        return bg;
      }),
      foregroundColor: WidgetStateProperty.all(AppDesignSystem.white),
      padding: WidgetStateProperty.all(const EdgeInsets.all(10)),
      shape: WidgetStateProperty.all(RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
      elevation: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.hovered)) return 2;
        return 0;
      }),
      minimumSize: WidgetStateProperty.all(
        _iconOnly ? const Size(40, 40) : (width == ButtonWidth.fill ? const Size(double.infinity, 40) : Size.zero),
      ),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );

    if (_iconOnly) {
      return IconButton(
        onPressed: enabled ? onPressed : null,
        icon: isLoading
            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: AppDesignSystem.white))
            : Icon(icon, size: 16),
        style: style,
      );
    }

    final child = isLoading
        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: AppDesignSystem.white))
        : Row(
            mainAxisSize: width == ButtonWidth.hug ? MainAxisSize.min : MainAxisSize.max,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 16),
                const SizedBox(width: 8),
              ],
              Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          );

    return SizedBox(
      width: width == ButtonWidth.fill ? double.infinity : null,
      child: ElevatedButton(
        onPressed: enabled ? onPressed : null,
        style: style,
        child: child,
      ),
    );
  }
}

// ─────────────────────────────────────────
// 2. SECONDARY BUTTON
// ─────────────────────────────────────────

/// Outline-style secondary action button.
///
/// ```dart
/// ShifaSecondaryButton(label: 'Cancel', onPressed: () {})
/// ShifaSecondaryButton(label: 'Delete', onPressed: () {}, destructive: true)
/// ShifaSecondaryButton.icon(icon: Icons.close, onPressed: () {})
/// ```
class ShifaSecondaryButton extends StatelessWidget {
  const ShifaSecondaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.destructive = false,
    this.width = ButtonWidth.fill,
    this.isLoading = false,
  }) : _iconOnly = false;

  const ShifaSecondaryButton.icon({
    super.key,
    required this.icon,
    required this.onPressed,
    this.destructive = false,
    this.isLoading = false,
  })  : label = '',
        width = ButtonWidth.hug,
        _iconOnly = true;

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool destructive;
  final ButtonWidth width;
  final bool isLoading;
  final bool _iconOnly;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !isLoading;

    Color accent = destructive ? AppDesignSystem.destructiveRed : AppDesignSystem.primary;
    Color disabledColor = AppDesignSystem.disabledGrey;
    Color hoverBg = destructive ? AppDesignSystem.destructiveSecondaryLight : AppDesignSystem.secondaryLight;

    final style = ButtonStyle(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return AppDesignSystem.white;
        if (states.contains(WidgetState.pressed) || states.contains(WidgetState.hovered)) return hoverBg;
        return AppDesignSystem.white;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return disabledColor;
        return accent;
      }),
      side: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return BorderSide(color: disabledColor, width: 2);
        return BorderSide(color: accent, width: 2);
      }),
      padding: WidgetStateProperty.all(const EdgeInsets.all(10)),
      shape: WidgetStateProperty.all(RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
      elevation: WidgetStateProperty.all(0),
      minimumSize: WidgetStateProperty.all(
        _iconOnly ? const Size(40, 40) : (width == ButtonWidth.fill ? const Size(double.infinity, 40) : Size.zero),
      ),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );

    if (_iconOnly) {
      return IconButton(
        onPressed: enabled ? onPressed : null,
        icon: isLoading
            ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: accent))
            : Icon(icon, size: 16),
        style: style,
      );
    }

    final child = isLoading
        ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: accent))
        : Row(
            mainAxisSize: width == ButtonWidth.hug ? MainAxisSize.min : MainAxisSize.max,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 16),
                const SizedBox(width: 8),
              ],
              Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          );

    return SizedBox(
      width: width == ButtonWidth.fill ? double.infinity : null,
      child: OutlinedButton(
        onPressed: enabled ? onPressed : null,
        style: style,
        child: child,
      ),
    );
  }
}

// ─────────────────────────────────────────
// 3. ACTION BUTTON (compact, for cards)
// ─────────────────────────────────────────

/// Smaller action button for cards, calendars, compact containers.
///
/// ```dart
/// ShifaActionButton(label: 'View', onPressed: () {})
/// ShifaActionButton(label: 'Cancel', onPressed: () {}, secondary: true, destructive: true)
/// ShifaActionButton.icon(icon: Icons.edit, onPressed: () {})
/// ```
class ShifaActionButton extends StatelessWidget {
  const ShifaActionButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.secondary = false,
    this.destructive = false,
    this.width = ButtonWidth.hug,
    this.isLoading = false,
  }) : _iconOnly = false;

  const ShifaActionButton.icon({
    super.key,
    required this.icon,
    required this.onPressed,
    this.secondary = false,
    this.destructive = false,
    this.isLoading = false,
  })  : label = '',
        width = ButtonWidth.hug,
        _iconOnly = true;

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool secondary;
  final bool destructive;
  final ButtonWidth width;
  final bool isLoading;
  final bool _iconOnly;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !isLoading;

    Color accent = destructive ? AppDesignSystem.destructiveRed : AppDesignSystem.primary;

    if (secondary) {
      return _buildSecondaryAction(context, enabled, accent);
    }
    return _buildPrimaryAction(context, enabled, accent);
  }

  Widget _buildPrimaryAction(BuildContext context, bool enabled, Color accent) {
    Color bg;
    if (!enabled) {
      bg = AppDesignSystem.disabledGrey;
    } else if (destructive) {
      bg = AppDesignSystem.destructiveRed;
    } else {
      bg = AppDesignSystem.primary;
    }

    Color hoverBg = destructive ? AppDesignSystem.destructiveLight : AppDesignSystem.primaryLight;

    final style = ButtonStyle(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return AppDesignSystem.disabledGrey;
        if (states.contains(WidgetState.pressed) || states.contains(WidgetState.hovered)) return hoverBg;
        return bg;
      }),
      foregroundColor: WidgetStateProperty.all(AppDesignSystem.white),
      padding: WidgetStateProperty.all(const EdgeInsets.all(8)),
      shape: WidgetStateProperty.all(RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
      elevation: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.hovered)) return 2;
        return 0;
      }),
      minimumSize: WidgetStateProperty.all(_iconOnly ? const Size(32, 32) : Size.zero),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );

    return _buildChild(style, enabled, AppDesignSystem.white, isElevated: true);
  }

  Widget _buildSecondaryAction(BuildContext context, bool enabled, Color accent) {
    Color disabledColor = AppDesignSystem.disabledGrey;
    Color hoverBg = destructive ? AppDesignSystem.destructiveSecondaryLight : AppDesignSystem.secondaryLight;

    final style = ButtonStyle(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return AppDesignSystem.white;
        if (states.contains(WidgetState.pressed) || states.contains(WidgetState.hovered)) return hoverBg;
        return AppDesignSystem.white;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return disabledColor;
        return accent;
      }),
      side: WidgetStateProperty.resolveWith((states) {
        if (!enabled) return BorderSide(color: disabledColor, width: 2);
        return BorderSide(color: accent, width: 2);
      }),
      padding: WidgetStateProperty.all(const EdgeInsets.all(8)),
      shape: WidgetStateProperty.all(RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
      elevation: WidgetStateProperty.all(0),
      minimumSize: WidgetStateProperty.all(_iconOnly ? const Size(32, 32) : Size.zero),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );

    return _buildChild(style, enabled, accent, isElevated: false);
  }

  Widget _buildChild(ButtonStyle style, bool enabled, Color spinnerColor, {required bool isElevated}) {
    if (_iconOnly) {
      return IconButton(
        onPressed: enabled ? onPressed : null,
        icon: isLoading
            ? SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: spinnerColor))
            : Icon(icon, size: 12),
        style: style,
      );
    }

    final child = isLoading
        ? SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: spinnerColor))
        : Row(
            mainAxisSize: width == ButtonWidth.hug ? MainAxisSize.min : MainAxisSize.max,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 12),
                const SizedBox(width: 4),
              ],
              Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
            ],
          );

    final button = isElevated
        ? ElevatedButton(onPressed: enabled ? onPressed : null, style: style, child: child)
        : OutlinedButton(onPressed: enabled ? onPressed : null, style: style, child: child);

    return SizedBox(
      width: width == ButtonWidth.fill ? double.infinity : null,
      child: button,
    );
  }
}
